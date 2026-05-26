package com.cmms.constant;

/**
 * 결재 단계 유형 (approval_step.approval_type 컬럼, char(1)).
 * 결과(approval_result)는 별도 — 빈칸/Y/N(대기/승인/반려)로 관리하며 enum을 두지 않는다.
 */
public enum ApprovalStepType {
    DRAFT("D"),       // 기안
    APPROVAL("A"),    // 결재(대상)
    AGREEMENT("G"),   // 합의
    REFERENCE("R");   // 참조

    private final String code;

    ApprovalStepType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
