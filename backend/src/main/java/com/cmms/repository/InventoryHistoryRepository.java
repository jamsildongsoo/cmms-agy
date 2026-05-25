package com.cmms.repository;

import com.cmms.model.InventoryHistory;
import com.cmms.model.InventoryHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryHistoryRepository extends JpaRepository<InventoryHistory, InventoryHistoryId> {
    List<InventoryHistory> findByCompanyIdAndDeleteYn(String companyId, String deleteYn);
    List<InventoryHistory> findByCompanyIdAndWarehouseIdAndDeleteYn(String companyId, String warehouseId, String deleteYn);
}
