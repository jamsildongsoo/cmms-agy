# 데이터베이스 설계서 (Database Specification)

본 문서는 CMMS의 데이터베이스 구조, 테이블 명세 및 실제 구동 가능한 PostgreSQL DDL 스크립트를 정의합니다.

---

## 1. 전역 데이터베이스 정책

### 1.1. 공통 컬럼 (Auditing 및 Soft Delete)
*   **적용 대상**: 아래 명시된 **상세/아이템 테이블을 제외한 모든 테이블**.
*   **공통 컬럼 구성**:
    *   `created_at`: TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP (생성일시)
    *   `created_by`: VARCHAR(50) NOT NULL (생성자 ID)
    *   `updated_at`: TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP (수정일시)
    *   `updated_by`: VARCHAR(50) NOT NULL (수정자 ID)
    *   `delete_yn`: CHAR(1) NOT NULL DEFAULT 'N' (삭제여부, 'Y' 또는 'N')

### 1.2. 테이블 명명 규칙
*   스네이크 케이스(snake_case)를 적용하며 복수형 또는 명사형을 일관되게 사용합니다.
*   기본키(PK)는 단일 대리키일 경우 `id`(BIGSERIAL)를 권장하며, 복합키가 더 자연스러운 마스터-디테일 테이블(예: 코드아이템, 설비점검항목 등)은 비즈니스 키의 조합으로 구성합니다.

### 1.3. 멀티테넌트(Multi-tenant) 및 SYSTEM 테넌트 정책
*   **테넌트 격리**: 본 DB 스키마는 공유 DB/공유 스키마 방식을 지원합니다. 모든 테이블(상세 테이블 제외)의 기본키 및 외래키 상위에 `company_id`가 포함되어 물리적 수준의 테넌트 파티셔닝을 형성합니다.
*   **시스템 제어 평면 예약 테넌트**:
    *   `company_id = 'SYSTEM'`은 플랫폼 관리(가입 회사 등록, 모니터링 등)를 위해 예약된 시스템 관리자용 테넌트입니다.
    *   `system` 계정은 테넌트 격리 정책을 우회하여 모든 `company_id`에 해당하는 데이터를 쿼리할 수 있도록 어플리케이션(Spring Boot) 레이어에서 제어합니다.

---

## 2. 모듈별 테이블 DDL 및 상세 명세

### 2.1. 조직 및 기준정보 (Organization & MDM)

