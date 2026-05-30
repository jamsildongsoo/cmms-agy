package com.cmms.dto;

import lombok.Getter;
import lombok.Setter;

public class AuthDto {

    @Getter
    @Setter
    public static class LoginRequest {
        private String companyId;
        private String id;
        private String password;
    }

    @Getter
    @Setter
    public static class SignUpRequest {
        private String companyId;
        private String id;
        private String name;
        private String password;
        private String departmentId;
        private String roleId;
        private String email;
        private String phone;
        private String position;
        private String title;
    }

    @Getter
    @Setter
    public static class LoginResponse {
        private String accessToken;
        private String tokenType = "Bearer";
        private String companyId;
        private String id;
        private String name;
        private String roleId;
        private String departmentId;
        private String position;
        private String title;
        private boolean mustChangePassword; // 강제 변경 필요(만료 또는 관리자 초기화)
        private boolean passwordExpired;    // 만료 경과 여부(안내용)
    }

    @Getter
    @Setter
    public static class UserProfileResponse {
        private String companyId;
        private String id;
        private String name;
        private String departmentId;
        private String roleId;
        private String email;
        private String phone;
        private String position;
        private String title;
    }

    @Getter
    @Setter
    public static class UserUpdateRequest {
        private String name;
        private String email;
        private String phone;
        private String position;
        private String title;
    }

    @Getter
    @Setter
    public static class PasswordChangeRequest {
        private String currentPassword;
        private String newPassword;
    }
}
