package com.cmms.repository;

import com.cmms.model.Warehouse;
import com.cmms.model.WarehouseId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, WarehouseId> {
    List<Warehouse> findByCompanyIdAndDeleteYn(String companyId, String deleteYn);
}
