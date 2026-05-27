package com.cmms.util;

/**
 * 코드성 식별자 정규화: 공백 제거 + 대문자 변환, 영문 대문자/숫자/`_`/`-`만 허용.
 * 적용 대상(코드 ID): companyId, roleId, departmentId, plantId, warehouseId, codeGroupId, codeItemId.
 * 제외: userId(로그인 아이디), 이름/표시문자, 생성 문서번호.
 *
 * 금지 문자 사유: ':'(JWT subject 구분자), '/ ? # % & 공백'(URL), 따옴표류(SpEL) 등은 허용 charset에서 제외됨.
 */
public final class CodeUtil {

    private CodeUtil() {}

    public static String normalize(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException("코드는 필수입니다.");
        }
        String code = raw.trim().toUpperCase();
        if (!code.matches("[A-Z0-9_-]+")) {
            throw new IllegalArgumentException("코드는 영문 대문자/숫자/_/- 만 사용할 수 있습니다.");
        }
        return code;
    }

    /** 선택값(null/빈값 허용) 정규화 — 예: departmentId. 비었으면 null 반환. */
    public static String normalizeOptional(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        return normalize(raw);
    }
}
