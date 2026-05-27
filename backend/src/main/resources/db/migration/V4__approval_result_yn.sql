-- 결재 단계 결과(approval_result)를 빈칸(대기)/Y(승인)/N(반려)로 전환.
-- 기존 값: T(작성/대기)·P(진행/현재차례) → 대기(NULL), A(승인) → Y, R(반려) → N
-- (정본: docs/db_specification.md §5.3)
ALTER TABLE approval_step ALTER COLUMN approval_result DROP NOT NULL;

UPDATE approval_step
SET approval_result = CASE approval_result
    WHEN 'A' THEN 'Y'   -- 승인(기안 완료 포함)
    WHEN 'R' THEN 'N'   -- 반려
    ELSE NULL           -- T(대기)/P(진행) → 대기
END;
