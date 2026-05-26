package com.cmms.security;

import com.cmms.constant.RoleType;
import com.cmms.model.RoleDetailId;
import com.cmms.repository.RoleDetailRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * @PreAuthorize("@perm.check('WO','C')") 형태로 호출되는 권한 검사 빈.
 *
 * 규칙(단순):
 *   1) SYSTEM role  → 무조건 통과
 *   2) 그 외 role    → role_detail[companyId, roleId, module]의 해당 action(C/R/U/D/A) 플래그가 'Y'인지 확인
 * Company API는 이 빈을 쓰지 않고 hasRole('SYSTEM')로 별도 통제한다.
 */
@Component("perm")
public class PermissionChecker {

    private final RoleDetailRepository roleDetailRepository;

    public PermissionChecker(RoleDetailRepository roleDetailRepository) {
        this.roleDetailRepository = roleDetailRepository;
    }

    /**
     * 결재 연계 모듈(PM/WO/WP) 저장 권한.
     * 저장 자체는 생성(C) 권한, 단 status="S"(결재를 거치지 않은 자체 확정)는 추가로 A 권한이 필요하다.
     * A가 없으면 자체 확정 불가 → 결재 상신을 거쳐야 한다.
     */
    public boolean checkSave(String module, String status) {
        if (!check(module, "C")) {
            return false;
        }
        if ("S".equals(status)) {
            return check(module, "A");
        }
        return true;
    }

    public boolean check(String module, String action) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }

        // 1) SYSTEM은 전 모듈 통과
        if (RoleType.SYSTEM.code().equalsIgnoreCase(principal.getRoleId())) {
            return true;
        }

        // 2) 매트릭스 조회 (없으면 거부)
        RoleDetailId id = new RoleDetailId(principal.getCompanyId(), principal.getRoleId(), module);
        return roleDetailRepository.findById(id)
                .map(d -> "Y".equals(permValue(d, action)))
                .orElse(false);
    }

    private String permValue(com.cmms.model.RoleDetail d, String action) {
        return switch (action) {
            case "C" -> d.getPermC();
            case "R" -> d.getPermR();
            case "U" -> d.getPermU();
            case "D" -> d.getPermD();
            case "A" -> d.getPermA();
            default -> "N";
        };
    }
}
