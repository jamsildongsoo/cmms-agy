package com.cmms.repository;

import com.cmms.model.Inventory;
import com.cmms.model.InventoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, InventoryId> {
    List<Inventory> findByCompanyIdAndDeleteYn(String companyId, String deleteYn);
}
