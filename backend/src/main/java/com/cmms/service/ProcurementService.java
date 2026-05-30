package com.cmms.service;

import com.cmms.constant.DocStatus;
import com.cmms.dto.InventoryTxDto;
import com.cmms.dto.PurchaseRequestDto.*;
import com.cmms.model.*;
import com.cmms.repository.*;
import com.cmms.security.AppModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProcurementService {

    @Autowired private PurchaseRequestRepository purchaseRequestRepository;
    @Autowired private PurchaseRequestItemRepository purchaseRequestItemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PlantRepository plantRepository;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private SequenceService sequenceService;
    @Autowired private InventoryTransactionService inventoryTransactionService;
    @Autowired private InventoryHistoryRepository inventoryHistoryRepository;
    @Autowired private InventoryStatusRepository inventoryStatusRepository;

    // ==========================================
    // 조회 (플랜트 스코프 적용)
    // ==========================================
    @Transactional(readOnly = true)
    public List<PurchaseRequest> getRequests(String companyId, String operator, String reqPlantId) {
        String activePlant = resolveActivePlantId(companyId, operator, reqPlantId);
        if (activePlant == null) {
            // 멀티+전체 또는 lastLoginPlantId null → 전 플랜트 합산
            return purchaseRequestRepository.findByCompanyIdAndDeleteYn(companyId, "N");
        }
        return purchaseRequestRepository.findByCompanyIdAndPlantIdAndDeleteYn(companyId, activePlant, "N");
    }

    @Transactional(readOnly = true)
    public RequestDetail getRequestDetail(String companyId, String id) {
        PurchaseRequest pr = purchaseRequestRepository.findByCompanyIdAndId(companyId, id)
                .filter(p -> "N".equals(p.getDeleteYn()))
                .orElseThrow(() -> new IllegalArgumentException("구매요청을 찾을 수 없습니다."));
        List<PurchaseRequestItem> items = purchaseRequestItemRepository.findByCompanyIdAndRequestId(companyId, id);
        RequestDetail detail = new RequestDetail();
        detail.setHeader(pr);
        // 라인 → DTO 변환
        List<ItemLine> lines = new ArrayList<>();
        for (PurchaseRequestItem it : items) {
            ItemLine l = new ItemLine();
            l.setLineNo(it.getLineNo());
            l.setInventoryId(it.getInventoryId());
            l.setQty(it.getQty());
            l.setUnit(it.getUnit());
            l.setRemarks(it.getRemarks());
            lines.add(l);
        }
        detail.setItems(lines);
        return detail;
    }

    // ==========================================
    // 저장 / 확정
    // ==========================================
    @Transactional
    public PurchaseRequest createOrUpdate(String companyId, SaveRequest req, String operator) {
        PurchaseRequest header = req.getHeader();
        boolean isNew = header.getId() == null || header.getId().isBlank();

        // 플랜트 스코프: 비멀티 사용자는 본인 lastLoginPlantId로 강제, 멀티는 클라 plantId 사용
        User user = userRepository.findByCompanyIdAndId(companyId, operator)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));
        boolean multi = isMultiPlant(companyId, user.getRoleId());
        if (!multi) {
            if (user.getLastLoginPlantId() == null) {
                throw new IllegalArgumentException("지정 플랜트가 없어 구매요청을 생성할 수 없습니다.");
            }
            header.setPlantId(user.getLastLoginPlantId());
        }

        if (isNew) {
            String prNo = sequenceService.generateNextNo(companyId, AppModule.PUR.name(), user.getDepartmentId());
            header.setId(prNo);
            header.setRequesterId(operator);
            header.setRequestDate(header.getRequestDate() != null ? header.getRequestDate() : LocalDate.now());
            header.setCompanyId(companyId);
            header.setStatus(DocStatus.TEMP.code());  // T
            header.setCreatedBy(operator);
            header.setUpdatedBy(operator);
            purchaseRequestRepository.save(header);
        } else {
            PurchaseRequest existing = purchaseRequestRepository.findByCompanyIdAndId(companyId, header.getId())
                    .filter(p -> "N".equals(p.getDeleteYn()))
                    .orElseThrow(() -> new IllegalArgumentException("구매요청을 찾을 수 없습니다."));
            if (!DocStatus.TEMP.code().equals(existing.getStatus())) {
                throw new IllegalArgumentException("저장 상태(T)에서만 수정할 수 있습니다.");
            }
            existing.setWarehouseId(header.getWarehouseId());
            existing.setRequestType(header.getRequestType());
            existing.setRemarks(header.getRemarks());
            existing.setUpdatedBy(operator);
            purchaseRequestRepository.save(existing);
            header = existing;
            // 라인 재생성을 위해 기존 삭제
            purchaseRequestItemRepository.deleteByCompanyIdAndRequestId(companyId, header.getId());
            purchaseRequestItemRepository.flush();
        }

        // 라인 저장
        if (req.getItems() != null) {
            int lineNo = 1;
            for (ItemLine l : req.getItems()) {
                PurchaseRequestItem item = new PurchaseRequestItem();
                item.setCompanyId(companyId);
                item.setRequestId(header.getId());
                item.setLineNo(lineNo++);
                item.setInventoryId(l.getInventoryId());
                item.setQty(l.getQty());
                item.setUnit(l.getUnit());
                item.setReceivedQty(BigDecimal.ZERO);
                item.setRemarks(l.getRemarks());
                purchaseRequestItemRepository.save(item);
            }
        }

        // 확정 요청 시
        if (req.isConfirm()) {
            header.setStatus(DocStatus.SELF_CONFIRMED.code());  // S
            header.setUpdatedBy(operator);
            purchaseRequestRepository.save(header);
        }
        return header;
    }

    @Transactional
    public PurchaseRequest confirm(String companyId, String requestId, String operator) {
        PurchaseRequest pr = mustGetActive(companyId, requestId);
        if (!DocStatus.TEMP.code().equals(pr.getStatus())) {
            throw new IllegalArgumentException("저장 상태(T)에서만 확정할 수 있습니다.");
        }
        pr.setStatus(DocStatus.SELF_CONFIRMED.code());
        pr.setUpdatedBy(operator);
        return purchaseRequestRepository.save(pr);
    }

    // ==========================================
    // 발주 / 배송 / 종료 (절차)
    // ==========================================
    @Transactional
    public PurchaseRequest placeOrder(String companyId, OrderRequest req, String operator) {
        PurchaseRequest pr = mustGetConfirmed(companyId, req.getRequestId());
        pr.setVendorId(req.getVendorId());
        pr.setOrderDate(req.getOrderDate() != null ? req.getOrderDate() : LocalDate.now());
        pr.setEtaDate(req.getEtaDate());
        pr.setProcStatus(DocStatus.ORDERED.code());  // O
        pr.setUpdatedBy(operator);
        return purchaseRequestRepository.save(pr);
    }

    @Transactional
    public PurchaseRequest startShipping(String companyId, ShipRequest req, String operator) {
        PurchaseRequest pr = mustGetConfirmed(companyId, req.getRequestId());
        pr.setShipStartDate(req.getShipStartDate() != null ? req.getShipStartDate() : LocalDate.now());
        pr.setProcStatus(DocStatus.SHIPPING.code());  // D
        pr.setUpdatedBy(operator);
        return purchaseRequestRepository.save(pr);
    }

    @Transactional
    public PurchaseRequest close(String companyId, String requestId, String operator) {
        PurchaseRequest pr = mustGetConfirmed(companyId, requestId);
        if (DocStatus.CLOSED.code().equals(pr.getProcStatus())) {
            throw new IllegalArgumentException("이미 종료된 요청입니다.");
        }
        pr.setProcStatus(DocStatus.CLOSED.code());  // E
        pr.setUpdatedBy(operator);
        return purchaseRequestRepository.save(pr);
    }

    // ==========================================
    // 입고 (재고 반영)
    // ==========================================
    @Transactional
    public PurchaseRequest receive(String companyId, ReceiveRequest req, String operator) {
        PurchaseRequest pr = mustGetConfirmed(companyId, req.getRequestId());
        if (DocStatus.CLOSED.code().equals(pr.getProcStatus())) {
            throw new IllegalArgumentException("종료된 요청에는 입고할 수 없습니다.");
        }
        if (req.getLines() == null || req.getLines().isEmpty()) {
            throw new IllegalArgumentException("입고 라인이 비어 있습니다.");
        }

        // 입고 라인 → InventoryTx items 변환
        List<PurchaseRequestItem> prItems = purchaseRequestItemRepository.findByCompanyIdAndRequestId(companyId, pr.getId());

        // STK 전표번호 채번 (조작자 부서)
        String operatorDept = userRepository.findByCompanyIdAndId(companyId, operator)
                .map(User::getDepartmentId).orElse(null);
        String docNo = sequenceService.generateNextNo(companyId, AppModule.STK.name(), operatorDept);

        InventoryTxDto.InventoryTxRequest txReq = new InventoryTxDto.InventoryTxRequest();
        List<InventoryTxDto.TxItem> txItems = new ArrayList<>();
        LocalDate txDate = req.getTxDate() != null ? req.getTxDate() : LocalDate.now();

        for (ReceiveLine line : req.getLines()) {
            PurchaseRequestItem prItem = prItems.stream()
                    .filter(it -> it.getLineNo().equals(line.getLineNo()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("PR 라인 " + line.getLineNo() + "을 찾을 수 없습니다."));

            InventoryTxDto.TxItem tx = new InventoryTxDto.TxItem();
            tx.setWarehouseId(pr.getWarehouseId());
            tx.setInventoryId(prItem.getInventoryId());
            tx.setTxTypeCode("IN");
            tx.setQty(line.getQty());
            tx.setUnitPrice(line.getUnitPrice());
            tx.setTxDate(txDate);
            tx.setDocNo(docNo);
            tx.setRefNo(pr.getId());
            tx.setRefModule(AppModule.PUR.name());
            txItems.add(tx);

            // PR 라인 receivedQty 누적
            prItem.setReceivedQty(prItem.getReceivedQty().add(line.getQty()));
            purchaseRequestItemRepository.save(prItem);
        }
        txReq.setItems(txItems);
        inventoryTransactionService.processTransactions(companyId, txReq, operator);

        // proc_status: 입고 시 'I', close=true면 'E'
        pr.setProcStatus(req.isClose() ? DocStatus.CLOSED.code() : DocStatus.RECEIVED.code());
        pr.setUpdatedBy(operator);
        return purchaseRequestRepository.save(pr);
    }

    // ==========================================
    // 입고 취소 (역분개) — 전표(docNo) 단위
    // ==========================================
    @Transactional
    public void cancelReceive(String companyId, String docNo, String operator) {
        List<InventoryHistory> rows = inventoryHistoryRepository.findByCompanyIdAndDocNo(companyId, docNo);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("전표를 찾을 수 없습니다: " + docNo);
        }
        // 1) PR 입고 전표인지 확인 + 후속 transaction 존재 여부 검증
        String prId = null;
        for (InventoryHistory h : rows) {
            if (!"IN".equalsIgnoreCase(h.getTxTypeCode()) || !AppModule.PUR.name().equals(h.getRefModule())) {
                throw new IllegalArgumentException("PR 입고 전표만 취소 가능합니다.");
            }
            if (prId == null) prId = h.getRefNo();
            boolean hasSubsequent = inventoryHistoryRepository
                    .existsByCompanyIdAndWarehouseIdAndInventoryIdAndHistoryNoGreaterThan(
                            companyId, h.getWarehouseId(), h.getInventoryId(), h.getHistoryNo());
            if (hasSubsequent) {
                throw new IllegalArgumentException("후속 거래가 있어 취소할 수 없습니다 (품목 " + h.getInventoryId() + ").");
            }
        }

        // 2) 역분개 OUT 생성 (같은 docNo로 묶기)
        InventoryTxDto.InventoryTxRequest txReq = new InventoryTxDto.InventoryTxRequest();
        List<InventoryTxDto.TxItem> txItems = new ArrayList<>();
        for (InventoryHistory h : rows) {
            InventoryTxDto.TxItem tx = new InventoryTxDto.TxItem();
            tx.setWarehouseId(h.getWarehouseId());
            tx.setInventoryId(h.getInventoryId());
            tx.setTxTypeCode("OUT");
            tx.setQty(h.getQty());  // 원본 IN 수량(양수)
            tx.setTxDate(LocalDate.now());
            tx.setDocNo(docNo);  // 같은 전표번호로 묶음
            tx.setRefNo(h.getRefNo());
            tx.setRefModule(AppModule.PUR.name());
            txItems.add(tx);
        }
        txReq.setItems(txItems);
        inventoryTransactionService.processTransactions(companyId, txReq, operator);

        // 3) PR 라인 receivedQty 차감
        if (prId != null) {
            List<PurchaseRequestItem> prItems = purchaseRequestItemRepository.findByCompanyIdAndRequestId(companyId, prId);
            for (InventoryHistory h : rows) {
                for (PurchaseRequestItem pri : prItems) {
                    if (pri.getInventoryId().equals(h.getInventoryId())) {
                        pri.setReceivedQty(pri.getReceivedQty().subtract(h.getQty()));
                        if (pri.getReceivedQty().compareTo(BigDecimal.ZERO) < 0) {
                            pri.setReceivedQty(BigDecimal.ZERO);
                        }
                        purchaseRequestItemRepository.save(pri);
                        break;
                    }
                }
            }
        }
    }

    // ==========================================
    // 삭제 (T 상태에서만)
    // ==========================================
    @Transactional
    public void deleteRequest(String companyId, String requestId, String operator) {
        PurchaseRequest pr = mustGetActive(companyId, requestId);
        if (!DocStatus.TEMP.code().equals(pr.getStatus())) {
            throw new IllegalArgumentException("저장 상태(T)에서만 삭제할 수 있습니다. 확정 이후는 종료(E)로 처리하세요.");
        }
        pr.setDeleteYn("Y");
        pr.setUpdatedBy(operator);
        purchaseRequestRepository.save(pr);
    }

    // ==========================================
    // 헬퍼
    // ==========================================
    private PurchaseRequest mustGetActive(String companyId, String requestId) {
        return purchaseRequestRepository.findByCompanyIdAndId(companyId, requestId)
                .filter(p -> "N".equals(p.getDeleteYn()))
                .orElseThrow(() -> new IllegalArgumentException("구매요청을 찾을 수 없습니다."));
    }

    private PurchaseRequest mustGetConfirmed(String companyId, String requestId) {
        PurchaseRequest pr = mustGetActive(companyId, requestId);
        if (!DocStatus.SELF_CONFIRMED.code().equals(pr.getStatus())) {
            throw new IllegalArgumentException("확정(S) 상태가 아닙니다.");
        }
        return pr;
    }

    private boolean isMultiPlant(String companyId, String roleId) {
        if (roleId == null) return false;
        return roleRepository.findById(new RoleId(companyId, roleId))
                .map(r -> "Y".equals(r.getMultiPlant()))
                .orElse(false);
    }

    /**
     * 비멀티: lastLoginPlantId 강제. 멀티: 요청 plantId 사용(null=전체).
     */
    private String resolveActivePlantId(String companyId, String operator, String reqPlantId) {
        User user = userRepository.findByCompanyIdAndId(companyId, operator).orElse(null);
        if (user == null) return null;
        boolean multi = isMultiPlant(companyId, user.getRoleId());
        if (!multi) {
            return user.getLastLoginPlantId();  // null이면 전체 조회 허용
        }
        return reqPlantId;  // null=전체
    }
}
