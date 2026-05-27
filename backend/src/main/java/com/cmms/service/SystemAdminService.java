package com.cmms.service;

import com.cmms.dto.SystemDto.SystemUserResponse;
import com.cmms.model.LoginHistory;
import com.cmms.model.User;
import com.cmms.repository.LoginHistoryRepository;
import com.cmms.repository.UserRepository;
import com.cmms.util.CodeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SYSTEM(플랫폼 슈퍼관리자) 전용 — 전 테넌트 사용자/로그인이력 관리.
 * MDM(자사 스코프)과 달리 companyId를 파라미터로 받아 교차 테넌트로 동작한다.
 */
@Service
public class SystemAdminService {

    private static final String SYSTEM_TENANT = "SYSTEM";

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LoginHistoryRepository loginHistoryRepository;

    /** 사용자 조회 — companyId 있으면 해당 회사, 없으면 전 테넌트. */
    @Transactional(readOnly = true)
    public List<SystemUserResponse> listUsers(String companyId) {
        String c = CodeUtil.normalizeOptional(companyId);
        List<User> users = (c != null)
                ? userRepository.findByCompanyIdAndDeleteYn(c, "N")
                : userRepository.findByDeleteYn("N");
        return users.stream().map(SystemUserResponse::from).toList();
    }

    /** 사용여부(useYn) 변경 — 교차 테넌트. 플랫폼(SYSTEM) 테넌트 계정은 잠금 방지를 위해 변경 불가. */
    @Transactional
    public void setUseYn(String companyId, String userId, String useYn, String operator) {
        String c = CodeUtil.normalize(companyId);
        if (SYSTEM_TENANT.equals(c)) {
            throw new IllegalArgumentException("플랫폼(SYSTEM) 계정의 사용여부는 변경할 수 없습니다.");
        }
        if (!"Y".equals(useYn) && !"N".equals(useYn)) {
            throw new IllegalArgumentException("useYn은 Y 또는 N 이어야 합니다.");
        }
        User user = userRepository.findByCompanyIdAndId(c, userId)
                .filter(u -> "N".equals(u.getDeleteYn()))
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setUseYn(useYn);
        user.setUpdatedBy(operator);
        userRepository.save(user);
    }

    /** 로그인 이력 조회 — companyId/userId 필터(최신순, 상한 200). */
    @Transactional(readOnly = true)
    public List<LoginHistory> listLoginHistory(String companyId, String userId) {
        String c = CodeUtil.normalizeOptional(companyId);
        if (c != null && userId != null && !userId.trim().isEmpty()) {
            return loginHistoryRepository.findByCompanyIdAndUserIdOrderByLoginAtDesc(c, userId);
        }
        if (c != null) {
            return loginHistoryRepository.findByCompanyIdOrderByLoginAtDesc(c);
        }
        return loginHistoryRepository.findTop200ByOrderByLoginAtDesc();
    }
}
