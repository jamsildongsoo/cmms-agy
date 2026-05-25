package com.cmms.service;

import com.cmms.model.WorkPermit;
import com.cmms.model.WorkPermitId;
import com.cmms.repository.WorkPermitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkPermitService {

    @Autowired
    private WorkPermitRepository workPermitRepository;

    @Autowired
    private SequenceService sequenceService;

    @Transactional(readOnly = true)
    public List<WorkPermit> getWorkPermitsByCompany(String companyId) {
        return workPermitRepository.findByCompanyIdAndDeleteYn(companyId, "N");
    }

    @Transactional(readOnly = true)
    public WorkPermit getWorkPermitDetails(String companyId, String plantId, String id) {
        return workPermitRepository.findById(new WorkPermitId(companyId, plantId, id))
                .filter(wp -> "N".equals(wp.getDeleteYn()))
                .orElseThrow(() -> new IllegalArgumentException("작업허가서를 찾을 수 없습니다."));
    }

    @Transactional
    public WorkPermit saveWorkPermit(String companyId, WorkPermit permit, String operator) {
        permit.setCompanyId(companyId);

        boolean isNew = permit.getId() == null || permit.getId().trim().isEmpty();
        if (isNew) {
            String wpNo = sequenceService.generateNextNo(companyId, "WP", permit.getDepartmentId());
            permit.setId(wpNo);
            permit.setCreatedBy(operator);
        }
        permit.setUpdatedBy(operator);
        permit.setDeleteYn("N");

        return workPermitRepository.save(permit);
    }

    @Transactional
    public void deleteWorkPermit(String companyId, String plantId, String id, String operator) {
        WorkPermit wp = workPermitRepository.findById(new WorkPermitId(companyId, plantId, id))
                .filter(r -> "N".equals(r.getDeleteYn()))
                .orElseThrow(() -> new IllegalArgumentException("작업허가서를 찾을 수 없습니다."));
        wp.setDeleteYn("Y");
        wp.setUpdatedBy(operator);
        workPermitRepository.save(wp);
    }
}
