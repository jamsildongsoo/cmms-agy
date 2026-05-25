package com.cmms.repository;

import com.cmms.model.WorkOrder;
import com.cmms.model.WorkOrderId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, WorkOrderId> {
    List<WorkOrder> findByCompanyIdAndDeleteYn(String companyId, String deleteYn);
    List<WorkOrder> findByCompanyIdAndPlantIdAndDeleteYn(String companyId, String plantId, String deleteYn);
}
