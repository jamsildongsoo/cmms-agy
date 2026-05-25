package com.cmms.repository;

import com.cmms.model.EquipmentCheckItem;
import com.cmms.model.EquipmentCheckItemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentCheckItemRepository extends JpaRepository<EquipmentCheckItem, EquipmentCheckItemId> {
    List<EquipmentCheckItem> findByCompanyIdAndPlantIdAndEquipmentIdOrderByItemNoAsc(String companyId, String plantId, String equipmentId);
    void deleteByCompanyIdAndPlantIdAndEquipmentId(String companyId, String plantId, String equipmentId);
}
