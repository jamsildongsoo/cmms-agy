package com.cmms.service;

import com.cmms.dto.WorkOrderDto.WorkOrderSaveRequest;
import com.cmms.model.WorkOrder;
import com.cmms.model.WorkOrderId;
import com.cmms.model.WorkOrderItem;
import com.cmms.repository.WorkOrderRepository;
import com.cmms.repository.WorkOrderItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkOrderService {

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private WorkOrderItemRepository workOrderItemRepository;

    @Autowired
    private SequenceService sequenceService;

    @Transactional(readOnly = true)
    public List<WorkOrder> getWorkOrdersByCompany(String companyId) {
        return workOrderRepository.findByCompanyIdAndDeleteYn(companyId, "N");
    }

    @Transactional(readOnly = true)
    public WorkOrderSaveRequest getWorkOrderDetails(String companyId, String plantId, String id) {
        WorkOrder workOrder = workOrderRepository.findById(new WorkOrderId(companyId, plantId, id))
                .filter(wo -> "N".equals(wo.getDeleteYn()))
                .orElseThrow(() -> new IllegalArgumentException("작업 지시를 찾을 수 없습니다."));

        List<WorkOrderItem> items = workOrderItemRepository.findByCompanyIdAndPlantIdAndWorkOrderIdOrderByItemNoAsc(companyId, plantId, id);

        WorkOrderSaveRequest response = new WorkOrderSaveRequest();
        response.setWorkOrder(workOrder);
        response.setWorkItems(items);
        return response;
    }

    @Transactional
    public WorkOrder saveWorkOrder(String companyId, WorkOrderSaveRequest request, String operator) {
        WorkOrder wo = request.getWorkOrder();
        wo.setCompanyId(companyId);

        boolean isNew = wo.getId() == null || wo.getId().trim().isEmpty();
        if (isNew) {
            String woNo = sequenceService.generateNextNo(companyId, "WO", wo.getDepartmentId());
            wo.setId(woNo);
            wo.setCreatedBy(operator);
        }
        wo.setUpdatedBy(operator);
        wo.setDeleteYn("N");

        WorkOrder savedWo = workOrderRepository.save(wo);

        // 기존 상세 아이템 삭제 후 재생성
        workOrderItemRepository.deleteByCompanyIdAndPlantIdAndWorkOrderId(companyId, wo.getPlantId(), wo.getId());

        List<WorkOrderItem> workItems = request.getWorkItems();
        if (workItems != null) {
            for (WorkOrderItem item : workItems) {
                item.setCompanyId(companyId);
                item.setPlantId(wo.getPlantId());
                item.setWorkOrderId(wo.getId());
                workOrderItemRepository.save(item);
            }
        }

        return savedWo;
    }

    @Transactional
    public void deleteWorkOrder(String companyId, String plantId, String id, String operator) {
        WorkOrder wo = workOrderRepository.findById(new WorkOrderId(companyId, plantId, id))
                .filter(r -> "N".equals(r.getDeleteYn()))
                .orElseThrow(() -> new IllegalArgumentException("작업 지시를 찾을 수 없습니다."));
        wo.setDeleteYn("Y");
        wo.setUpdatedBy(operator);
        workOrderRepository.save(wo);
    }
}
