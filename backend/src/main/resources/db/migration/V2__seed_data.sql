-- ==========================================
-- 1. SYSTEM 테넌트 및 초기 관리자 계정 생성
-- ==========================================

-- 시스템 관리용 가상 회사 생성
INSERT INTO company (id, name, business_number, email, created_by, updated_by)
VALUES ('SYSTEM', '시스템 관리본부', '000-00-00000', 'system@cmms.com', 'SYSTEM', 'SYSTEM');

-- 시스템 관리용 권한 그룹 생성 (롤 ID는 대문자 코드 규칙에 따라 'SYSTEM')
INSERT INTO role (company_id, id, role_name, multi_plant, created_by, updated_by)
VALUES ('SYSTEM', 'SYSTEM', '시스템 총괄 관리자', 'Y', 'SYSTEM', 'SYSTEM');

-- 시스템 관리자 계정 생성 (비밀번호: admin123)
-- BCrypt 해시: $2b$10$Gntmi.xoNxCZrVzkt6DIxe9o/lz8elMM64j0f/OVr1tZzIMVI0ft2
INSERT INTO users (company_id, id, name, password_hash, role_id, use_yn, created_by, updated_by)
VALUES ('SYSTEM', 'sysadmin', '시스템관리자', '$2b$10$Gntmi.xoNxCZrVzkt6DIxe9o/lz8elMM64j0f/OVr1tZzIMVI0ft2', 'SYSTEM', 'Y', 'SYSTEM', 'SYSTEM');


-- ==========================================
-- 2. 시스템 예약 공통코드 시드 데이터
-- ==========================================

-- 2.1. 설비 타입 코드 그룹 (EQ_TYPE)
INSERT INTO code_group (company_id, id, name, system_use_yn, created_by, updated_by)
VALUES ('SYSTEM', 'EQ_TYPE', '설비 구분 타입', 'Y', 'SYSTEM', 'SYSTEM');

INSERT INTO code_item (company_id, group_id, id, name, legal_inspect_yn, sort_order) VALUES
('SYSTEM', 'EQ_TYPE', 'PUMP', '펌프', 'N', 10),
('SYSTEM', 'EQ_TYPE', 'MOTOR', '모터', 'N', 20),
('SYSTEM', 'EQ_TYPE', 'BOILER', '보일러', 'Y', 30), -- 보일러는 법정검사 대상 기본값 설정 가능
('SYSTEM', 'EQ_TYPE', 'VALVE', '밸브', 'N', 40),
('SYSTEM', 'EQ_TYPE', 'COMPRESSOR', '압축기', 'N', 50),
('SYSTEM', 'EQ_TYPE', 'PANEL', '전기판넬', 'Y', 60),
('SYSTEM', 'EQ_TYPE', 'ETC', '기타 설비', 'N', 99);

-- 2.2. 자재/부품 타입 코드 그룹 (ITEM_TYPE)
INSERT INTO code_group (company_id, id, name, system_use_yn, created_by, updated_by)
VALUES ('SYSTEM', 'ITEM_TYPE', '자재/부품 타입', 'Y', 'SYSTEM', 'SYSTEM');

INSERT INTO code_item (company_id, group_id, id, name, legal_inspect_yn, sort_order) VALUES
('SYSTEM', 'ITEM_TYPE', 'SPARE_PART', '예비부품', 'N', 10),
('SYSTEM', 'ITEM_TYPE', 'CONSUMABLE', '소모품', 'N', 20),
('SYSTEM', 'ITEM_TYPE', 'TOOL', '공구류', 'N', 30),
('SYSTEM', 'ITEM_TYPE', 'ETC', '기타 자재', 'N', 99);

-- 2.3. 작업지시 타입 코드 그룹 (WO_TYPE)
INSERT INTO code_group (company_id, id, name, system_use_yn, created_by, updated_by)
VALUES ('SYSTEM', 'WO_TYPE', '작업지시 유형', 'Y', 'SYSTEM', 'SYSTEM');

INSERT INTO code_item (company_id, group_id, id, name, legal_inspect_yn, sort_order) VALUES
('SYSTEM', 'WO_TYPE', 'BM', '고장정비 (BM)', 'N', 10),
('SYSTEM', 'WO_TYPE', 'PM', '예방보전 (PM)', 'N', 20),
('SYSTEM', 'WO_TYPE', 'CM', '개조/개선 (CM)', 'N', 30),
('SYSTEM', 'WO_TYPE', 'ETC', '기타 작업', 'N', 99);

-- 2.4. 작업허가 유형 코드 그룹 (WP_TYPE)
INSERT INTO code_group (company_id, id, name, system_use_yn, created_by, updated_by)
VALUES ('SYSTEM', 'WP_TYPE', '안전 작업허가서 유형', 'Y', 'SYSTEM', 'SYSTEM');

INSERT INTO code_item (company_id, group_id, id, name, legal_inspect_yn, sort_order) VALUES
('SYSTEM', 'WP_TYPE', 'GENERAL', '일반위험작업', 'N', 10),
('SYSTEM', 'WP_TYPE', 'FIRE', '화기작업', 'N', 20),
('SYSTEM', 'WP_TYPE', 'CONFINED', '밀폐공간출입작업', 'N', 30),
('SYSTEM', 'WP_TYPE', 'ELECTRIC', '정전작업', 'N', 40),
('SYSTEM', 'WP_TYPE', 'HIGH_PLACE', '고소작업', 'N', 50),
('SYSTEM', 'WP_TYPE', 'EXCAVATION', '굴착작업', 'N', 60),
('SYSTEM', 'WP_TYPE', 'HEAVY_LOAD', '중량물취급작업', 'N', 70);

-- 2.5. 예방점검/보전 타입 코드 그룹 (PM_TYPE)
INSERT INTO code_group (company_id, id, name, system_use_yn, created_by, updated_by)
VALUES ('SYSTEM', 'PM_TYPE', '예방점검 유형', 'Y', 'SYSTEM', 'SYSTEM');

INSERT INTO code_item (company_id, group_id, id, name, legal_inspect_yn, sort_order) VALUES
('SYSTEM', 'PM_TYPE', 'INSPECT', '예방점검', 'N', 10),
('SYSTEM', 'PM_TYPE', 'PATROL', '순회점검', 'N', 20),
('SYSTEM', 'PM_TYPE', 'REPLACE', '소모품교체', 'N', 30),
('SYSTEM', 'PM_TYPE', 'LEGAL', '정기법정검사', 'Y', 40);
