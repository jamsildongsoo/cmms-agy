package com.cmms.repository;

import com.cmms.model.WorkPermit;
import com.cmms.model.WorkPermitId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkPermitRepository extends JpaRepository<WorkPermit, WorkPermitId> {
    List<WorkPermit> findByCompanyIdAndDeleteYn(String companyId, String deleteYn);
    List<WorkPermit> findByCompanyIdAndPlantIdAndDeleteYn(String companyId, String plantId, String deleteYn);
}
