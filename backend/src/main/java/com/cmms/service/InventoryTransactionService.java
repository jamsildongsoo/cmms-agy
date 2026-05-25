package com.cmms.service;

import com.cmms.dto.InventoryTxDto.InventoryTxRequest;
import com.cmms.dto.InventoryTxDto.TxItem;
import com.cmms.model.*;
import com.cmms.repository.InventoryHistoryRepository;
import com.cmms.repository.InventoryMonthlyClosingRepository;
import com.cmms.repository.InventoryStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InventoryTransactionService {

    @Autowired
    private InventoryStatusRepository inventoryStatusRepository;

    @Autowired
    private InventoryHistoryRepository inventoryHistoryRepository;

    @Autowired
    private InventoryMonthlyClosingRepository inventoryMonthlyClosingRepository;

    private static class StatusKey implements Comparable<StatusKey> {
        final String warehouseId;
        final String inventoryId;

        StatusKey(String warehouseId, String inventoryId) {
            this.warehouseId = warehouseId;
            this.inventoryId = inventoryId;
        }

        @Override
        public int compareTo(StatusKey o) {
            int cmp = this.warehouseId.compareTo(o.warehouseId);
            if (cmp != 0) return cmp;
            return this.inventoryId.compareTo(o.inventoryId);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StatusKey statusKey = (StatusKey) o;
            return Objects.equals(warehouseId, statusKey.warehouseId) &&
                   Objects.equals(inventoryId, statusKey.inventoryId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(warehouseId, inventoryId);
        }
    }

    @Transactional
    public void processTransactions(String companyId, InventoryTxRequest request, String operator) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return;
        }

        // 1. 데드락 방지를 위한 정렬 잠금 대상 추출
        Set<StatusKey> keysToLock = new HashSet<>();
        for (TxItem item : request.getItems()) {
            keysToLock.add(new StatusKey(item.getWarehouseId(), item.getInventoryId()));
            if ("MOVE".equalsIgnoreCase(item.getTxTypeCode()) && item.getTargetWarehouseId() != null) {
                keysToLock.add(new StatusKey(item.getTargetWarehouseId(), item.getInventoryId()));
            }
        }

        List<StatusKey> sortedKeys = new ArrayList<>(keysToLock);
        Collections.sort(sortedKeys);

        // 2. 정렬된 순서대로 비관적 락 걸기
        Map<StatusKey, InventoryStatus> statusMap = new HashMap<>();
        for (StatusKey key : sortedKeys) {
            // DB에 데이터가 없으면 신규 데이터 생성 후 락
            Optional<InventoryStatus> statusOpt = inventoryStatusRepository.findByIdWithLock(
                    companyId, key.warehouseId, key.inventoryId);
            
            InventoryStatus status;
            if (statusOpt.isEmpty()) {
                status = new InventoryStatus();
                status.setCompanyId(companyId);
                status.setWarehouseId(key.warehouseId);
                status.setInventoryId(key.inventoryId);
                status.setQty(BigDecimal.ZERO);
                status.setAmount(BigDecimal.ZERO);
                status.setCreatedBy(operator);
                status.setUpdatedBy(operator);
                status.setDeleteYn("N");
                inventoryStatusRepository.saveAndFlush(status);

                // 생성 후 락 획득을 위해 다시 조회
                status = inventoryStatusRepository.findByIdWithLock(companyId, key.warehouseId, key.inventoryId)
                        .orElseThrow(() -> new IllegalStateException("재고 상태 행 생성 및 락 획득 실패"));
            } else {
                status = statusOpt.get();
            }
            statusMap.put(key, status);
        }

        // 3. 실제 비즈니스 로직 순차 수행
        for (TxItem item : request.getItems()) {
            LocalDate date = item.getTxDate() != null ? item.getTxDate() : LocalDate.now();

            if ("IN".equalsIgnoreCase(item.getTxTypeCode())) {
                // 단순 입고
                executeCheckIn(companyId, item, statusMap.get(new StatusKey(item.getWarehouseId(), item.getInventoryId())), date, operator);
            } else if ("OUT".equalsIgnoreCase(item.getTxTypeCode())) {
                // 단순 출고
                executeCheckOut(companyId, item, statusMap.get(new StatusKey(item.getWarehouseId(), item.getInventoryId())), date, operator);
            } else if ("MOVE".equalsIgnoreCase(item.getTxTypeCode())) {
                // 창고 이동 (이동출고 + 이동입고)
                executeTransfer(companyId, item, statusMap, date, operator);
            } else if ("ADJ".equalsIgnoreCase(item.getTxTypeCode())) {
                // 조정
                executeAdjustment(companyId, item, statusMap.get(new StatusKey(item.getWarehouseId(), item.getInventoryId())), date, operator);
            }
        }
    }

    private void executeCheckIn(String companyId, TxItem item, InventoryStatus status, LocalDate date, String operator) {
        BigDecimal qty = item.getQty();
        BigDecimal price = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal amount = qty.multiply(price);

        BigDecimal nextQty = status.getQty().add(qty);
        BigDecimal nextAmount = status.getAmount().add(amount);

        status.setQty(nextQty);
        status.setAmount(nextAmount);
        status.setUpdatedBy(operator);
        inventoryStatusRepository.save(status);

        // 이력 기록
        InventoryHistory history = new InventoryHistory();
        history.setCompanyId(companyId);
        history.setWarehouseId(status.getWarehouseId());
        history.setInventoryId(status.getInventoryId());
        history.setTxTypeCode("IN");
        history.setQty(qty);
        history.setUnitPrice(price);
        history.setAmount(amount);
        history.setTxDate(date);
        history.setUserId(operator);
        history.setCreatedBy(operator);
        history.setUpdatedBy(operator);
        inventoryHistoryRepository.save(history);
    }

    private void executeCheckOut(String companyId, TxItem item, InventoryStatus status, LocalDate date, String operator) {
        BigDecimal qty = item.getQty();
        
        // 현재 평균단가 계산
        BigDecimal currentQty = status.getQty();
        BigDecimal currentAmount = status.getAmount();
        BigDecimal unitPrice = BigDecimal.ZERO;
        if (currentQty.compareTo(BigDecimal.ZERO) > 0) {
            unitPrice = currentAmount.divide(currentQty, 4, RoundingMode.HALF_UP);
        }

        BigDecimal amount = qty.multiply(unitPrice);

        BigDecimal nextQty = currentQty.subtract(qty);
        BigDecimal nextAmount = currentAmount.subtract(amount);
        if (nextQty.compareTo(BigDecimal.ZERO) < 0) {
            nextQty = BigDecimal.ZERO;
        }
        if (nextAmount.compareTo(BigDecimal.ZERO) < 0) {
            nextAmount = BigDecimal.ZERO;
        }

        status.setQty(nextQty);
        status.setAmount(nextAmount);
        status.setUpdatedBy(operator);
        inventoryStatusRepository.save(status);

        // 이력 기록
        InventoryHistory history = new InventoryHistory();
        history.setCompanyId(companyId);
        history.setWarehouseId(status.getWarehouseId());
        history.setInventoryId(status.getInventoryId());
        history.setTxTypeCode("OUT");
        history.setQty(qty.negate()); // 출고는 음수로 표기
        history.setUnitPrice(unitPrice);
        history.setAmount(amount.negate());
        history.setTxDate(date);
        history.setUserId(operator);
        history.setCreatedBy(operator);
        history.setUpdatedBy(operator);
        inventoryHistoryRepository.save(history);
    }

    private void executeTransfer(String companyId, TxItem item, Map<StatusKey, InventoryStatus> statusMap, LocalDate date, String operator) {
        // 1. 보내는 창고 출고 처리
        InventoryStatus sourceStatus = statusMap.get(new StatusKey(item.getWarehouseId(), item.getInventoryId()));
        BigDecimal qty = item.getQty();

        BigDecimal sourceQty = sourceStatus.getQty();
        BigDecimal sourceAmount = sourceStatus.getAmount();
        BigDecimal avgPrice = BigDecimal.ZERO;
        if (sourceQty.compareTo(BigDecimal.ZERO) > 0) {
            avgPrice = sourceAmount.divide(sourceQty, 4, RoundingMode.HALF_UP);
        }

        BigDecimal outAmount = qty.multiply(avgPrice);

        BigDecimal nextSourceQty = sourceQty.subtract(qty);
        BigDecimal nextSourceAmount = sourceAmount.subtract(outAmount);
        if (nextSourceQty.compareTo(BigDecimal.ZERO) < 0) nextSourceQty = BigDecimal.ZERO;
        if (nextSourceAmount.compareTo(BigDecimal.ZERO) < 0) nextSourceAmount = BigDecimal.ZERO;

        sourceStatus.setQty(nextSourceQty);
        sourceStatus.setAmount(nextSourceAmount);
        sourceStatus.setUpdatedBy(operator);
        inventoryStatusRepository.save(sourceStatus);

        // 이동출고 이력 기록
        InventoryHistory outHistory = new InventoryHistory();
        outHistory.setCompanyId(companyId);
        outHistory.setWarehouseId(sourceStatus.getWarehouseId());
        outHistory.setInventoryId(sourceStatus.getInventoryId());
        outHistory.setTxTypeCode("MOVE_OUT");
        outHistory.setQty(qty.negate());
        outHistory.setUnitPrice(avgPrice);
        outHistory.setAmount(outAmount.negate());
        outHistory.setTxDate(date);
        outHistory.setUserId(operator);
        outHistory.setCreatedBy(operator);
        outHistory.setUpdatedBy(operator);
        
        // saveAndFlush하여 historyNo를 즉시 획득
        outHistory = inventoryHistoryRepository.saveAndFlush(outHistory);

        // 2. 받는 창고 입고 처리
        InventoryStatus targetStatus = statusMap.get(new StatusKey(item.getTargetWarehouseId(), item.getInventoryId()));
        BigDecimal targetQty = targetStatus.getQty();
        BigDecimal targetAmount = targetStatus.getAmount();

        BigDecimal nextTargetQty = targetQty.add(qty);
        BigDecimal nextTargetAmount = targetAmount.add(outAmount);

        targetStatus.setQty(nextTargetQty);
        targetStatus.setAmount(nextTargetAmount);
        targetStatus.setUpdatedBy(operator);
        inventoryStatusRepository.save(targetStatus);

        // 이동입고 이력 기록 (보내는 곳의 이력번호를 refNo에 입력)
        InventoryHistory inHistory = new InventoryHistory();
        inHistory.setCompanyId(companyId);
        inHistory.setWarehouseId(targetStatus.getWarehouseId());
        inHistory.setInventoryId(targetStatus.getInventoryId());
        inHistory.setTxTypeCode("MOVE_IN");
        inHistory.setQty(qty);
        inHistory.setUnitPrice(avgPrice);
        inHistory.setAmount(outAmount);
        inHistory.setTxDate(date);
        inHistory.setUserId(operator);
        inHistory.setRefNo(outHistory.getHistoryNo().toString());
        inHistory.setRefModule("INVENTORY");
        inHistory.setCreatedBy(operator);
        inHistory.setUpdatedBy(operator);
        inventoryHistoryRepository.save(inHistory);
    }

    private void executeAdjustment(String companyId, TxItem item, InventoryStatus status, LocalDate date, String operator) {
        BigDecimal qty = item.getQty(); // 양수면 증가, 음수면 감소
        BigDecimal price = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
        
        BigDecimal currentQty = status.getQty();
        BigDecimal currentAmount = status.getAmount();

        BigDecimal adjAmount;
        if (qty.compareTo(BigDecimal.ZERO) >= 0) {
            adjAmount = qty.multiply(price);
        } else {
            BigDecimal avgPrice = BigDecimal.ZERO;
            if (currentQty.compareTo(BigDecimal.ZERO) > 0) {
                avgPrice = currentAmount.divide(currentQty, 4, RoundingMode.HALF_UP);
            }
            adjAmount = qty.multiply(avgPrice); // 음수
        }

        BigDecimal nextQty = currentQty.add(qty);
        BigDecimal nextAmount = currentAmount.add(adjAmount);
        if (nextQty.compareTo(BigDecimal.ZERO) < 0) nextQty = BigDecimal.ZERO;
        if (nextAmount.compareTo(BigDecimal.ZERO) < 0) nextAmount = BigDecimal.ZERO;

        status.setQty(nextQty);
        status.setAmount(nextAmount);
        status.setUpdatedBy(operator);
        inventoryStatusRepository.save(status);

        // 이력 기록
        InventoryHistory history = new InventoryHistory();
        history.setCompanyId(companyId);
        history.setWarehouseId(status.getWarehouseId());
        history.setInventoryId(status.getInventoryId());
        history.setTxTypeCode("ADJ");
        history.setQty(qty);
        history.setUnitPrice(price);
        history.setAmount(adjAmount);
        history.setTxDate(date);
        history.setUserId(operator);
        history.setCreatedBy(operator);
        history.setUpdatedBy(operator);
        inventoryHistoryRepository.save(history);
    }

    @Transactional
    public void closeMonth(String companyId, String closingYm, String operator) {
        // 기존 마감 내역 삭제
        List<InventoryMonthlyClosing> existings = inventoryMonthlyClosingRepository
                .findByCompanyIdAndClosingYmAndDeleteYn(companyId, closingYm, "N");
        for (InventoryMonthlyClosing c : existings) {
            c.setDeleteYn("Y");
            c.setUpdatedBy(operator);
            inventoryMonthlyClosingRepository.save(c);
        }

        // 현재 재고현황 전체를 조회하여 마감 데이터로 이관
        List<InventoryStatus> statuses = inventoryStatusRepository.findByCompanyIdAndDeleteYn(companyId, "N");

        for (InventoryStatus status : statuses) {
            InventoryMonthlyClosing closing = new InventoryMonthlyClosing();
            closing.setCompanyId(companyId);
            closing.setWarehouseId(status.getWarehouseId());
            closing.setInventoryId(status.getInventoryId());
            closing.setClosingYm(closingYm);

            // 해당 월에 발생한 입고/출고/이동/조정 내역을 집계 (txDate가 해당 년월에 포함되는 이력)
            LocalDate start = LocalDate.parse(closingYm.substring(0, 4) + "-" + closingYm.substring(4, 6) + "-01");
            LocalDate end = start.plusMonths(1).minusDays(1);

            List<InventoryHistory> histories = inventoryHistoryRepository
                    .findByCompanyIdAndWarehouseIdAndDeleteYn(companyId, status.getWarehouseId(), "N")
                    .stream()
                    .filter(h -> h.getInventoryId().equals(status.getInventoryId()))
                    .filter(h -> !h.getTxDate().isBefore(start) && !h.getTxDate().isAfter(end))
                    .toList();

            BigDecimal inQty = BigDecimal.ZERO;
            BigDecimal inAmt = BigDecimal.ZERO;
            BigDecimal outQty = BigDecimal.ZERO;
            BigDecimal outAmt = BigDecimal.ZERO;
            BigDecimal moveQty = BigDecimal.ZERO;
            BigDecimal moveAmt = BigDecimal.ZERO;
            BigDecimal adjQty = BigDecimal.ZERO;
            BigDecimal adjAmt = BigDecimal.ZERO;

            for (InventoryHistory h : histories) {
                switch (h.getTxTypeCode().toUpperCase()) {
                    case "IN" -> {
                        inQty = inQty.add(h.getQty());
                        inAmt = inAmt.add(h.getAmount());
                    }
                    case "OUT" -> {
                        outQty = outQty.add(h.getQty().abs()); // 출고는 절대값 합산
                        outAmt = outAmt.add(h.getAmount().abs());
                    }
                    case "MOVE_IN" -> {
                        moveQty = moveQty.add(h.getQty());
                        moveAmt = moveAmt.add(h.getAmount());
                    }
                    case "MOVE_OUT" -> {
                        moveQty = moveQty.add(h.getQty()); // 이동출고는 음수이므로 더하면 감산됨
                        moveAmt = moveAmt.add(h.getAmount());
                    }
                    case "ADJ" -> {
                        adjQty = adjQty.add(h.getQty());
                        adjAmt = adjAmt.add(h.getAmount());
                    }
                }
            }

            closing.setInQty(inQty);
            closing.setInAmount(inAmt);
            closing.setOutQty(outQty);
            closing.setOutAmount(outAmt);
            closing.setMoveQty(moveQty);
            closing.setMoveAmount(moveAmt);
            closing.setAdjQty(adjQty);
            closing.setAdjAmount(adjAmt);

            // 마감 수량 및 마감 금액 (현재 수량 및 금액으로 설정)
            closing.setClosingQty(status.getQty());
            closing.setClosingAmount(status.getAmount());
            closing.setCreatedBy(operator);
            closing.setUpdatedBy(operator);
            closing.setDeleteYn("N");

            inventoryMonthlyClosingRepository.save(closing);
        }
    }
}
