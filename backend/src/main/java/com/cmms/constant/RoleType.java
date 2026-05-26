package com.cmms.constant;

/**
 * 표준 권한 등급. SYSTEM은 플랫폼 전역 슈퍼관리자(시드 SYSTEM 테넌트 전용),
 * 일반 회사는 ADMIN/MANAGER/USER만 생성한다.
 * 값(code)이 곧 role_id이므로 이름=코드.
 */
public enum RoleType {
    SYSTEM,
    ADMIN,
    MANAGER,
    USER;

    public String code() {
        return name();
    }
}
