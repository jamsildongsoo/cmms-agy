package com.cmms.constant;

/**
 * 문서 상태 + 구매 절차 상태 (단일 enum).
 *
 * 문서 상태 (status 컬럼, char(1); PM/WO/WP/Approval/PurchaseRequest):
 *   T/P/C/S/R/X. Approval은 S 미사용, PurchaseRequest는 결재 비연계로 T/S만 사용(X/P/C/R 미사용).
 *
 * 구매 절차 상태 (proc_status 컬럼, char(1); PurchaseRequest 전용):
 *   O/D/I/E. NULL=미시작. 기존 T/P/C/S/R/X와 비충돌.
 */
public enum DocStatus {
    // 문서 상태
    TEMP("T"),            // 임시저장
    IN_PROGRESS("P"),     // 결재중(상신됨)
    CONFIRMED("C"),       // 완결확정
    SELF_CONFIRMED("S"),  // 직접확정(권한자, 결재 우회)
    REJECTED("R"),        // 반려
    CANCELED("X"),        // 취소
    // 구매 절차 상태 (proc_status)
    ORDERED("O"),         // 발주
    SHIPPING("D"),        // 배송중 (Delivery)
    RECEIVED("I"),        // 입고 (Incoming)
    CLOSED("E");          // 종료 (End)

    private final String code;

    DocStatus(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
