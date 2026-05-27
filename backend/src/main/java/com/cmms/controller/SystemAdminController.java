package com.cmms.controller;

import com.cmms.dto.SystemDto.SystemUserResponse;
import com.cmms.dto.SystemDto.UseYnUpdateRequest;
import com.cmms.model.LoginHistory;
import com.cmms.security.UserPrincipal;
import com.cmms.service.SystemAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SYSTEM(플랫폼 슈퍼관리자) 전용 콘솔 — 전 테넌트 사용자/로그인이력 관리.
 */
@RestController
@RequestMapping("/api/system")
@PreAuthorize("hasRole('SYSTEM')")
public class SystemAdminController {

    @Autowired
    private SystemAdminService systemAdminService;

    @GetMapping("/users")
    public ResponseEntity<List<SystemUserResponse>> listUsers(@RequestParam(required = false) String companyId) {
        return ResponseEntity.ok(systemAdminService.listUsers(companyId));
    }

    @PutMapping("/users/{companyId}/{userId}/use-yn")
    public ResponseEntity<Void> setUseYn(
            @PathVariable String companyId,
            @PathVariable String userId,
            @RequestBody UseYnUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        systemAdminService.setUseYn(companyId, userId, request.getUseYn(), principal.getUsername());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/login-history")
    public ResponseEntity<List<LoginHistory>> listLoginHistory(
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String userId) {
        return ResponseEntity.ok(systemAdminService.listLoginHistory(companyId, userId));
    }
}
