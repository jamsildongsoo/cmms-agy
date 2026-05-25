package com.cmms.repository;

import com.cmms.model.InventoryMonthlyClosing;
import com.cmms.model.InventoryMonthlyClosingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryMonthlyClosingRepository extends JpaRepository<InventoryMonthlyClosing, InventoryMonthlyClosingId> {
    List<InventoryMonthlyClosing> findByCompanyIdAndClosingYmAndDeleteYn(String companyId, String closingYm, String deleteYn);
}