```sql
-- 1. 회사
CREATE TABLE company (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    business_number VARCHAR(20),
    email VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    delete_yn CHAR(1) NOT NULL DEFAULT 'N'
);

-- 2. 플랜트
CREATE TABLE plant (
    company_id VARCHAR(50) REFERENCES company(id),
    id VARCHAR(50),
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    delete_yn CHAR(1) NOT NULL DEFAULT 'N',
    PRIMARY KEY (company_id, id)
);

-- 3. 부서 (계층 구조)
CREATE TABLE department (
    company_id VARCHAR(50),
    id VARCHAR(50),
    name VARCHAR(100) NOT NULL,
    parent_id VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    delete_yn CHAR(1) NOT NULL DEFAULT 'N',
    PRIMARY KEY (company_id, id),
    FOREIGN KEY (company_id, parent_id) REFERENCES department(company_id, id)
);

-- 4. 권한 그룹
CREATE TABLE role (
    company_id VARCHAR(50) REFERENCES company(id),
    id VARCHAR(50),
    role_name VARCHAR(100) NOT NULL,
    multi_plant CHAR(1) NOT NULL DEFAULT 'N', -- 복수 플랜트 관리 여부 (Y/N)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    delete_yn CHAR(1) NOT NULL DEFAULT 'N',
    PRIMARY KEY (company_id, id)
);

-- 5. 권한 상세 (C/R/U/D/A 권한 매트릭스)
-- 상세 테이블이므로 공통 컬럼(생성일 등) 제외, Hard Delete 대상
CREATE TABLE role_detail (
    company_id VARCHAR(50),
    role_id VARCHAR(50),
    module_detail VARCHAR(100), -- 예: 'EQUIPMENT_MASTER', 'WORK_ORDER'
    perm_c CHAR(1) NOT NULL DEFAULT 'N', -- Create 권한 (Y/N)
    perm_r CHAR(1) NOT NULL DEFAULT 'N', -- Read 권한 (Y/N)
    perm_u CHAR(1) NOT NULL DEFAULT 'N', -- Update 권한 (Y/N)
    perm_d CHAR(1) NOT NULL DEFAULT 'N', -- Delete 권한 (Y/N)
    perm_a CHAR(1) NOT NULL DEFAULT 'N', -- Approval/Bypass 권한 (Y/N)
    PRIMARY KEY (company_id, role_id, module_detail),
    FOREIGN KEY (company_id, role_id) REFERENCES role(company_id, id) ON DELETE CASCADE
);

-- 6. 사용자
CREATE TABLE users (
    company_id VARCHAR(50),
    id VARCHAR(50), -- 로그인 ID
    name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(256) NOT NULL,
    department_id VARCHAR(50),
    role_id VARCHAR(50), -- 권한 그룹 ID (system, admin, manager, user)
    email VARCHAR(100),
    phone VARCHAR(50),
    position VARCHAR(50), -- 직급 (예: 과장, 대리)
    title VARCHAR(50), -- 직책 (예: 팀장, 파트장)
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',
    last_login_ip VARCHAR(50),
    last_login_at TIMESTAMP,
    last_login_plant_id VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    delete_yn CHAR(1) NOT NULL DEFAULT 'N',
    PRIMARY KEY (company_id, id),
    FOREIGN KEY (company_id, department_id) REFERENCES department(company_id, id),
    FOREIGN KEY (company_id, role_id) REFERENCES role(company_id, id)
);

-- 7. 저장소(창고)
CREATE TABLE warehouse (
    company_id VARCHAR(50) REFERENCES company(id),
    id VARCHAR(50),
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    delete_yn CHAR(1) NOT NULL DEFAULT 'N',
    PRIMARY KEY (company_id, id)
);

-- 8. 공통코드 그룹
CREATE TABLE code_group (
    company_id VARCHAR(50) REFERENCES company(id),
    id VARCHAR(50), -- 그룹코드
    name VARCHAR(100) NOT NULL,
    system_use_yn CHAR(1) NOT NULL DEFAULT 'N',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    delete_yn CHAR(1) NOT NULL DEFAULT 'N',
    PRIMARY KEY (company_id, id)
);

-- 9. 공통코드 아이템 (상세)
-- 상세 테이블로 공통 컬럼 제외, Hard Delete
CREATE TABLE code_item (
    company_id VARCHAR(50),
    group_id VARCHAR(50),
    id VARCHAR(50), -- 세부코드
    name VARCHAR(100) NOT NULL,
    legal_inspect_yn CHAR(1) NOT NULL DEFAULT 'N', -- 법정검사 대상 코드 여부
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (company_id, group_id, id),
    FOREIGN KEY (company_id, group_id) REFERENCES code_group(company_id, id) ON DELETE CASCADE
);
```

### 2.2. 마스터 정보 (Master Info)

