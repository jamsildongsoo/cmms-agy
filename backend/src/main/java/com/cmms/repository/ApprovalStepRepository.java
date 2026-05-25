package com.cmms.repository;

import com.cmms.model.ApprovalStep;
import com.cmms.model.ApprovalStepId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, ApprovalStepId> {
    List<ApprovalStep> findByCompanyIdAndApprovalIdOrderByStepNoAsc(String companyId, String approvalId);
    List<ApprovalStep> findByCompanyIdAndApproverId(String companyId, String approverId);
    void deleteByCompanyIdAndApprovalId(String companyId, String approvalId);
}
