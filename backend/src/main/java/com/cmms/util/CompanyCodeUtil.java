package com.cmms.util;

/**
 * 회사 코드 정규화: 공백 제거 + 대문자 변환, 영문 대문자/숫자만 허용.
 * 가입/로그인/회사생성에서 동일하게 적용해 대소문자·잡문자로 인한 중복 회사·로그인 불일치를 막는다.
 */
public final class CompanyCodeUtil {

    private CompanyCodeUtil() {}

    public static String normalize(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException("회사 코드는 필수입니다.");
        }
        String code = raw.trim().toUpperCase();
        if (!code.matches("[A-Z0-9]+")) {
            throw new IllegalArgumentException("회사 코드는 영문/숫자만 사용할 수 있습니다.");
        }
        return code;
    }
}
