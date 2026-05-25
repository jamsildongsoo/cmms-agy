package com.cmms.repository;

import com.cmms.model.PmRecord;
import com.cmms.model.PmRecordId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PmRecordRepository extends JpaRepository<PmRecord, PmRecordId> {
    List<PmRecord> findByCompanyIdAndDeleteYn(String companyId, String deleteYn);
    List<PmRecord> findByCompanyIdAndPlantIdAndDeleteYn(String companyId, String plantId, String deleteYn);
}
