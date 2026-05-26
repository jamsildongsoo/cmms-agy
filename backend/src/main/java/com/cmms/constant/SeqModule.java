package com.cmms.constant;

/**
 * 채번 모듈 = 문서번호 접두사(SequenceService.generateNextNo의 refModule).
 * 값(code)이 그대로 문서번호 접두사가 되므로 이름=코드.
 * 주의: 권한 모듈(AppModule)과 별개 네임스페이스 — 결재는 여기선 APR, 권한에선 APPROVAL.
 */
public enum SeqModule {
    WO,   // 작업지시
    WP,   // 작업허가
    PM,   // 예방점검
    APR;  // 결재

    public String code() {
        return name();
    }
}
