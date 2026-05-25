package com.cmms.repository;

import com.cmms.model.EquipmentCheckCycle;
import com.cmms.model.EquipmentCheckCycleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentCheckCycleRepository extends JpaRepository<EquipmentCheckCycle, EquipmentCheckCycleId> {
    List<EquipmentCheckCycle> findByCompanyIdAndPlantIdAndEquipmentIdAndDeleteYn(String companyId, String plantId, String equipmentId, String deleteYn);
}