```sql
-- 10. 설비 마스터
CREATE TABLE equipment (
    company_id VARCHAR(50),
    plant_id VARCHAR(50),
    id VARCHAR(50), -- 설비아이디
    name VARCHAR(100) NOT NULL,
    location VARCHAR(150),
    eq_type_code VARCHAR(50), -- 공통코드 연동
    install_date DATE,
    work_permit_yn CHAR(1) NOT NULL DEFAULT 'N', -- 작업허가대상 여부
    maker_name VARCHAR(100),
    spec TEXT,
    model VARCHAR(100),
    serial_number VARCHAR(100),
    remarks TEXT,
    file_group_id BIGINT, -- 파일첨부 연동
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    delete_yn CHAR(1) NOT NULL DEFAULT 'N',
    PRIMARY KEY (company_id, plant_id, id),
    FOREIGN KEY (company_id, plant_id) REFERENCES plant(company_id, id)
);

-- 11. 설비 마스터 점검 항목 (상세이므로 공통 컬럼 제외)
CREATE TABLE equipment_check_item (
    company_id VARCHAR(50),
    plant_id VARCHAR(50),
    equipment_id VARCHAR(50),
    item_no INT, -- 항목번호 (1, 2, 3...)
    check_name VARCHAR(150) NOT NULL,
    check_method VARCHAR(250),
    min_value NUMERIC(15, 4),
    max_value NUMERIC(15, 4),
    base_value NUMERIC(15, 4),
    unit VARCHAR(20),
    PRIMARY KEY (company_id, plant_id, equipment_id, item_no),
    FOREIGN KEY (company_id, plant_id, equipment_id) REFERENCES equipment(company_id, plant_id, id) ON DELETE CASCADE
);

-- 12. 설비 마스터 점검 주기
CREATE TABLE equipment_check_cycle (
    company_id VARCHAR(50),
    plant_id VARCHAR(50),
    equipment_id VARCHAR(50),
    check_type_code VARCHAR(50), -- 공통코드 (예: 일상점검, 정기점검)
    cycle_val INT NOT NULL, -- 주기 숫자
    cycle_unit VARCHAR(10) NOT NULL, -- 주기 단위 (D, W, M, Y)
    last_check_date DATE,
    next_check_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    delete_yn CHAR(1) NOT NULL DEFAULT 'N',
    PRIMARY KEY (company_id, plant_id, equipment_id, check_type_code),
    FOREIGN KEY (company_id, plant_id, equipment_id) REFERENCES equipment(company_id, plant_id, id)
);

-- 13. 재고 마스터
CREATE TABLE inventory (
    company_id VARCHAR(50) REFERENCES company(id),
    id VARCHAR(50), -- 재고아이디(품목코드)
    name VARCHAR(150) NOT NULL,
    item_type_code VARCHAR(50), -- 공통코드 (자재/부품 분류)
    department_id VARCHAR(50),
    unit VARCHAR(20), -- 단위 (EA, SET, Box 등)
    maker_name VARCHAR(100),
    spec TEXT,
    model VARCHAR(100),
    serial_number VARCHAR(100),
    safety_qty NUMERIC(15, 4) NOT NULL DEFAULT 0, -- 안전재고
    reorder_qty NUMERIC(15, 4) NOT NULL DEFAULT 0, -- 재주문량
    lead_time_days INT NOT NULL DEFAULT 0, -- 리드타임
    remarks TEXT,
    file_group_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    delete_yn CHAR(1) NOT NULL DEFAULT 'N',
    PRIMARY KEY (company_id, id),
    FOREIGN KEY (company_id, department_id) REFERENCES department(company_id, id)
);
```

### 2.3. 결재 모듈 (Approval)

```sql
-- 14. 결재 마스터
CREATE TABLE approval (
    company_id VARCHAR(50) REFERENCES company(id),
    id VARCHAR(50), -- 결재일련번호 (APV-YYYYMM-0001 등)
    title VARCHAR(150) NOT NULL,
    content TEXT,
    drafter_id VARCHAR(50) NOT NULL, -- 기안자 ID
    file_group_id BIGINT,
    status CHAR(1) NOT NULL DEFAULT 'T', -- 상태: T(임시저장), P(진행중), C(확정/승인), R(반려), X(취소)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    delete_yn CHAR(1) NOT NULL DEFAULT 'N',
    PRIMARY KEY (company_id, id),
    FOREIGN KEY (company_id, drafter_id) REFERENCES users(company_id, id)
);

-- 15. 결재단계 (상세이므로 공통 컬럼 제외)
CREATE TABLE approval_step (
    company_id VARCHAR(50),
    approval_id VARCHAR(50),
    step_no INT, -- 0: 상신자, 1부터 결재순번
    approver_id VARCHAR(50) NOT NULL,
    approval_type CHAR(1) NOT NULL, -- D(기안), A(결재), G(합의), R(참조)
    approval_result CHAR(1) NOT NULL, -- D(기안), A(승인), R(반려), W(대기)
    action_at TIMESTAMP,
    comments TEXT,
    PRIMARY KEY (company_id, approval_id, step_no),
    FOREIGN KEY (company_id, approval_id) REFERENCES approval(company_id, id) ON DELETE CASCADE,
    FOREIGN KEY (company_id, approver_id) REFERENCES users(company_id, id)
);
```

