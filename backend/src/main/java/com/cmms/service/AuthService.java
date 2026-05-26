package com.cmms.service;

import com.cmms.dto.AuthDto.*;
import com.cmms.model.*;
import com.cmms.repository.*;
import com.cmms.security.JwtTokenProvider;
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
    private DepartmentRepository departmentRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RoleDetailRepository roleDetailRepository;

    @Autowired
    private LoginHistoryRepository loginHistoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress) {


        User user = userRepository.findByCompanyIdAndIdAndDeleteYnAndUseYn(
                request.getCompanyId(), request.getId(), "N", "Y")
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 사용 중지된 사용자입니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            recordLoginHistory(request.getCompanyId(), request.getId(), ipAddress, "FAIL");
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 로그인 이력 기록 및 사용자 최종 로그인 정보 갱신
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(ipAddress);
        userRepository.save(user);

        recordLoginHistory(request.getCompanyId(), request.getId(), ipAddress, "SUCCESS");

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
        Optional<User> existingUser = userRepository.findByCompanyIdAndId(request.getCompanyId(), request.getId());
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        // 회사가 존재하지 않으면 신규 생성 및 초기 시드 정보 자동 셋업
        if (!companyRepository.existsById(request.getCompanyId())) {
            setupNewCompany(request.getCompanyId());
        }

        User user = new User();
        user.setCompanyId(request.getCompanyId());
        user.setId(request.getId());
        user.setName(request.getName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        
        // 부서가 없으면 기본 부서("DEPT_ROOT") 세팅
        String deptId = request.getDepartmentId();
        if (deptId == null || deptId.trim().isEmpty()) {
            deptId = "DEPT_ROOT";
        }
        user.setDepartmentId(deptId);

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

    private void setupNewCompany(String companyId) {
        // 1. 회사 생성
        Company company = new Company();
        company.setId(companyId);
        company.setName(companyId + " 회사");
        company.setCreatedBy("SYSTEM");
        company.setUpdatedBy("SYSTEM");
        companyRepository.save(company);

        // 2. 기본 ROOT 부서 생성
        Department dept = new Department();
        dept.setCompanyId(companyId);
        dept.setId("DEPT_ROOT");
        dept.setName("기본부서");
        dept.setCreatedBy("SYSTEM");
        dept.setUpdatedBy("SYSTEM");
        departmentRepository.save(dept);

        // 3. 표준 권한 그룹 생성
        // SYSTEM 역할은 플랫폼 전역 슈퍼관리자(시드 SYSTEM 테넌트의 sysadmin) 전용이므로 회사별로 만들지 않는다.
        // (회사별 SYSTEM 역할은 권한 매트릭스 우회 + 교차 테넌트 Company API 접근을 허용하는 격리 위반 경로)
        String[] standardRoles = {"ADMIN", "MANAGER", "USER"};
        String[] standardRoleNames = {"회사관리자", "중간관리자", "일반사용자"};

        for (int i = 0; i < standardRoles.length; i++) {
            Role role = new Role();
            role.setCompanyId(companyId);
            role.setId(standardRoles[i]);
            role.setRoleName(standardRoleNames[i]);
            role.setMultiPlant("N");
            role.setCreatedBy("SYSTEM");
            role.setUpdatedBy("SYSTEM");
            roleRepository.save(role);

            // 4. 모듈 상세 권한 설정 매트릭스 생성
            createDefaultRoleDetails(companyId, standardRoles[i]);
        }
    }

    private void createDefaultRoleDetails(String companyId, String roleId) {
        List<RoleDetail> details = new ArrayList<>();

        for (com.cmms.security.AppModule m : com.cmms.security.AppModule.values()) {
            String module = m.name();
            RoleDetail detail = new RoleDetail();
            detail.setCompanyId(companyId);
            detail.setRoleId(roleId);
            detail.setModuleDetail(module);

            switch (roleId) {
                case "SYSTEM":
                case "ADMIN":
                    // 관리자는 모든 권한 가짐
                    detail.setPermC("Y");
                    detail.setPermR("Y");
                    detail.setPermU("Y");
                    detail.setPermD("Y");
                    detail.setPermA("Y");
                    break;
                case "MANAGER":
                    // 중간관리자: MDM은 R(조회)만 가능하고 수정(CUD) 불가. 
                    // 트랜잭션 등 나머지 모듈은 CRU 전부 가능하며 승인(A) 우회/확정 권한 제공.
                    if (module.equals("MDM")) {
                        detail.setPermC("N");
                        detail.setPermR("Y");
                        detail.setPermU("N");
                        detail.setPermD("N");
                        detail.setPermA("N");
                    } else {
                        detail.setPermC("Y");
                        detail.setPermR("Y");
                        detail.setPermU("Y");
                        detail.setPermD("Y");
                        detail.setPermA("Y"); // 직접 확정(S) 가능하므로 승인(A)도 가능
                    }
                    break;
                case "USER":
                    // 일반사용자: MDM은 R(조회)만 가능.
                    // 트랜잭션 모듈은 CRU 가능하나 결재 승인(A) 권한 없음.
                    if (module.equals("MDM")) {
                        detail.setPermC("N");
                        detail.setPermR("Y");
                        detail.setPermU("N");
                        detail.setPermD("N");
                        detail.setPermA("N");
                    } else {
                        detail.setPermC("Y");
                        detail.setPermR("Y");
                        detail.setPermU("Y");
                        detail.setPermD("Y");
                        detail.setPermA("N"); // 승인(A) 권한 없음. 결재 상신 필요
                    }
                    break;
            }
            details.add(detail);
        }
        roleDetailRepository.saveAll(details);
    }
}
