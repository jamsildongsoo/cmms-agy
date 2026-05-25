package com.cmms.service;

import com.cmms.dto.MasterDto.EquipmentSaveRequest;
import com.cmms.model.*;
import com.cmms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringWriter;
import java.time.LocalDate;
import java.util.List;

@Service
public class MasterService {

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private EquipmentCheckItemRepository equipmentCheckItemRepository;

    @Autowired
    private EquipmentCheckCycleRepository equipmentCheckCycleRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    // ==========================================
    // 1. 설비 마스터 (Equipment)
    // ==========================================
    @Transactional(readOnly = true)
    public List<Equipment> getEquipmentsByCompany(String companyId) {
        return equipmentRepository.findByCompanyIdAndDeleteYn(companyId, "N");
    }

    @Transactional(readOnly = true)
    public List<Equipment> getEquipmentsByPlant(String companyId, String plantId) {
        return equipmentRepository.findByCompanyIdAndPlantIdAndDeleteYn(companyId, plantId, "N");
    }

    @Transactional(readOnly = true)
    public EquipmentSaveRequest getEquipmentWithDetails(String companyId, String plantId, String id) {
        Equipment eq = equipmentRepository.findById(new EquipmentId(companyId, plantId, id))
                .filter(e -> "N".equals(e.getDeleteYn()))
                .orElseThrow(() -> new IllegalArgumentException("설비를 찾을 수 없습니다."));

        List<EquipmentCheckItem> checkItems = equipmentCheckItemRepository
                .findByCompanyIdAndPlantIdAndEquipmentIdOrderByItemNoAsc(companyId, plantId, id);

        List<EquipmentCheckCycle> checkCycles = equipmentCheckCycleRepository
                .findByCompanyIdAndPlantIdAndEquipmentIdAndDeleteYn(companyId, plantId, id, "N");

        EquipmentSaveRequest response = new EquipmentSaveRequest();
        response.setEquipment(eq);
        response.setCheckItems(checkItems);
        response.setCheckCycles(checkCycles);
        return response;
    }

    @Transactional
    public Equipment saveEquipment(String companyId, EquipmentSaveRequest request, String operator) {
        Equipment reqEq = request.getEquipment();
        reqEq.setCompanyId(companyId);

        EquipmentId eqId = new EquipmentId(companyId, reqEq.getPlantId(), reqEq.getId());
        boolean isNew = !equipmentRepository.existsById(eqId);

        if (isNew) {
            reqEq.setCreatedBy(operator);
        }
        reqEq.setUpdatedBy(operator);
        reqEq.setDeleteYn("N");

        Equipment savedEq = equipmentRepository.save(reqEq);

        // 점검 항목 업데이트: 기존 데이터 삭제 후 재생성
        equipmentCheckItemRepository.deleteByCompanyIdAndPlantIdAndEquipmentId(companyId, reqEq.getPlantId(), reqEq.getId());

        List<EquipmentCheckItem> checkItems = request.getCheckItems();
        if (checkItems != null) {
            int seq = 1;
            for (EquipmentCheckItem item : checkItems) {
                item.setCompanyId(companyId);
                item.setPlantId(reqEq.getPlantId());
                item.setEquipmentId(reqEq.getId());
                item.setItemNo(seq++);
                equipmentCheckItemRepository.save(item);
            }
        }

        // 점검 주기 업데이트: 기존 데이터 논리삭제 후 재생성
        List<EquipmentCheckCycle> oldCycles = equipmentCheckCycleRepository
                .findByCompanyIdAndPlantIdAndEquipmentIdAndDeleteYn(companyId, reqEq.getPlantId(), reqEq.getId(), "N");
        for (EquipmentCheckCycle old : oldCycles) {
            old.setDeleteYn("Y");
            old.setUpdatedBy(operator);
            equipmentCheckCycleRepository.save(old);
        }

        List<EquipmentCheckCycle> checkCycles = request.getCheckCycles();
        if (checkCycles != null) {
            for (EquipmentCheckCycle cycle : checkCycles) {
                cycle.setCompanyId(companyId);
                cycle.setPlantId(reqEq.getPlantId());
                cycle.setEquipmentId(reqEq.getId());
                cycle.setDeleteYn("N");
                cycle.setCreatedBy(operator);
                cycle.setUpdatedBy(operator);
                // nextCheckDate 자동 계산: lastCheckDate가 있으면 주기를 더해 계산
                if (cycle.getLastCheckDate() != null && cycle.getCycleVal() != null) {
                    LocalDate base = cycle.getLastCheckDate();
                    LocalDate next;
                    switch (cycle.getCycleUnit()) {
                        case "D": next = base.plusDays(cycle.getCycleVal()); break;
                        case "W": next = base.plusWeeks(cycle.getCycleVal()); break;
                        case "Y": next = base.plusYears(cycle.getCycleVal()); break;
                        default:  next = base.plusMonths(cycle.getCycleVal()); break;
                    }
                    cycle.setNextCheckDate(next);
                }
                equipmentCheckCycleRepository.save(cycle);
            }
        }


        return savedEq;
    }