### 2.4. 트랜잭션 모듈 (Transaction)

```sql
-- 16. 예방점검 기록
CREATE TABLE pm_record (
    company_id VARCHAR(50),
    plant_id VARCHAR(50),
    id VARCHAR(50), -- 점검번호 (PM-YYYYMM-0001 등)
    equipment_id VARCHAR(50) NOT NULL,
    department_id VARCHAR(50) NOT NULL,
    check_type_code VARCHAR(50) NOT NULL,
    work_date DATE NOT NULL, -- 수행일자
    worker_id VARCHAR(50) NOT NULL, -- 점검자
    judge_code VARCHAR(20) NOT NULL, -- 판정: 양호(G), 불량(B), 기타(O) 등 공통코드
    remarks TEXT,
    cert_number VARCHAR(100), -- 법정검사 인증번호
    cert_expire_date DATE, -- 인증만료일
    cert_agency VARCHAR(100), -- 인증기관
    approval_id VARCHAR(50), -- 결재아이디 연동
    status CHAR(1) NOT NULL DEFAULT 'T', -- 상태: T(임시저장), P(진행), C(확정-결재완결), R(반려), S(직접확정저장)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    delete_yn CHAR(1) NOT NULL DEFAULT 'N',
    PRIMARY KEY (company_id, plant_id, id),
    FOREIGN KEY (company_id, plant_id, equipment_id) REFERENCES equipment(company_id, plant_id, id),
    FOREIGN KEY (company_id, department_id) REFERENCES department(company_id, id),
    FOREIGN KEY (company_id, worker_id) REFERENCES users(company_id, id),
    FOREIGN KEY (company_id, approval_id) REFERENCES approval(company_id, id)
);

-- 17. 예방점검 기록 아이템 (상세)
CREATE TABLE pm_record_item (
    company_id VARCHAR(50),
    plant_id VARCHAR(50),
    pm_record_id VARCHAR(50),
    item_no INT,
    check_name VARCHAR(150) NOT NULL,
    check_method VARCHAR(250),
    min_value NUMERIC(15, 4),
    max_value NUMERIC(15, 4),
    base_value NUMERIC(15, 4),
    unit VARCHAR(20),
    check_value NUMERIC(15, 4), -- 작업자가 입력한 측정값
    PRIMARY KEY (company_id, plant_id, pm_record_id, item_no),
    FOREIGN KEY (company_id, plant_id, pm_record_id) REFERENCES pm_record(company_id, plant_id, id) ON DELETE CASCADE
);

-- 18. 작업지시
CREATE TABLE work_order (
    company_id VARCHAR(50),
    plant_id VARCHAR(50),
    id VARCHAR(50), -- 지시번호
    equipment_id VARCHAR(50) NOT NULL,
    title VARCHAR(150) NOT NULL,
    step_stage CHAR(1) NOT NULL, -- P(계획), A(실적) 단계
    wo_type_code VARCHAR(50) NOT NULL, -- 공통코드 (고장정비, 예방보전 등)
    department_id VARCHAR(50) NOT NULL,
    worker_id VARCHAR(50), -- 작업담당자
    work_date DATE, -- 계획/실적일자
    cost NUMERIC(15, 2) NOT NULL DEFAULT 0, -- 소요비용
    man_hours NUMERIC(8, 2) NOT NULL DEFAULT 0, -- 소요공수
    man_hours_unit VARCHAR(10) NOT NULL DEFAULT 'H',
    remarks TEXT,
    file_group_id BIGINT,
    ref_no VARCHAR(50), -- 참조번호 (스케줄 연계 등)
    ref_module VARCHAR(50), -- 참조모듈
    approval_id VARCHAR(50),
    status CHAR(1) NOT NULL DEFAULT 'T', -- T, P, C, R, S
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    delete_yn CHAR(1) NOT NULL DEFAULT 'N',
    PRIMARY KEY (company_id, plant_id, id),
    FOREIGN KEY (company_id, plant_id, equipment_id) REFERENCES equipment(company_id, plant_id, id),
    FOREIGN KEY (company_id, department_id) REFERENCES department(company_id, id),
    FOREIGN KEY (company_id, worker_id) REFERENCES users(company_id, id),
    FOREIGN KEY (company_id, approval_id) REFERENCES approval(company_id, id)
);

-- 19. 작업지시 아이템 (상세)
CREATE TABLE work_order_item (
    company_id VARCHAR(50),
    plant_id VARCHAR(50),
    work_order_id VARCHAR(50),
    item_no INT,
    work_name VARCHAR(150) NOT NULL,
    work_method VARCHAR(250),
    work_result TEXT,
    PRIMARY KEY (company_id, plant_id, work_order_id, item_no),
    FOREIGN KEY (company_id, plant_id, work_order_id) REFERENCES work_order(company_id, plant_id, id) ON DELETE CASCADE
);

-- 20. 작업허가
CREATE TABLE work_permit (
    company_id VARCHAR(50),
    plant_id VARCHAR(50),
    id VARCHAR(50), -- 허가번호
    equipment_id VARCHAR(50) NOT NULL,
    work_order_id VARCHAR(50), -- 연계 작업지시 번호
    title VARCHAR(150) NOT NULL,
    step_stage CHAR(1) NOT NULL, -- P(계획), A(완료)
    permit_type_codes TEXT NOT NULL, -- 다중유형 콤마구분 문자열(예: 'GENERAL,FIRE,CONFINED')
    start_at TIMESTAMP,
    end_at TIMESTAMP,
    department_id VARCHAR(50) NOT NULL,
    supervisor_id VARCHAR(50) NOT NULL, -- 안전감독자
    work_summary TEXT,
    risk_factors TEXT,
    safety_measures TEXT,
    -- JSONB 체크시트 데이터 저장
    json_general JSONB,  -- 일반 작업 체크시트
    json_fire JSONB,     -- 화기 작업 체크시트
    json_confined JSONB, -- 밀폐 공간 작업 체크시트
    json_electric JSONB, -- 전기 작업 체크시트
    json_high_place JSONB, -- 고소 작업 체크시트
    json_excavation JSONB, -- 굴착 작업 체크시트
    json_heavy_load JSONB, -- 중량물 취급 체크시트
    remarks TEXT,
    file_group_id BIGINT,
    ref_no VARCHAR(50),
    ref_module VARCHAR(50),
    approval_id VARCHAR(50),
    status CHAR(1) NOT NULL DEFAULT 'T', -- T, P, C, R, S
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    delete_yn CHAR(1) NOT NULL DEFAULT 'N',
    PRIMARY KEY (company_id, plant_id, id),
    FOREIGN KEY (company_id, plant_id, equipment_id) REFERENCES equipment(company_id, plant_id, id),
    FOREIGN KEY (company_id, plant_id, work_order_id) REFERENCES work_order(company_id, plant_id, id),
    FOREIGN KEY (company_id, department_id) REFERENCES department(company_id, id),
    FOREIGN KEY (company_id, supervisor_id) REFERENCES users(company_id, id),
    FOREIGN KEY (company_id, approval_id) REFERENCES approval(company_id, id)
);

-- 21. 재고현황
CREATE TABLE inventory_status (
    company_id VARCHAR(50),
    warehouse_id VARCHAR(50),
    inventory_id VARCHAR(50),
    qty NUMERIC(15, 4) NOT NULL DEFAULT 0,
    amount NUMERIC(19, 4) NOT NULL DEFAULT 0, -- 총 재고 평가액
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    delete_yn CHAR(1) NOT NULL DEFAULT 'N',
    PRIMARY KEY (company_id, warehouse_id, inventory_id),
    FOREIGN KEY (company_id, warehouse_id) REFERENCES warehouse(company_id, id),
    FOREIGN KEY (company_id, inventory_id) REFERENCES inventory(company_id, id)
);

-- 22. 재고이력
CREATE TABLE inventory_history (
    company_id VARCHAR(50) NOT NULL,
    warehouse_id VARCHAR(50) NOT NULL,
    inventory_id VARCHAR(50) NOT NULL,
    history_no BIGSERIAL, -- PK 대리키
    tx_type_code VARCHAR(50) NOT NULL, -- 입고(IN), 출고(OUT), 이동(MOVE), 조정(ADJ) 등 공통코드
    qty NUMERIC(15, 4) NOT NULL,
    unit_price NUMERIC(19, 4) NOT NULL, -- 입/출고 단가
    amount NUMERIC(19, 4) NOT NULL, -- 처리 금액 (수량 * 단가)
    tx_date DATE NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    ref_no VARCHAR(50), -- 연계 이동이력의 출고번호 등 참조번호
    ref_module VARCHAR(50), -- 참조 모듈명
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    delete_yn CHAR(1) NOT NULL DEFAULT 'N',
    PRIMARY KEY (company_id, warehouse_id, inventory_id, history_no),
    FOREIGN KEY (company_id, warehouse_id, inventory_id) REFERENCES inventory_status(company_id, warehouse_id, inventory_id)
);

-- 23. 재고마감
CREATE TABLE inventory_monthly_closing (
    company_id VARCHAR(50),
    warehouse_id VARCHAR(50),
    inventory_id VARCHAR(50),
    closing_ym CHAR(6), -- YYYYMM
    in_qty NUMERIC(15, 4) NOT NULL DEFAULT 0,
    in_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    out_qty NUMERIC(15, 4) NOT NULL DEFAULT 0,
    out_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    move_qty NUMERIC(15, 4) NOT NULL DEFAULT 0,
    move_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    adj_qty NUMERIC(15, 4) NOT NULL DEFAULT 0,
    adj_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    closing_qty NUMERIC(15, 4) NOT NULL DEFAULT 0, -- 마감 현재고
    closing_amount NUMERIC(19, 4) NOT NULL DEFAULT 0, -- 마감 재고금액
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    delete_yn CHAR(1) NOT NULL DEFAULT 'N',
    PRIMARY KEY (company_id, warehouse_id, inventory_id, closing_ym),
    FOREIGN KEY (company_id, warehouse_id, inventory_id) REFERENCES inventory_status(company_id, warehouse_id, inventory_id)
);
```

