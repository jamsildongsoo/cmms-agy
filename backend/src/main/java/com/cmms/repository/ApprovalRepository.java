package com.cmms.repository;

import com.cmms.model.Approval;
import com.cmms.model.ApprovalId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalRepository extends JpaRepository<Approval, ApprovalId> {
    List<Approval> findByCompanyIdAndDeleteYn(String companyId, String deleteYn);
}
