package com.cmms.constant;

/**
 * 문서 상태 (PM/WO/WP/Approval의 status 컬럼, char(1)).
 * Approval 문서는 S(직접확정) 미사용 — 부분집합.
 */
public enum DocStatus {
    TEMP("T"),            // 임시저장
    IN_PROGRESS("P"),     // 결재중(상신됨)
    CONFIRMED("C"),       // 완결확정
    SELF_CONFIRMED("S"),  // 직접확정(권한자, 결재 우회)
    REJECTED("R"),        // 반려
    CANCELED("X");        // 취소

    private final String code;

    DocStatus(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
