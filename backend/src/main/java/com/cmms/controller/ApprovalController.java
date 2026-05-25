package com.cmms.controller;

import com.cmms.dto.ApprovalDto.*;
import com.cmms.model.Approval;
import com.cmms.security.UserPrincipal;
import com.cmms.service.ApprovalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/approval")
public class ApprovalController {

    @Autowired
    private ApprovalService approvalService;

    @PostMapping("/submit")
    public ResponseEntity<Approval> submitApproval(
            @RequestBody ApprovalSubmitRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(approvalService.submitApproval(principal.getCompanyId(), request, principal.getUsername()));
    }

    @GetMapping("/sent")
    public ResponseEntity<List<Approval>> getSentApprovals(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(approvalService.getSentApprovals(principal.getCompanyId(), principal.getUsername()));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Approval>> getPendingApprovals(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(approvalService.getPendingApprovals(principal.getCompanyId(), principal.getUsername()));
    }

    @GetMapping("/referenced")
    public ResponseEntity<List<Approval>> getReferencedApprovals(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(approvalService.getReferencedApprovals(principal.getCompanyId(), principal.getUsername()));
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<ApprovalDetailResponse> getApprovalDetails(
            @PathVariable String id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(approvalService.getApprovalDetails(principal.getCompanyId(), id));
    }

    @PostMapping("/{id}/action")
    public ResponseEntity<Void> processApprovalAction(
            @PathVariable String id,
            @RequestBody ApprovalActionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        approvalService.processApprovalAction(principal.getCompanyId(), id, request, principal.getUsername());
        return ResponseEntity.ok().build();
    }
}
