package com.cmms.controller;

import com.cmms.dto.CompanyDto;
import com.cmms.model.Company;
import com.cmms.security.UserPrincipal;
import com.cmms.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 회사 관리 — sysadmin(SYSTEM) 전용. MDM에서 분리.
 * 경로는 기존과 동일(/api/mdm/companies)하여 프론트 영향 없음.
 */
@RestController
@RequestMapping("/api/mdm/companies")
@PreAuthorize("hasRole('SYSTEM')")
public class CompanyController {

    @Autowired
    private CompanyService companyService;

    @GetMapping
    public ResponseEntity<List<Company>> getAllCompanies() {
        return ResponseEntity.ok(companyService.getAllCompanies());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Company> getCompanyById(@PathVariable String id) {
        return ResponseEntity.ok(companyService.getCompanyById(id));
    }

    @PostMapping
    public ResponseEntity<Company> createCompany(@RequestBody CompanyDto.CompanyCreateRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(companyService.createCompany(request, principal.getUsername()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Company> updateCompany(@PathVariable String id, @RequestBody Company company, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(companyService.updateCompany(id, company, principal.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompany(@PathVariable String id, @AuthenticationPrincipal UserPrincipal principal) {
        companyService.deleteCompany(id, principal.getUsername());
        return ResponseEntity.ok().build();
    }
}
