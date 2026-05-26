package com.cmms.controller;

import com.cmms.dto.WorkOrderDto.WorkOrderSaveRequest;
import com.cmms.model.WorkOrder;
import com.cmms.security.UserPrincipal;
import com.cmms.service.WorkOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/work-order")
public class WorkOrderController {

    @Autowired
    private WorkOrderService workOrderService;

    @GetMapping
    @PreAuthorize("@perm.check('WO','R')")
    public ResponseEntity<List<WorkOrder>> getWorkOrders(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workOrderService.getWorkOrdersByCompany(principal.getCompanyId()));
    }

    @GetMapping("/details")
    @PreAuthorize("@perm.check('WO','R')")
    public ResponseEntity<WorkOrderSaveRequest> getWorkOrderDetails(
            @RequestParam String plantId, @RequestParam String id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workOrderService.getWorkOrderDetails(principal.getCompanyId(), plantId, id));
    }

    @PostMapping
    @PreAuthorize("@perm.checkSave('WO', #request.workOrder?.status)")
    public ResponseEntity<WorkOrder> saveWorkOrder(
            @RequestBody WorkOrderSaveRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workOrderService.saveWorkOrder(principal.getCompanyId(), request, principal.getUsername()));
    }

    @DeleteMapping
    @PreAuthorize("@perm.check('WO','D')")
    public ResponseEntity<Void> deleteWorkOrder(
            @RequestParam String plantId, @RequestParam String id, @AuthenticationPrincipal UserPrincipal principal) {
        workOrderService.deleteWorkOrder(principal.getCompanyId(), plantId, id, principal.getUsername());
        return ResponseEntity.ok().build();
    }
}
