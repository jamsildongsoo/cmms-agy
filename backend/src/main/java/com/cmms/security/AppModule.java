package com.cmms.security;

/**
 * 권한 매트릭스(role_detail.module_detail)의 모듈 단일 정의.
 * 시드(AuthService/MdmService)와 @PreAuthorize("@perm.check('...')")가 이 이름을 공유한다.
 * Company는 권한 매트릭스가 아니라 SYSTEM role로만 접근 통제하므로 여기에 포함하지 않는다.
 */
public enum AppModule {
    MDM,        // 기준정보(회사 제외): 플랜트/부서/권한/사용자/창고/공통코드
    EQUIPMENT,  // 설비 마스터
    INVENTORY,  // 재고 마스터
    STOCK,      // 재고처리(입출고/이동/월마감)
    PM,         // 예방점검
    WO,         // 작업지시
    WP,         // 작업허가
    APPROVAL,   // 결재
    BOARD       // 게시판
}
