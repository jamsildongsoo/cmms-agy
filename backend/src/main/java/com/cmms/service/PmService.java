package com.cmms.service;

import com.cmms.dto.PmDto.*;
import com.cmms.model.*;
import com.cmms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class PmService {

    @Autowired
    private PmRecordRepository pmRecordRepository;

    @Autowired
    private PmRecordItemRepository pmRecordItemRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private EquipmentCheckCycleRepository equipmentCheckCycleRepository;

    @Autowired
    private EquipmentCheckItemRepository equipmentCheckItemRepository;

    @Autowired
    private SequenceService sequenceService;

    @Transactional(readOnly = true)
    public List<PmScheduleResponse> getPmSchedules(String companyId, LocalDate targetDate) {
        List<EquipmentCheckCycle> cycles = equipmentCheckCycleRepository.findAll().stream()
                .filter(c -> companyId.equals(c.getCompanyId()) && "N".equals(c.getDeleteYn()))
                .filter(c -> c.getNextCheckDate() != null && !c.getNextCheckDate().isAfter(targetDate))
                .toList();

        List<PmScheduleResponse> result = new ArrayList<>();
        for (EquipmentCheckCycle cycle : cycles) {
            Equipment eq = equipmentRepository.findById(new EquipmentId(companyId, cycle.getPlantId(), cycle.getEquipmentId()))
                    .orElse(null);
            
            if (eq != null && "N".equals(eq.getDeleteYn())) {
                PmScheduleResponse res = new PmScheduleResponse();
                res.setEquipmentId(cycle.getEquipmentId());
                res.setEquipmentName(eq.getName());
                res.setPlantId(cycle.getPlantId());
                res.setCheckTypeCode(cycle.getCheckTypeCode());
                res.setCycleVal(cycle.getCycleVal());
                res.setCycleUnit(cycle.getCycleUnit());
                res.setLastCheckDate(cycle.getLastCheckDate());
                res.setNextCheckDate(cycle.getNextCheckDate());
                result.add(res);
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<PmRecord> getPmRecordsByCompany(String companyId) {
        return pmRecordRepository.findByCompanyIdAndDeleteYn(companyId, "N");
    }

    @Transactional(readOnly = true)
    public PmSaveRequest getPmRecordDetails(String companyId, String plantId, String id) {
        PmRecord record = pmRecordRepository.findById(new PmRecordId(companyId, plantId, id))
                .filter(r -> "N".equals(r.getDeleteYn()))
                .orElseThrow(() -> new IllegalArgumentException("점검 기록을 찾을 수 없습니다."));

        List<PmRecordItem> items = pmRecordItemRepository.findByCompanyIdAndPlantIdAndPmRecordIdOrderByItemNoAsc(companyId, plantId, id);

        PmSaveRequest response = new PmSaveRequest();
        response.setPmRecord(record);
        response.setCheckItems(items);
        return response;
    }

    @Transactional(readOnly = true)
    public List<PmRecordItem> getInitialCheckItems(String companyId, String plantId, String equipmentId) {
        List<EquipmentCheckItem> eqItems = equipmentCheckItemRepository
                .findByCompanyIdAndPlantIdAndEquipmentIdOrderByItemNoAsc(companyId, plantId, equipmentId);

        List<PmRecordItem> result = new ArrayList<>();
        for (EquipmentCheckItem eqItem : eqItems) {
            PmRecordItem item = new PmRecordItem();
            item.setItemNo(eqItem.getItemNo());
            item.setCheckName(eqItem.getCheckName());
            item.setCheckMethod(eqItem.getCheckMethod());
            item.setMinValue(eqItem.getMinValue());
            item.setMaxValue(eqItem.getMaxValue());
            item.setBaseValue(eqItem.getBaseValue());
            item.setUnit(eqItem.getUnit());
            item.setCheckValue(null);
            result.add(item);
        }
        return result;
    }

    @Transactional
    public PmRecord savePmRecord(String companyId, PmSaveRequest request, String operator) {
        PmRecord pm = request.getPmRecord();
        pm.setCompanyId(companyId);

        boolean isNew = pm.getId() == null || pm.getId().trim().isEmpty();
        if (isNew) {
            String pmNo = sequenceService.generateNextNo(companyId, "PM", pm.getDepartmentId());
            pm.setId(pmNo);
            pm.setCreatedBy(operator);
        }
        pm.setUpdatedBy(operator);
        pm.setDeleteYn("N");

        PmRecord savedPm = pmRecordRepository.save(pm);

        pmRecordItemRepository.deleteByCompanyIdAndPlantIdAndPmRecordId(companyId, pm.getPlantId(), pm.getId());

        List<PmRecordItem> checkItems = request.getCheckItems();
        if (checkItems != null) {
            for (PmRecordItem item : checkItems) {
                item.setCompanyId(companyId);
                item.setPlantId(pm.getPlantId());
                item.setPmRecordId(pm.getId());
                pmRecordItemRepository.save(item);
            }
        }

        if ("S".equals(pm.getStatus()) || "C".equals(pm.getStatus())) {
            updateCheckCycleSchedule(companyId, pm, operator);
        }

        return savedPm;
    }

    private void updateCheckCycleSchedule(String companyId, PmRecord pm, String operator) {
        EquipmentCheckCycleId cycleId = new EquipmentCheckCycleId(companyId, pm.getPlantId(), pm.getEquipmentId(), pm.getCheckTypeCode());
        equipmentCheckCycleRepository.findById(cycleId).ifPresent(cycle -> {
            cycle.setLastCheckDate(pm.getWorkDate());
            cycle.setNextCheckDate(calculateNextDate(pm.getWorkDate(), cycle.getCycleVal(), cycle.getCycleUnit()));
            cycle.setUpdatedBy(operator);
            equipmentCheckCycleRepository.save(cycle);
        });
    }

    private LocalDate calculateNextDate(LocalDate lastDate, int val, String unit) {
        if (lastDate == null) return null;
        return switch (unit.toUpperCase()) {
            case "D" -> lastDate.plusDays(val);
            case "W" -> lastDate.plusWeeks(val);
            case "M" -> lastDate.plusMonths(val);
            case "Y" -> lastDate.plusYears(val);
            default -> lastDate.plusMonths(val);
        };
    }

    @Transactional
    public void deletePmRecord(String companyId, String plantId, String id, String operator) {
        PmRecord pm = pmRecordRepository.findById(new PmRecordId(companyId, plantId, id))
                .filter(r -> "N".equals(r.getDeleteYn()))
                .orElseThrow(() -> new IllegalArgumentException("점검 기록을 찾을 수 없습니다."));
        pm.setDeleteYn("Y");
        pm.setUpdatedBy(operator);
        pmRecordRepository.save(pm);
    }
}
