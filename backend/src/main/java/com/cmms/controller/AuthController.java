package com.cmms.controller;

import com.cmms.dto.AuthDto.*;
import com.cmms.security.UserPrincipal;
import com.cmms.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        String ipAddress = servletRequest.getRemoteAddr();
        LoginResponse response = authService.login(request, ipAddress);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@RequestBody SignUpRequest request) {
        authService.signUp(request);
        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refresh(@RequestHeader("Authorization") String tokenHeader) {
        if (tokenHeader != null && tokenHeader.startsWith("Bearer ")) {
            String token = tokenHeader.substring(7);
            String newToken = authService.refresh(token);
            return ResponseEntity.ok(newToken);
        }
        return ResponseEntity.badRequest().body("Bearer 토큰이 제공되지 않았습니다.");
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        UserProfileResponse response = authService.getUserProfile(userPrincipal.getCompanyId(), userPrincipal.getUsername());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<String> updateMe(@AuthenticationPrincipal UserPrincipal userPrincipal, @RequestBody UserUpdateRequest request) {
        authService.updateUserProfile(userPrincipal.getCompanyId(), userPrincipal.getUsername(), request);
        return ResponseEntity.ok("사용자 정보가 수정되었습니다.");
    }

    @PutMapping("/me/password")
    public ResponseEntity<String> changePassword(@AuthenticationPrincipal UserPrincipal userPrincipal, @RequestBody PasswordChangeRequest request) {
        authService.changePassword(userPrincipal.getCompanyId(), userPrincipal.getUsername(), request);
        return ResponseEntity.ok("비밀번호가 수정되었습니다.");
    }
}
