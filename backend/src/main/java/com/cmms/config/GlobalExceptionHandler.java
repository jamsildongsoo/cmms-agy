package com.cmms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 컨트롤러 진입 이후 발생하는 예외를 일관된 JSON({status, message})으로 변환한다.
 * (미인증/토큰만료 등 시큐리티 필터 단계 예외는 SecurityConfig의 EntryPoint/AccessDeniedHandler가 처리)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 서비스 전반의 비즈니스 검증 실패 (없음/중복/상태위반 등)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException e) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // @PreAuthorize 거부 (RBAC) — 인증된 사용자의 권한 부족
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(AccessDeniedException e) {
        return body(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
    }

    // 동시 첫 입고 PK 충돌 등 무결성 위반 → 재시도 안내 (트랜잭션은 롤백되어 데이터는 안전)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(DataIntegrityViolationException e) {
        return body(HttpStatus.CONFLICT, "다른 사용자가 동시에 처리 중입니다. 잠시 후 다시 시도해주세요.");
    }

    // 예상하지 못한 나머지 — 상세는 로그로 남기고 사용자에겐 일반 메시지
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleEtc(Exception e) {
        log.error("Unhandled exception", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "처리 중 오류가 발생했습니다.");
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "status", status.value(),
                "message", message != null ? message : status.getReasonPhrase()
        ));
    }
}
