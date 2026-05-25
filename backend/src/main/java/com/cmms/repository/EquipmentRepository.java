package com.cmms.repository;

import com.cmms.model.Equipment;
import com.cmms.model.EquipmentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, EquipmentId> {
    List<Equipment> findByCompanyIdAndDeleteYn(String companyId, String deleteYn);
    List<Equipment> findByCompanyIdAndPlantIdAndDeleteYn(String companyId, String plantId, String deleteYn);
}