    @Transactional
    public void deleteEquipment(String companyId, String plantId, String id, String operator) {
        Equipment eq = equipmentRepository.findById(new EquipmentId(companyId, plantId, id))
                .filter(e -> "N".equals(e.getDeleteYn()))
                .orElseThrow(() -> new IllegalArgumentException("설비를 찾을 수 없습니다."));
        eq.setDeleteYn("Y");
        eq.setUpdatedBy(operator);
        equipmentRepository.save(eq);
    }

    // ==========================================
    // 2. 재고 마스터 (Inventory)
    // ==========================================
    @Transactional(readOnly = true)
    public List<Inventory> getInventoriesByCompany(String companyId) {
        return inventoryRepository.findByCompanyIdAndDeleteYn(companyId, "N");
    }

    @Transactional(readOnly = true)
    public Inventory getInventoryById(String companyId, String id) {
        return inventoryRepository.findById(new InventoryId(companyId, id))
                .filter(i -> "N".equals(i.getDeleteYn()))
                .orElseThrow(() -> new IllegalArgumentException("재고 품목을 찾을 수 없습니다."));
    }

    @Transactional
    public Inventory saveInventory(String companyId, Inventory inventory, String operator) {
        inventory.setCompanyId(companyId);
        InventoryId invId = new InventoryId(companyId, inventory.getId());
        boolean isNew = !inventoryRepository.existsById(invId);

        if (isNew) {
            inventory.setCreatedBy(operator);
        }
        inventory.setUpdatedBy(operator);
        inventory.setDeleteYn("N");

        return inventoryRepository.save(inventory);
    }

    @Transactional
    public void deleteInventory(String companyId, String id, String operator) {
        Inventory inventory = inventoryRepository.findById(new InventoryId(companyId, id))
                .filter(i -> "N".equals(i.getDeleteYn()))
                .orElseThrow(() -> new IllegalArgumentException("재고 품목을 찾을 수 없습니다."));
        inventory.setDeleteYn("Y");
        inventory.setUpdatedBy(operator);
        inventoryRepository.save(inventory);
    }

    // ==========================================
    // 3. CSV EXPORT HELPERS
    // ==========================================
    @Transactional(readOnly = true)
    public String exportEquipmentsToCsv(String companyId) {
        List<Equipment> list = getEquipmentsByCompany(companyId);
        StringWriter writer = new StringWriter();
        
        // UTF-8 BOM 추가 (Excel 깨짐 방지)
        writer.write('\ufeff');
        writer.write("설비코드,설비명,플랜트,설치위치,설비타입,설치일자,작업허가대상,제조사,모델,일련번호,비고\n");
        for (Equipment eq : list) {
            writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    escapeCsv(eq.getId()),
                    escapeCsv(eq.getName()),
                    escapeCsv(eq.getPlantId()),
                    escapeCsv(eq.getLocation()),
                    escapeCsv(eq.getEqTypeCode()),
                    eq.getInstallDate() != null ? eq.getInstallDate().toString() : "",
                    escapeCsv(eq.getWorkPermitYn()),
                    escapeCsv(eq.getMakerName()),
                    escapeCsv(eq.getModel()),
                    escapeCsv(eq.getSerialNumber()),
                    escapeCsv(eq.getRemarks())
            ));
        }
        return writer.toString();
    }

    @Transactional(readOnly = true)
    public String exportInventoriesToCsv(String companyId) {
        List<Inventory> list = getInventoriesByCompany(companyId);
        StringWriter writer = new StringWriter();
        
        writer.write('\ufeff');
        writer.write("자재코드,자재명,자재타입,관리부서,단위,제조사,스펙,모델,일련번호,안전재고,재주문점,리드타임(일),비고\n");
        for (Inventory inv : list) {
            writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    escapeCsv(inv.getId()),
                    escapeCsv(inv.getName()),
                    escapeCsv(inv.getItemTypeCode()),
                    escapeCsv(inv.getDepartmentId()),
                    escapeCsv(inv.getUnit()),
                    escapeCsv(inv.getMakerName()),
                    escapeCsv(inv.getSpec()),
                    escapeCsv(inv.getModel()),
                    escapeCsv(inv.getSerialNumber()),
                    inv.getSafetyQty() != null ? inv.getSafetyQty().toString() : "0",
                    inv.getReorderQty() != null ? inv.getReorderQty().toString() : "0",
                    inv.getLeadTimeDays() != null ? inv.getLeadTimeDays().toString() : "0",
                    escapeCsv(inv.getRemarks())
            ));
        }
        return writer.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        String clean = value.replace("\n", " ").replace("\r", " ").replace(",", " ");
        return clean;
    }
}
