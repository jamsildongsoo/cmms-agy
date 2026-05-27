package com.cmms.service;

import com.cmms.dto.AuthDto.*;
import com.cmms.model.*;
import com.cmms.repository.*;
import com.cmms.security.JwtTokenProvider;
import com.cmms.util.CompanyCodeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private LoginHistoryRepository loginHistoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress) {
        String companyId = CompanyCodeUtil.normalize(request.getCompanyId());

        User user = userRepository.findByCompanyIdAndIdAndDeleteYnAndUseYn(
                companyId, request.getId(), "N", "Y")
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 사용 중지된 사용자입니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            recordLoginHistory(companyId, request.getId(), ipAddress, "FAIL");
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 로그인 이력 기록 및 사용자 최종 로그인 정보 갱신
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(ipAddress);
        userRepository.save(user);

        recordLoginHistory(companyId, request.getId(), ipAddress, "SUCCESS");

        // JWT 토큰 생성
        String token = jwtTokenProvider.generateToken(user.getCompanyId(), user.getId());

        LoginResponse response = new LoginResponse();
        response.setAccessToken(token);
        response.setCompanyId(user.getCompanyId());
        response.setId(user.getId());
        response.setName(user.getName());
        response.setRoleId(user.getRoleId());
        response.setDepartmentId(user.getDepartmentId());
        response.setPosition(user.getPosition());
        response.setTitle(user.getTitle());

        return response;
    }

    @Transactional
    public void signUp(SignUpRequest request) {
        String companyId = CompanyCodeUtil.normalize(request.getCompanyId());

        // 가입은 기존 회사 참여 전용 — 회사가 없으면 에러(회사 생성은 sysadmin 전용)
        if (!companyRepository.existsById(companyId)) {
            throw new IllegalArgumentException("존재하지 않는 회사 코드입니다.");
        }

        Optional<User> existingUser = userRepository.findByCompanyIdAndId(companyId, request.getId());
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        User user = new User();
        user.setCompanyId(companyId);
        user.setId(request.getId());
        user.setName(request.getName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        
        // 부서 미선택 시 null (이후 관리자가 배정) — 복합 FK는 null이면 미검증
        String deptId = request.getDepartmentId();
        user.setDepartmentId((deptId == null || deptId.trim().isEmpty()) ? null : deptId);

        // 권한이 없으면 기본 권한("USER") 세팅
        String roleId = request.getRoleId();
        if (roleId == null || roleId.trim().isEmpty()) {
            roleId = "USER";
        }
        user.setRoleId(roleId.toUpperCase());

        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPosition(request.getPosition());
        user.setTitle(request.getTitle());
        user.setUseYn("N"); // 기본 회원가입 시 미승인(N) 상태로 저장 -> 관리자 승인 필요
        user.setCreatedBy(request.getId());
        user.setUpdatedBy(request.getId());

        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public String refresh(String currentToken) {
        if (jwtTokenProvider.validateToken(currentToken)) {
            String username = jwtTokenProvider.getUsernameFromJWT(currentToken);
            String[] parts = username.split(":", 2);
            return jwtTokenProvider.generateToken(parts[0], parts[1]);
        }
        throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(String companyId, String id) {
        User user = userRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        UserProfileResponse response = new UserProfileResponse();
        response.setCompanyId(user.getCompanyId());
        response.setId(user.getId());
        response.setName(user.getName());
        response.setDepartmentId(user.getDepartmentId());
        response.setRoleId(user.getRoleId());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setPosition(user.getPosition());
        response.setTitle(user.getTitle());

        return response;
    }

    @Transactional
    public void updateUserProfile(String companyId, String id, UserUpdateRequest request) {
        User user = userRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPosition(request.getPosition());
        user.setTitle(request.getTitle());
        user.setUpdatedBy(id);

        userRepository.save(user);
    }

    @Transactional
    public void changePassword(String companyId, String id, PasswordChangeRequest request) {
        User user = userRepository.findByCompanyIdAndId(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedBy(id);
        userRepository.save(user);
    }

    private void recordLoginHistory(String companyId, String userId, String ipAddress, String result) {
        LoginHistory history = new LoginHistory();
        history.setCompanyId(companyId);
        history.setUserId(userId);
        history.setLoginAt(LocalDateTime.now());
        history.setLoginIp(ipAddress);
        history.setLoginResult(result);
        loginHistoryRepository.save(history);
    }
}