### 2.5. 게시판 및 댓글 (Board & Comment)

```sql
-- 24. 게시판 (공지사항 등)
CREATE TABLE board (
    company_id VARCHAR(50) REFERENCES company(id),
    id BIGSERIAL,
    board_type_code VARCHAR(50) NOT NULL, -- 공통코드
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    notice_yn CHAR(1) NOT NULL DEFAULT 'N', -- 공지글 여부 (최상단 고정용)
    file_group_id BIGINT,
    ref_no VARCHAR(50),
    ref_module VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    delete_yn CHAR(1) NOT NULL DEFAULT 'N',
    PRIMARY KEY (company_id, id)
);

-- 25. 게시판 댓글 (상세, 공통 컬럼 제외)
CREATE TABLE board_comment (
    company_id VARCHAR(50),
    board_id BIGINT,
    comment_no BIGINT, -- 1부터 시퀀스 또는 대리키
    author_id VARCHAR(50) NOT NULL,
    author_name VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (company_id, board_id, comment_no),
    FOREIGN KEY (company_id, board_id) REFERENCES board(company_id, id) ON DELETE CASCADE
);
```

### 2.6. 시스템 공통 및 인터페이스 (System & Interface)

```sql
-- 26. 파일첨부 마스터
CREATE TABLE file_attachment (
    company_id VARCHAR(50) REFERENCES company(id),
    group_no BIGSERIAL, -- 파일그룹 일련번호
    ref_no VARCHAR(50), -- 참조 문서번호
    ref_module VARCHAR(50), -- 참조 모듈
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    delete_yn CHAR(1) NOT NULL DEFAULT 'N',
    PRIMARY KEY (company_id, group_no)
);

-- 27. 파일첨부 아이템 (상세이므로 공통 컬럼 제외)
CREATE TABLE file_attachment_item (
    company_id VARCHAR(50),
    group_no BIGINT,
    item_no INT,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255) NOT NULL,
    file_extension VARCHAR(10),
    mime_type VARCHAR(100),
    file_size BIGINT NOT NULL,
    checksum_sha256 CHAR(64) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    PRIMARY KEY (company_id, group_no, item_no),
    FOREIGN KEY (company_id, group_no) REFERENCES file_attachment(company_id, group_no) ON DELETE CASCADE
);

-- 28. 일련번호 생성기 (자동 채번용)
CREATE TABLE sequence_generator (
    company_id VARCHAR(50) REFERENCES company(id),
    ref_module VARCHAR(50) NOT NULL, -- 예: 'PM', 'WO', 'WP', 'APV'
    department_id VARCHAR(50),       -- 부서별 채번을 원할 시 사용
    year_month CHAR(6) NOT NULL,     -- YYYYMM (예: 202605)
    last_seq INT NOT NULL DEFAULT 0, -- 마지막 시퀀스 번호
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    delete_yn CHAR(1) NOT NULL DEFAULT 'N',
    PRIMARY KEY (company_id, ref_module, department_id, year_month)
);

-- 29. 로그인 이력
CREATE TABLE login_history (
    company_id VARCHAR(50),
    user_id VARCHAR(50),
    login_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    login_ip VARCHAR(50),
    login_result VARCHAR(20) NOT NULL, -- SUCCESS, FAILED
    PRIMARY KEY (company_id, user_id, login_at)
);

-- 30. 전력거래소(KPX) 인터페이스 정보
CREATE TABLE kpx_interface (
    company_id VARCHAR(50) REFERENCES company(id),
    kpx_member_id VARCHAR(50) NOT NULL,
    target_ym CHAR(6) NOT NULL, -- YYYYMM
    hour_interval INT NOT NULL, -- 0 ~ 23시
    generation_qty NUMERIC(15, 4) NOT NULL DEFAULT 0, -- 발전량
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    delete_yn CHAR(1) NOT NULL DEFAULT 'N',
    PRIMARY KEY (company_id, kpx_member_id, target_ym, hour_interval)
);
```

