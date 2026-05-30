-- ==========================================
-- V4. 구매(Procurement) 도입 + 부수 정리
-- ==========================================
-- 1) AppModule 코드 통일(≤3자) — 기존 role_detail 데이터 정렬
-- 2) DocStatus 절차상태 확장은 enum/컬럼 변경 없음(기존 char(1)에 O/D/I/E 값만 추가)
-- 3) 창고에 플랜트 속성(nullable=공통부문), 재고 이력에 전표번호(STK docNo)
-- 4) 구매 도메인 테이블 신설: vendor / purchase_request / purchase_request_item
-- 5) 기존 회사들에 PUR 권한 매트릭스 시드 + ADMIN 역할 멀티플랜트화
-- 6) PR_TYPE 공통코드 시드(SYSTEM 공유 — 신규 회사는 copySystemCommonCodes로 자동 복사됨)

-- ------------------------------------------------------------
-- 1) AppModule 코드 rename: 기존 role_detail 데이터 정렬
--    (Java 측은 AppModule enum이 짧은 코드로 변경됨 — DB도 맞춰야 권한 체크 정상 동작)
-- ------------------------------------------------------------
UPDATE role_detail SET module_detail = 'APR' WHERE module_detail = 'APPROVAL';
UPDATE role_detail SET module_detail = 'STK' WHERE module_detail = 'STOCK';
UPDATE role_detail SET module_detail = 'EQP' WHERE module_detail = 'EQUIPMENT';
UPDATE role_detail SET module_detail = 'INV' WHERE module_detail = 'INVENTORY';
UPDATE role_detail SET module_detail = 'BRD' WHERE module_detail = 'BOARD';

-- ------------------------------------------------------------
-- 2) 창고 플랜트 속성 + 재고 이력 전표번호
-- ------------------------------------------------------------
ALTER TABLE warehouse         ADD COLUMN plant_id VARCHAR(50);   -- nullable = 공통부문
ALTER TABLE inventory_history ADD COLUMN doc_no   VARCHAR(50);   -- 전표번호 STK 단일 체계

-- ------------------------------------------------------------
-- 3) 벤더(거래처) 마스터
-- ------------------------------------------------------------
CREATE TABLE vendor (
    company_id VARCHAR(50) REFERENCES company(id),
    id         VARCHAR(50),
    name       VARCHAR(100) NOT NULL,
    biz_no     VARCHAR(50),
    contact    VARCHAR(100),
    manager    VARCHAR(50),
    remarks    TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    delete_yn  CHAR(1)     NOT NULL DEFAULT 'N',
    PRIMARY KEY (company_id, id)
);

-- ------------------------------------------------------------
-- 4) 구매요청(헤더)
--    status   = DocStatus 재사용. PR은 결재 비연계 → T(저장)/S(직접확정)만 사용. X 미사용.
--    proc_status = DocStatus 확장(O/D/I/E). NULL = 미시작.
-- ------------------------------------------------------------
CREATE TABLE purchase_request (
    company_id      VARCHAR(50) REFERENCES company(id),
    id              VARCHAR(50),                          -- 채번 PUR-{요청자부서}-yyyyMM-####
    plant_id        VARCHAR(50) NOT NULL,
    warehouse_id    VARCHAR(50) NOT NULL,                 -- 입고 저장소(헤더 1개, 확정 후 불변)
    requester_id    VARCHAR(50) NOT NULL,                 -- 요청자(현장)
    request_date    DATE        NOT NULL,
    request_type    VARCHAR(50),                          -- 공통코드 PR_TYPE 아이템 id (FK 없음, SYSTEM 공유)
    vendor_id       VARCHAR(50),
    order_date      DATE,
    eta_date        DATE,
    ship_start_date DATE,
    status          CHAR(1)     NOT NULL DEFAULT 'T',
    proc_status     CHAR(1),
    remarks         TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    delete_yn  CHAR(1)    NOT NULL DEFAULT 'N',
    PRIMARY KEY (company_id, id),
    FOREIGN KEY (company_id, plant_id)     REFERENCES plant(company_id, id),
    FOREIGN KEY (company_id, warehouse_id) REFERENCES warehouse(company_id, id),
    FOREIGN KEY (company_id, requester_id) REFERENCES users(company_id, id),
    FOREIGN KEY (company_id, vendor_id)    REFERENCES vendor(company_id, id)
);

-- ------------------------------------------------------------
-- 5) 구매요청 라인(상세) — 자식 테이블, BaseEntity 컬럼 없음 + CASCADE
-- ------------------------------------------------------------
CREATE TABLE purchase_request_item (
    company_id   VARCHAR(50),
    request_id   VARCHAR(50),
    line_no      INT,
    inventory_id VARCHAR(50)    NOT NULL,
    qty          NUMERIC(15, 4) NOT NULL,
    unit         VARCHAR(20),
    received_qty NUMERIC(15, 4) NOT NULL DEFAULT 0,
    remarks      TEXT,
    PRIMARY KEY (company_id, request_id, line_no),
    FOREIGN KEY (company_id, request_id)   REFERENCES purchase_request(company_id, id) ON DELETE CASCADE,
    FOREIGN KEY (company_id, inventory_id) REFERENCES inventory(company_id, id)
);

-- ------------------------------------------------------------
-- 6) 기존 회사들에 PUR 권한 매트릭스 시드
--    매트릭스(createDefaultRoleDetails 패턴):
--      ADMIN   = CRUDA 전부 Y
--      MANAGER = 비MDM CRUDA 전부 Y
--      USER    = 비MDM CRUD Y, A=N
--    SYSTEM 테넌트(sysadmin) role은 권한 매트릭스로 통제되지 않으므로 시드하지 않음(SYSTEM role 자체가 없음).
-- ------------------------------------------------------------
INSERT INTO role_detail (company_id, role_id, module_detail, perm_c, perm_r, perm_u, perm_d, perm_a)
SELECT r.company_id, r.id, 'PUR',
       'Y', 'Y', 'Y', 'Y',
       CASE WHEN r.id = 'USER' THEN 'N' ELSE 'Y' END
FROM role r
WHERE r.id IN ('ADMIN', 'MANAGER', 'USER')
ON CONFLICT (company_id, role_id, module_detail) DO NOTHING;

-- ------------------------------------------------------------
-- 7) ADMIN 역할 멀티플랜트화(현재 전부 N → ADMIN만 Y)
-- ------------------------------------------------------------
UPDATE role SET multi_plant = 'Y' WHERE id = 'ADMIN';

-- ------------------------------------------------------------
-- 8) PR_TYPE 공통코드 시드 (SYSTEM 공유 — 신규 회사는 copySystemCommonCodes로 자동 복사)
-- ------------------------------------------------------------
INSERT INTO code_group (company_id, id, name, system_use_yn, created_by, updated_by)
VALUES ('SYSTEM', 'PR_TYPE', '구매요청유형', 'Y', 'SYSTEM', 'SYSTEM')
ON CONFLICT (company_id, id) DO NOTHING;

INSERT INTO code_item (company_id, group_id, id, name, sort_order) VALUES
    ('SYSTEM', 'PR_TYPE', 'NORMAL',     '일반',         10),
    ('SYSTEM', 'PR_TYPE', 'ROUTINE',    '경상',         20),
    ('SYSTEM', 'PR_TYPE', 'PLANNED_PM', '계획예방정비', 30),
    ('SYSTEM', 'PR_TYPE', 'URGENT',     '긴급',         40),
    ('SYSTEM', 'PR_TYPE', 'ETC',        '기타',         99)
ON CONFLICT (company_id, group_id, id) DO NOTHING;
