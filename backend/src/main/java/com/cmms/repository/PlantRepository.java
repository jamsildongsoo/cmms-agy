package com.cmms.repository;

import com.cmms.model.Plant;
import com.cmms.model.PlantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlantRepository extends JpaRepository<Plant, PlantId> {
    List<Plant> findByCompanyIdAndDeleteYn(String companyId, String deleteYn);
}
