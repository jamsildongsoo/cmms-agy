package com.cmms.dto;

import com.cmms.model.WorkOrder;
import com.cmms.model.WorkOrderItem;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class WorkOrderDto {

    @Getter
    @Setter
    public static class WorkOrderSaveRequest {
        private WorkOrder workOrder;
        private List<WorkOrderItem> workItems;
    }
}
