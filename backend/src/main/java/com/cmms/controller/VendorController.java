package com.cmms.controller;

import com.cmms.model.Vendor;
import com.cmms.security.UserPrincipal;
import com.cmms.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendors")
public class VendorController {

    @Autowired
    private VendorService vendorService;

    @GetMapping
    @PreAuthorize("@perm.check('PUR','R')")
    public ResponseEntity<List<Vendor>> getVendors(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(vendorService.getVendors(principal.getCompanyId()));
    }

    @PostMapping
    @PreAuthorize("@perm.check('PUR','C')")
    public ResponseEntity<Vendor> saveVendor(
            @RequestBody Vendor vendor, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(vendorService.saveVendor(principal.getCompanyId(), vendor, principal.getUsername()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.check('PUR','U')")
    public ResponseEntity<Vendor> updateVendor(
            @PathVariable String id, @RequestBody Vendor vendor,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(vendorService.updateVendor(principal.getCompanyId(), id, vendor, principal.getUsername()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.check('PUR','D')")
    public ResponseEntity<Void> deleteVendor(
            @PathVariable String id, @AuthenticationPrincipal UserPrincipal principal) {
        vendorService.deleteVendor(principal.getCompanyId(), id, principal.getUsername());
        return ResponseEntity.ok().build();
    }
}
