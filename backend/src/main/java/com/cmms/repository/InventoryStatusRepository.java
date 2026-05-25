package com.cmms.repository;

import com.cmms.model.InventoryStatus;
import com.cmms.model.InventoryStatusId;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryStatusRepository extends JpaRepository<InventoryStatus, InventoryStatusId> {

    List<InventoryStatus> findByCompanyIdAndDeleteYn(String companyId, String deleteYn);

    List<InventoryStatus> findByCompanyIdAndWarehouseIdAndDeleteYn(String companyId, String warehouseId, String deleteYn);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("select s from InventoryStatus s where s.companyId = :companyId and s.warehouseId = :warehouseId and s.inventoryId = :inventoryId and s.deleteYn = 'N'")
    Optional<InventoryStatus> findByIdWithLock(
            @Param("companyId") String companyId, 
            @Param("warehouseId") String warehouseId, 
            @Param("inventoryId") String inventoryId);
}
