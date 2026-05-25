-- ==========================================
-- 1. 조직 및 기준정보 (Organization & MDM)
-- ==========================================

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
    multi_plant CHAR(1) NOT NULL DEFAULT 'N',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    delete_yn CHAR(1) NOT NULL DEFAULT 'N',
    PRIMARY KEY (company_id, id)
);

-- 5. 권한 상세 (C/R/U/D/A 권한 매트릭스)
CREATE TABLE role_detail (
    company_id VARCHAR(50),
    role_id VARCHAR(50),
    module_detail VARCHAR(100),
    perm_c CHAR(1) NOT NULL DEFAULT 'N',
    perm_r CHAR(1) NOT NULL DEFAULT 'N',
    perm_u CHAR(1) NOT NULL DEFAULT 'N',
    perm_d CHAR(1) NOT NULL DEFAULT 'N',
    perm_a CHAR(1) NOT NULL DEFAULT 'N',
    PRIMARY KEY (company_id, role_id, module_detail),
    FOREIGN KEY (company_id, role_id) REFERENCES role(company_id, id) ON DELETE CASCADE
);

-- 6. 사용자
CREATE TABLE users (
    company_id VARCHAR(50),
    id VARCHAR(50),
    name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(256) NOT NULL,
    department_id VARCHAR(50),
    role_id VARCHAR(50),
    email VARCHAR(100),
    phone VARCHAR(50),
    position VARCHAR(50),
    title VARCHAR(50),
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
    id VARCHAR(50),
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
CREATE TABLE code_item (
    company_id VARCHAR(50),
    group_id VARCHAR(50),
    id VARCHAR(50),
    name VARCHAR(100) NOT NULL,
    legal_inspect_yn CHAR(1) NOT NULL DEFAULT 'N',
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (company_id, group_id, id),
    FOREIGN KEY (company_id, group_id) REFERENCES code_group(company_id, id) ON DELETE CASCADE
);


-- ==========================================
-- 2. 마스터 정보 (Master Info)
-- ==========================================

-- 10. 설비 마스터
CREATE TABLE equipment (
    company_id VARCHAR(50),
    plant_id VARCHAR(50),
    id VARCHAR(50),
    name VARCHAR(100) NOT NULL,
    location VARCHAR(150),
    eq_type_code VARCHAR(50),
    install_date DATE,
    work_permit_yn CHAR(1) NOT NULL DEFAULT 'N',
    maker_name VARCHAR(100),
    spec TEXT,
    model VARCHAR(100),
    serial_number VARCHAR(100),
    remarks TEXT,
    file_group_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    delete_yn CHAR(1) NOT NULL DEFAULT 'N',
    PRIMARY KEY (company_id, plant_id, id),
    FOREIGN KEY (company_id, plant_id) REFERENCES plant(company_id, id)
);

-- 11. 설비 마스터 점검 항목 (상세)
CREATE TABLE equipment_check_item (
    company_id VARCHAR(50),
    plant_id VARCHAR(50),
    equipment_id VARCHAR(50),
    item_no INT,
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
    check_type_code VARCHAR(50),
    cycle_val INT NOT NULL,
    cycle_unit VARCHAR(10) NOT NULL,
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
    id VARCHAR(50),
    name VARCHAR(150) NOT NULL,
    item_type_code VARCHAR(50),
    department_id VARCHAR(50),
    unit VARCHAR(20),
    maker_name VARCHAR(100),
    spec TEXT,
    model VARCHAR(100),
    serial_number VARCHAR(100),
    safety_qty NUMERIC(15, 4) NOT NULL DEFAULT 0,
    reorder_qty NUMERIC(15, 4) NOT NULL DEFAULT 0,
    lead_time_days INT NOT NULL DEFAULT 0,
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


-- ==========================================
-- 3. 결재 모듈 (Approval)
-- ==========================================

-- 14. 결재 마스터
CREATE TABLE approval (
    company_id VARCHAR(50) REFERENCES company(id),
    id VARCHAR(50),
    title VARCHAR(150) NOT NULL,
    content TEXT,
    drafter_id VARCHAR(50) NOT NULL,
    file_group_id BIGINT,
    status CHAR(1) NOT NULL DEFAULT 'T',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    delete_yn CHAR(1) NOT NULL DEFAULT 'N',
    PRIMARY KEY (company_id, id),
    FOREIGN KEY (company_id, drafter_id) REFERENCES users(company_id, id)
);

-- 15. 결재 단계 (상세)
CREATE TABLE approval_step (
    company_id VARCHAR(50),
    approval_id VARCHAR(50),
    step_no INT,
    approver_id VARCHAR(50) NOT NULL,
    approval_type CHAR(1) NOT NULL,
    approval_result CHAR(1) NOT NULL,
    action_at TIMESTAMP,
    comments TEXT,
    PRIMARY KEY (company_id, approval_id, step_no),
    FOREIGN KEY (company_id, approval_id) REFERENCES approval(company_id, id) ON DELETE CASCADE,
    FOREIGN KEY (company_id, approver_id) REFERENCES users(company_id, id)
);


-- ==========================================
-- 4. 트랜잭션 모듈 (Transaction)
-- ==========================================

-- 16. 예방점검 기록
CREATE TABLE pm_record (
    company_id VARCHAR(50),
    plant_id VARCHAR(50),
    id VARCHAR(50),
    equipment_id VARCHAR(50) NOT NULL,
    department_id VARCHAR(50) NOT NULL,
    check_type_code VARCHAR(50) NOT NULL,
    work_date DATE NOT NULL,
    worker_id VARCHAR(50) NOT NULL,
    judge_code VARCHAR(20) NOT NULL,
    remarks TEXT,
    cert_number VARCHAR(100),
    cert_expire_date DATE,
    cert_agency VARCHAR(100),
    approval_id VARCHAR(50),
    status CHAR(1) NOT NULL DEFAULT 'T',
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
    check_value NUMERIC(15, 4),
    PRIMARY KEY (company_id, plant_id, pm_record_id, item_no),
    FOREIGN KEY (company_id, plant_id, pm_record_id) REFERENCES pm_record(company_id, plant_id, id) ON DELETE CASCADE
);

-- 18. 작업지시
CREATE TABLE work_order (
    company_id VARCHAR(50),
    plant_id VARCHAR(50),
    id VARCHAR(50),
    equipment_id VARCHAR(50) NOT NULL,
    title VARCHAR(150) NOT NULL,
    step_stage CHAR(1) NOT NULL,
    wo_type_code VARCHAR(50) NOT NULL,
    department_id VARCHAR(50) NOT NULL,
    worker_id VARCHAR(50),
    work_date DATE,
    cost NUMERIC(15, 2) NOT NULL DEFAULT 0,
    man_hours NUMERIC(8, 2) NOT NULL DEFAULT 0,
    man_hours_unit VARCHAR(10) NOT NULL DEFAULT 'H',
    remarks TEXT,
    file_group_id BIGINT,
    ref_no VARCHAR(50),
    ref_module VARCHAR(50),
    approval_id VARCHAR(50),
    status CHAR(1) NOT NULL DEFAULT 'T',
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
    id VARCHAR(50),
    equipment_id VARCHAR(50) NOT NULL,
    work_order_id VARCHAR(50),
    title VARCHAR(150) NOT NULL,
    step_stage CHAR(1) NOT NULL,
    permit_type_codes TEXT NOT NULL,
    start_at TIMESTAMP,
    end_at TIMESTAMP,
    department_id VARCHAR(50) NOT NULL,
    supervisor_id VARCHAR(50) NOT NULL,
    work_summary TEXT,
    risk_factors TEXT,
    safety_measures TEXT,
    json_general JSONB,
    json_fire JSONB,
    json_confined JSONB,
    json_electric JSONB,
    json_high_place JSONB,
    json_excavation JSONB,
    json_heavy_load JSONB,
    remarks TEXT,
    file_group_id BIGINT,
    ref_no VARCHAR(50),
    ref_module VARCHAR(50),
    approval_id VARCHAR(50),
    status CHAR(1) NOT NULL DEFAULT 'T',
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

-- 21. 재고 현황
CREATE TABLE inventory_status (
    company_id VARCHAR(50),
    warehouse_id VARCHAR(50),
    inventory_id VARCHAR(50),
    qty NUMERIC(15, 4) NOT NULL DEFAULT 0,
    amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    delete_yn CHAR(1) NOT NULL DEFAULT 'N',
    PRIMARY KEY (company_id, warehouse_id, inventory_id),
    FOREIGN KEY (company_id, warehouse_id) REFERENCES warehouse(company_id, id),
    FOREIGN KEY (company_id, inventory_id) REFERENCES inventory(company_id, id)
);

-- 22. 재고 이력
CREATE TABLE inventory_history (
    company_id VARCHAR(50) NOT NULL,
    warehouse_id VARCHAR(50) NOT NULL,
    inventory_id VARCHAR(50) NOT NULL,
    history_no BIGSERIAL,
    tx_type_code VARCHAR(50) NOT NULL,
    qty NUMERIC(15, 4) NOT NULL,
    unit_price NUMERIC(19, 4) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    tx_date DATE NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    ref_no VARCHAR(50),
    ref_module VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    delete_yn CHAR(1) NOT NULL DEFAULT 'N',
    PRIMARY KEY (company_id, warehouse_id, inventory_id, history_no),
    FOREIGN KEY (company_id, warehouse_id, inventory_id) REFERENCES inventory_status(company_id, warehouse_id, inventory_id)
);

-- 23. 재고 마감
CREATE TABLE inventory_monthly_closing (
    company_id VARCHAR(50),
    warehouse_id VARCHAR(50),
    inventory_id VARCHAR(50),
    closing_ym CHAR(6),
    in_qty NUMERIC(15, 4) NOT NULL DEFAULT 0,
    in_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    out_qty NUMERIC(15, 4) NOT NULL DEFAULT 0,
    out_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    move_qty NUMERIC(15, 4) NOT NULL DEFAULT 0,
    move_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    adj_qty NUMERIC(15, 4) NOT NULL DEFAULT 0,
    adj_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    closing_qty NUMERIC(15, 4) NOT NULL DEFAULT 0,
    closing_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    delete_yn CHAR(1) NOT NULL DEFAULT 'N',
    PRIMARY KEY (company_id, warehouse_id, inventory_id, closing_ym),
    FOREIGN KEY (company_id, warehouse_id, inventory_id) REFERENCES inventory_status(company_id, warehouse_id, inventory_id)
);


-- ==========================================
-- 5. 게시판 및 댓글 (Board & Comment)
-- ==========================================

-- 24. 게시판
CREATE TABLE board (
    company_id VARCHAR(50) REFERENCES company(id),
    id BIGSERIAL,
    board_type_code VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    notice_yn CHAR(1) NOT NULL DEFAULT 'N',
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

-- 25. 게시판 댓글 (상세)
CREATE TABLE board_comment (
    company_id VARCHAR(50),
    board_id BIGINT,
    comment_no BIGINT,
    author_id VARCHAR(50) NOT NULL,
    author_name VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (company_id, board_id, comment_no),
    FOREIGN KEY (company_id, board_id) REFERENCES board(company_id, id) ON DELETE CASCADE
);


-- ==========================================
-- 6. 시스템 공통 및 인터페이스 (System & Interface)
-- ==========================================

-- 26. 파일첨부 마스터
CREATE TABLE file_attachment (
    company_id VARCHAR(50) REFERENCES company(id),
    group_no BIGSERIAL,
    ref_no VARCHAR(50),
    ref_module VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    delete_yn CHAR(1) NOT NULL DEFAULT 'N',
    PRIMARY KEY (company_id, group_no)
);

-- 27. 파일첨부 아이템 (상세)
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
    ref_module VARCHAR(50) NOT NULL,
    department_id VARCHAR(50),
    year_month CHAR(6) NOT NULL,
    last_seq INT NOT NULL DEFAULT 0,
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
    login_result VARCHAR(20) NOT NULL,
    PRIMARY KEY (company_id, user_id, login_at)
);

-- 30. 전력거래소(KPX) 인터페이스 정보
CREATE TABLE kpx_interface (
    company_id VARCHAR(50) REFERENCES company(id),
    kpx_member_id VARCHAR(50) NOT NULL,
    target_ym CHAR(6) NOT NULL,
    hour_interval INT NOT NULL,
    generation_qty NUMERIC(15, 4) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    delete_yn CHAR(1) NOT NULL DEFAULT 'N',
    PRIMARY KEY (company_id, kpx_member_id, target_ym, hour_interval)
);
