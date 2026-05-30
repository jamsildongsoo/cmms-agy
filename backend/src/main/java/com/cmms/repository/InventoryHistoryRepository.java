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

    // 입고 취소(역분개) 가능 여부 — 해당 전표 이후 동일 (창고, 품목)에 transaction이 있으면 false
    boolean existsByCompanyIdAndWarehouseIdAndInventoryIdAndHistoryNoGreaterThan(
            String companyId, String warehouseId, String inventoryId, Long historyNo);

    // 전표 묶음 조회
    List<InventoryHistory> findByCompanyIdAndDocNo(String companyId, String docNo);

    // PR별 입고 이력
    List<InventoryHistory> findByCompanyIdAndRefModuleAndRefNo(String companyId, String refModule, String refNo);
}
