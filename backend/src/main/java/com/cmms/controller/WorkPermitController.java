package com.cmms.controller;

import com.cmms.model.WorkPermit;
import com.cmms.security.UserPrincipal;
import com.cmms.service.WorkPermitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/work-permit")
public class WorkPermitController {

    @Autowired
    private WorkPermitService workPermitService;

    @GetMapping
    @PreAuthorize("@perm.check('WP','R')")
    public ResponseEntity<List<WorkPermit>> getWorkPermits(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workPermitService.getWorkPermitsByCompany(principal.getCompanyId()));
    }

    @GetMapping("/details")
    @PreAuthorize("@perm.check('WP','R')")
    public ResponseEntity<WorkPermit> getWorkPermitDetails(
            @RequestParam String plantId, @RequestParam String id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workPermitService.getWorkPermitDetails(principal.getCompanyId(), plantId, id));
    }

    @PostMapping
    @PreAuthorize("@perm.checkSave('WP', #permit?.status)")
    public ResponseEntity<WorkPermit> saveWorkPermit(
            @RequestBody WorkPermit permit, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workPermitService.saveWorkPermit(principal.getCompanyId(), permit, principal.getUsername()));
    }

    @DeleteMapping
    @PreAuthorize("@perm.check('WP','D')")
    public ResponseEntity<Void> deleteWorkPermit(
            @RequestParam String plantId, @RequestParam String id, @AuthenticationPrincipal UserPrincipal principal) {
        workPermitService.deleteWorkPermit(principal.getCompanyId(), plantId, id, principal.getUsername());
        return ResponseEntity.ok().build();
    }
}