---

## 3. 정합성 및 성능 인덱스 가이드

1.  **소프트 딜리트 인덱스**: 모든 소프트 딜리트 대상 테이블의 조회 쿼리에 `delete_yn = 'N'` 조건이 포함되므로, 조회 빈도가 높은 테이블(`users`, `equipment`, `inventory_status`, `work_order`, `pm_record`)에는 복합 기본키 외에 `(company_id, delete_yn)` 또는 `(delete_yn)` 필터를 지원하는 부분 인덱스(Partial Index)를 적용하여 조회 효율을 극대화합니다.
2.  **외래키 인덱스**: PostgreSQL은 외래키 제약조건 설정 시 인덱스를 자동으로 생성해주지 않으므로, 성능 저하 방지 및 삭제 연산 가속을 위해 모든 참조 대상 FK 컬럼에 명시적 인덱스를 추가할 예정입니다.

---

## 4. 시스템 예약 공통코드 명세 (System Reserved Common Codes)

백엔드 로직(Enum) 및 프론트엔드 화면 구성 시 하드코딩을 방지하고 DB 기준의 명칭을 유연하게 사용하기 위해, 다음 코드 그룹 및 코드 아이템을 **시스템 예약 코드**로 지정하여 초기 DB 시드로 제공합니다.

### 4.1. `EQ_TYPE` (설비 타입)
*   **용도**: 설비 마스터 등록 시 설비 종류 구분
*   **코드 그룹 ID**: `EQ_TYPE`
*   **예약 코드 아이템**:
    *   `PUMP`: 펌프
    *   `MOTOR`: 모터
    *   `BOILER`: 보일러
    *   `VALVE`: 밸브
    *   `COMPRESSOR`: 압축기
    *   `PANEL`: 판넬
    *   `ETC`: 기타 설비

