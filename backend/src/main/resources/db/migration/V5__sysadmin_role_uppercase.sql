-- 시드 sysadmin의 롤 'system' → 'SYSTEM' 정규화 (코드 대문자 규칙 일치).
-- role.id는 PK이고 users.role_id가 FK(ON UPDATE CASCADE 없음)이므로,
-- 새 롤 삽입 → 사용자 재지정 → 기존 롤 삭제 순으로 처리한다.
-- (V2는 이미 적용된 마이그레이션이라 직접 수정 불가 → 신규 V5로 처리)

INSERT INTO role (company_id, id, role_name, multi_plant, created_at, created_by, updated_at, updated_by, delete_yn)
SELECT company_id, 'SYSTEM', role_name, multi_plant, created_at, created_by, updated_at, updated_by, delete_yn
FROM role
WHERE company_id = 'SYSTEM' AND id = 'system';

UPDATE users SET role_id = 'SYSTEM'
WHERE company_id = 'SYSTEM' AND role_id = 'system';

DELETE FROM role
WHERE company_id = 'SYSTEM' AND id = 'system';
