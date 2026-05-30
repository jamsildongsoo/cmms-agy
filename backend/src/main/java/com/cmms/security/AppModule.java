package com.cmms.security;

/**
 * 권한 매트릭스(role_detail.module_detail)의 모듈 단일 정의.
 * 시드(CompanyService)와 @PreAuthorize("@perm.check('...')")가 이 이름을 공유한다.
 *
 * 모든 코드는 ≤3자 짧은 코드로 통일 — 권한 모듈명·채번 접두사·DB 컬럼값 단일 소스.
 * 채번에도 그대로 사용(`SequenceService.generateNextNo(companyId, AppModule.WO.name(), ...)`).
 * label()은 한글 라벨 단일 소스 — FE는 /api/meta/modules로 받아 사용.
 * Company는 권한 매트릭스가 아니라 SYSTEM role로만 접근 통제하므로 여기에 포함하지 않는다.
 */
public enum AppModule {
    MDM("기준정보 설정"),   // 회사 제외: 플랜트/부서/권한/사용자/창고/공통코드
    EQP("설비 마스터"),
    INV("재고 마스터"),
    STK("재고처리"),        // 입출고/이동/조정/마감 (구매 입고 포함)
    PM("예방점검"),
    WO("작업지시서"),
    WP("작업허가서"),
    APR("전자결재"),
    BRD("게시판"),
    PUR("구매");            // 구매요청·발주·배송·벤더

    private final String label;

    AppModule(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