### 4.2. `ITEM_TYPE` (자재/부품 타입)
*   **용도**: 재고 마스터 등록 시 자재 종류 구분
*   **코드 그룹 ID**: `ITEM_TYPE`
*   **예약 코드 아이템**:
    *   `SPARE_PART`: 예비품 (설비 수리용 부품)
    *   `CONSUMABLE`: 소모품 (오일, 벨트 등 주기적 교체 대상)
    *   `TOOL`: 공구류 (작업용 장비)
    *   `ETC`: 기타 자재
### 4.3. `WO_TYPE` (작업지시 타입)
*   **용도**: 작업지시 등록 시 작업의 성격 분류
*   **코드 그룹 ID**: `WO_TYPE`
*   **예약 코드 아이템**:
    *   `BM`: 고장정비 (Breakdown Maintenance)
    *   `PM`: 예방보전 (Preventive Maintenance)
    *   `CM`: 개조/개선 (Corrective Maintenance)
    *   `ETC`: 기타 작업

### 4.4. `WP_TYPE` (작업허가 유형)
*   **용도**: 작업허가서 작성 시 다중 선택하는 안전 작업 종류 (일반은 상시 필수)
*   **코드 그룹 ID**: `WP_TYPE`
*   **예약 코드 아이템**:
    *   `GENERAL`: 일반위험작업 (기본 양식)
    *   `FIRE`: 화기작업 (불꽃, 고온 발생)
    *   `CONFINED`: 밀폐공간출입 (산소결핍, 질식 우려)
    *   `ELECTRIC`: 정전/전기작업 (감전 위험)
    *   `HIGH_PLACE`: 고소작업 (추락 위험)
    *   `EXCAVATION`: 굴착작업 (붕괴, 지하매설물 위험)
    *   `HEAVY_LOAD`: 중량물취급 (크레인, 낙하 위험)

