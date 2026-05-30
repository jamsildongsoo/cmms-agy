-- ==========================================
-- V3. 비밀번호 정책 관리 (만료 / 강제변경 / 계정 잠금)
-- ==========================================
-- users 테이블에 비밀번호 수명·잠금 관리 컬럼 추가.
-- 기존 사용자: password_changed_at 을 마이그레이션 시점(CURRENT_TIMESTAMP)으로 백필
--   → 만료 시계가 V3 배포 시점부터 시작(과거 생성일 기준 즉시 만료 방지).
ALTER TABLE users
    ADD COLUMN password_changed_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN must_change_password CHAR(1)   NOT NULL DEFAULT 'N',
    ADD COLUMN failed_login_count   INTEGER   NOT NULL DEFAULT 0,
    ADD COLUMN account_locked_until TIMESTAMP;
