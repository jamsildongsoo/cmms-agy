package com.cmms.repository;

import com.cmms.model.WorkOrderItem;
import com.cmms.model.WorkOrderItemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkOrderItemRepository extends JpaRepository<WorkOrderItem, WorkOrderItemId> {
    List<WorkOrderItem> findByCompanyIdAndPlantIdAndWorkOrderIdOrderByItemNoAsc(String companyId, String plantId, String workOrderId);
    void deleteByCompanyIdAndPlantIdAndWorkOrderId(String companyId, String plantId, String workOrderId);
}
