package com.cmms.controller;

import com.cmms.dto.PurchaseRequestDto.*;
import com.cmms.model.PurchaseRequest;
import com.cmms.security.UserPrincipal;
import com.cmms.service.ProcurementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/procurement")
public class ProcurementController {

    @Autowired
    private ProcurementService procurementService;

    @GetMapping("/requests")
    @PreAuthorize("@perm.check('PUR','R')")
    public ResponseEntity<List<PurchaseRequest>> getRequests(
            @RequestParam(required = false) String plantId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(procurementService.getRequests(principal.getCompanyId(), principal.getUsername(), plantId));
    }

    @GetMapping("/requests/{id}")
    @PreAuthorize("@perm.check('PUR','R')")
    public ResponseEntity<RequestDetail> getRequest(
            @PathVariable String id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(procurementService.getRequestDetail(principal.getCompanyId(), id));
    }

    @PostMapping("/requests")
    @PreAuthorize("@perm.checkSave('PUR', #request.header?.status)")
    public ResponseEntity<PurchaseRequest> saveRequest(
            @RequestBody SaveRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(procurementService.createOrUpdate(principal.getCompanyId(), request, principal.getUsername()));
    }

    @PostMapping("/requests/{id}/confirm")
    @PreAuthorize("@perm.checkSave('PUR', 'S')")
    public ResponseEntity<PurchaseRequest> confirmRequest(
            @PathVariable String id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(procurementService.confirm(principal.getCompanyId(), id, principal.getUsername()));
    }

    @PostMapping("/orders")
    @PreAuthorize("@perm.check('PUR','U')")
    public ResponseEntity<PurchaseRequest> placeOrder(
            @RequestBody OrderRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(procurementService.placeOrder(principal.getCompanyId(), request, principal.getUsername()));
    }

    @PostMapping("/shipments")
    @PreAuthorize("@perm.check('PUR','U')")
    public ResponseEntity<PurchaseRequest> startShipping(
            @RequestBody ShipRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(procurementService.startShipping(principal.getCompanyId(), request, principal.getUsername()));
    }

    @PostMapping("/receipts")
    @PreAuthorize("@perm.check('STK','C')")
    public ResponseEntity<PurchaseRequest> receive(
            @RequestBody ReceiveRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(procurementService.receive(principal.getCompanyId(), request, principal.getUsername()));
    }

    /** 전표 취소(역분개) — IN/OUT 둘 다 지원. 후속 거래 없을 때만. */
    @PostMapping("/slips/cancel/{docNo}")
    @PreAuthorize("@perm.check('STK','C')")
    public ResponseEntity<Void> cancelSlip(
            @PathVariable String docNo, @AuthenticationPrincipal UserPrincipal principal) {
        procurementService.cancelSlip(principal.getCompanyId(), docNo, principal.getUsername());
        return ResponseEntity.ok().build();
    }

    /** @deprecated 호환용 — /slips/cancel/{docNo} 사용 권장. */
    @PostMapping("/receipts/cancel/{docNo}")
    @PreAuthorize("@perm.check('STK','C')")
    public ResponseEntity<Void> cancelReceipt(
            @PathVariable String docNo, @AuthenticationPrincipal UserPrincipal principal) {
        procurementService.cancelSlip(principal.getCompanyId(), docNo, principal.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/requests/{id}/close")
    @PreAuthorize("@perm.check('PUR','U')")
    public ResponseEntity<PurchaseRequest> close(
            @PathVariable String id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(procurementService.close(principal.getCompanyId(), id, principal.getUsername()));
    }

    @DeleteMapping("/requests/{id}")
    @PreAuthorize("@perm.check('PUR','D')")
    public ResponseEntity<Void> deleteRequest(
            @PathVariable String id, @AuthenticationPrincipal UserPrincipal principal) {
        procurementService.deleteRequest(principal.getCompanyId(), id, principal.getUsername());
        return ResponseEntity.ok().build();
    }
}