### 4.5. `PM_TYPE` (점검/보전 타입)
*   **용도**: 예방점검 기록 및 설비 마스터 점검주기 설정 시 보전 성격 구분
*   **코드 그룹 ID**: `PM_TYPE`
*   **예약 코드 아이템**:
    *   `INSPECT`: 예방점검 (정밀 측정 및 육안 점검)
    *   `PATROL`: 순회점검 (정기 순회 및 간이 점검)
    *   `REPLACE`: 소모품교체 (부품 및 필터/오일 정기 교환)
    *   `LEGAL`: 법정검사 (소방/안전 등 관련 법에 따른 법정 정기 검사)

---

### 4.6. 어플리케이션 상수 및 Enum 명세 (Application Level Enums)
아래 항목들은 물리적인 계산식이나 상태 처리에 고정 결합되어 있으므로, 데이터베이스에 저장하지 않고 백엔드(Java Enum) 및 프론트엔드(TypeScript Union Type) 코드레벨에서 상수로 관리합니다.

#### 1) `PM_CYCLE_UNIT` (점검 주기 단위)
*   `D`: 일 (Daily)
*   `W`: 주 (Weekly)
*   `M`: 월 (Monthly)
*   `Y`: 년 (Yearly)

#### 2) `JUDGE_CODE` (점검 판정)
*   `GOOD`: 양호 (정상 가동)
*   `BAD`: 불량 (정비 필요)
*   `OTHER`: 기타 (운휴 등 특이사항)

#### 3) `TX_TYPE` (재고 처리 유형)
*   `IN`: 입고 (구매 입고 등)
*   `OUT`: 출고 (정비용 자재 출고 등)
*   `MOVE`: 이동 (저장소 간 이동)
*   `ADJ`: 실사조정 (재고조사 후 수량 조정)



