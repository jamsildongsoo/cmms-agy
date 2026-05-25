package com.cmms.repository;

import com.cmms.model.PmRecordItem;
import com.cmms.model.PmRecordItemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PmRecordItemRepository extends JpaRepository<PmRecordItem, PmRecordItemId> {
    List<PmRecordItem> findByCompanyIdAndPlantIdAndPmRecordIdOrderByItemNoAsc(String companyId, String plantId, String pmRecordId);
    void deleteByCompanyIdAndPlantIdAndPmRecordId(String companyId, String plantId, String pmRecordId);
}
