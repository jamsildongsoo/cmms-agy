# CMMS 코드 리뷰 미수정 항목 (TODO)

> 기준일: 2026-05-26  
> 소스코드 직접 검증 후 미반영 확인된 항목만 기록

---

## 우선순위 HIGH — 서비스 안정성 직접 영향

### [ ] C3. 채번(Sequence) 동시성 결함 — 문서번호 중복 위험

- **파일**: `backend/src/main/java/com/cmms/service/SequenceService.java`
- **현상**: `@Transactional + synchronized` 조합 사용 중. synchronized는 메서드 종료 시 해제되지만 트랜잭션 커밋은 그 이후 발생 → 스레드 B가 커밋 전 진입하여 동일 lastSeq 읽음 → 중복 번호 생성. 멀티 인스턴스 환경에서 synchronized 자체 무력.
- **조치**: `SequenceGeneratorRepository`에 `@Lock(LockModeType.PESSIMISTIC_WRITE)` + JPQL 쿼리 추가, `SequenceService`에서 `synchronized` 제거 후 락 쿼리 사용으로 교체
- **참고**: `InventoryStatusRepository.findByIdWithLock()` 패턴 동일 적용

### [x] C4. 예약 공통코드 신규 테넌트 미배포 — 해결 (컴파일 검증, 런타임 보류)
- 회사 생성 시 `CompanyService.copySystemCommonCodes`로 SYSTEM 공통코드(CodeGroup+CodeItem)를 신규 회사로 복사. (가입(signup)은 회사 생성 안 함 → 회사 생성은 sysadmin 전용 경로에서 복사됨)

### [x] M1. 신규 회사 첫 가입자 데드엔드 — 온보딩 모델로 해소 (컴파일 검증, 런타임 보류)
- 첫 사용자 자동 ADMIN/Y 방식은 **롤백**. 대신: 회사 생성=sysadmin 전용 / 가입=참여 전용(useYn='N') / **활성화=sysadmin이 SYSTEM 콘솔(`PUT /api/system/users/{c}/{u}/use-yn`)에서** 처리. → 데드엔드 해소.

### [x] [추가] 코드 ID 정규화 + SYSTEM 콘솔 (컴파일/빌드 검증, 런타임 보류)
- `CodeUtil`(대문자+`[A-Z0-9_-]`) — company/role/dept/plant/warehouse/codeGroup/codeItem 쓰기 지점 적용. userId 제외. 롤ID 케이스 불일치 권한 버그 해소.
- `SystemAdminController/Service`(`/api/system`, SYSTEM 전용, 교차 테넌트) + FE `SystemAdmin.tsx`(사용자/로그인이력) + Sidebar SYSTEM 메뉴.
- 가입 참여 전용(self 회사생성 제거), 회사코드 정규화. V5(sysadmin 롤 대문자).

---

## 구매(Procurement) 도입 + 부수 정리 — 신규 기능 (HIGH)

> 설계 단일 소스: `docs/procurement.md`. PRD/DB명세/UI명세 분산 정리됨.

### [ ] P1. AppModule 코드 ≤3자 통일 + 한글 `label()` 추가

- **변경 대상**: `backend/src/main/java/com/cmms/security/AppModule.java`
- **rename**: `EQUIPMENT`→`EQP`, `INVENTORY`→`INV`, `STOCK`→`STK`, `APPROVAL`→`APR`, `BOARD`→`BRD`; **신규** `PUR` 추가. (MDM/PM/WO/WP 변동 없음.)
- **`label()` 메서드 추가** — 한글 라벨 단일 소스(예: `APR("전자결재")`). FE는 BE에서 라벨 가져다 씀.
- **콜사이트 일괄 치환** (총 26곳):
  - `controller/MasterController.java` `@perm.check('EQUIPMENT',…)` × 6, `'INVENTORY'` × 5
  - `controller/ApprovalController.java` `'APPROVAL'` × 5
  - `controller/InventoryTransactionController.java` `'STOCK'` × 4
  - `controller/BoardController.java` `'BOARD'` × 6
- **`SeqModule.java:6`** 주석에서 "결재는 여기선 APR, 권한에선 APPROVAL" 분기 표현 제거(이제 일치).
- **신규 모듈 메타 엔드포인트**: `/api/meta/modules` → `[{code,label}]` (FE 라벨맵 대체).
- **FE 정리**: `frontend/src/pages/MdmLayout.tsx:705~712` 라벨맵 제거 → BE meta 사용.

### [ ] P2. 문서번호 포맷 변경: `{모듈}-{부서}-{년월}-{순번}`

- **근거**: 현 포맷 `{모듈}-{년월}-{순번}`은 부서가 다른 두 사용자가 같은 회사·플랜트·같은 월에 WO/WP/PM 생성 시 **동일 번호 → PK 충돌(`(company, plant, id)`) INSERT 실패** 잠재 버그.
- **변경**: `SequenceService.java:42`에 `departmentId` 포함 (`"%s-%s-%s-%04d"`).
- **APR 부서**: `ApprovalService.java:55` 현 `"DEPT_ROOT"` → **기안자 부서** 전달(`drafter.departmentId`).
- **WO/WP/PM 호출부 무변경** (이미 dept 전달 중). **PUR/STK는 신규**라 처음부터 요청자/조작자 부서 전달.
- **부서 없는 사용자 fallback** = `DEPT_ROOT` 유지(edge case).
- **컬럼 길이 검증/캡 추가 안 함**(`company.id`/`department.id` VARCHAR(50) 그대로). 실코드는 짧아 50자 안에 들어옴.
- **기존 데이터 영향 없음**(구포맷 id 그대로, 신규부터 새 포맷 — 혼재 허용).
- **회귀 확인**: WO/WP/PM/APR 새 번호 = 부서 세그먼트 포함 + 다부서 동시 생성 PK 충돌 없음.

### [ ] P3. V4 마이그레이션 (`V4__procurement.sql`)

- **DDL**:
  - `ALTER TABLE warehouse ADD COLUMN plant_id VARCHAR(50);` (nullable=공통부문)
  - `ALTER TABLE inventory_history ADD COLUMN doc_no VARCHAR(50);` (전표번호 STK 단일 체계, IN/OUT/MOVE/ADJ 공통)
  - `CREATE TABLE vendor` — (company_id, id, name NOT NULL, biz_no, contact, manager, remarks, BaseEntity 5컬럼, PK=(company_id, id))
  - `CREATE TABLE purchase_request` — (company_id, id, plant_id NOT NULL, warehouse_id NOT NULL, requester_id NOT NULL, request_date NOT NULL, request_type, vendor_id, order_date, eta_date, ship_start_date, **`status CHAR(1) NOT NULL DEFAULT 'T'`**, **`proc_status CHAR(1)`**(nullable), remarks, BaseEntity, PK=(company_id, id), FK plant/warehouse/requester/vendor)
  - `CREATE TABLE purchase_request_item` — (company_id, request_id, line_no, inventory_id NOT NULL, qty NUMERIC(15,4) NOT NULL, unit VARCHAR(20), received_qty NUMERIC(15,4) DEFAULT 0, remarks, PK=(company_id, request_id, line_no), FK CASCADE)
- **SEED / UPDATE**:
  - `UPDATE role_detail SET module_detail='APR' WHERE module_detail='APPROVAL';`
  - `UPDATE role_detail SET module_detail='STK' WHERE module_detail='STOCK';`
  - `UPDATE role_detail SET module_detail='EQP' WHERE module_detail='EQUIPMENT';`
  - `UPDATE role_detail SET module_detail='INV' WHERE module_detail='INVENTORY';`
  - `UPDATE role_detail SET module_detail='BRD' WHERE module_detail='BOARD';`
  - 기존 회사들에 `PUR` `role_detail` 행 INSERT (ADMIN=CRUDA, MANAGER=CRUDA, USER=CRUD A=N — `createDefaultRoleDetails` 매트릭스 동일)
  - `UPDATE role SET multi_plant='Y' WHERE id='ADMIN';` (현재 전부 N)
  - `INSERT INTO code_group(company_id='SYSTEM', id='PR_TYPE', name='구매요청유형', system_use_yn='Y')` + items: `NORMAL`(일반·10), `ROUTINE`(경상·20), `PLANNED_PM`(계획예방정비·30), `URGENT`(긴급·40), `ETC`(기타·99) — SYSTEM 공유 코드(신규 회사 추가 시드 불필요)

### [ ] P4. `DocStatus` enum 확장 (절차상태 O/D/I/E 추가)

- **변경**: `backend/src/main/java/com/cmms/constant/DocStatus.java`
- **추가**: `ORDERED("O")`, `SHIPPING("D")`, `RECEIVED("I")`, `CLOSED("E")` — **O**rder/**D**elivery/**I**ncoming/**E**nd.
- 기존 `T/P/C/S/R/X`와 비충돌. 단일 enum 단일 소스. **별도 ProcStatus enum 안 만듦**(분기 최소화).

### [ ] P5. 백엔드: 구매(Procurement) 도메인 신설

- **모델/IdClass/Repository**: `Vendor`+`VendorId`, `PurchaseRequest`+`PurchaseRequestId`, `PurchaseRequestItem`+`PurchaseRequestItemId` (복합키, BaseEntity 상속, JpaRepository).
- **기존 모델 컬럼 추가**:
  - `Warehouse.java` — `plantId`(`plant_id` len 50, nullable).
  - `InventoryHistory.java` — `docNo`(`doc_no` len 50).
- **DTO**: `VendorDto`, `PurchaseRequestDto.{SaveRequest, OrderRequest, ReceiveRequest{requestId, close:bool, lines}, CloseRequest}`.
- **`VendorService` + `VendorController`** (`/api/vendors`, `@perm.check('PUR',…)`): CRUD (`PlantManager` 패턴 복제).
- **`ProcurementService` + `ProcurementController`** (`/api/procurement`):
  - `createRequest`(저장 `status=T`)/`confirmRequest`(확정 `status=S`, 현장; `checkSave('PUR','S')` → C+A 권한): `SequenceService.generateNextNo(companyId, "PUR", requester.departmentId)`; plantId 컨텍스트 주입(클라 입력 금지).
  - `placeOrder`(→`proc_status='O'`, vendor·orderDate·etaDate)/`startShipping`(→`proc_status='D'`, shipStartDate) — 구매자, `@perm.check('PUR','U')`.
  - `receive`(현장, `@perm.check('STK','C')` — 재고 반영): **확정(`status='S'`) PR만**. STK 전표번호 채번 → 라인별 `InventoryTransactionService.processTransactions()` 위임(`TxItem{txTypeCode:"IN", warehouseId(=PR), inventoryId, qty, unitPrice, docNo, refModule:"PUR", refNo:requestId}`) → PR 라인 `receivedQty` 누적 → `proc_status='I'`. 수량 과소/초과 판정 없음. `ReceiveRequest.close=true`면 입고 후 곧바로 `proc_status='E'`.
  - `closeRequest`(→`proc_status='E'`, 현장; `@perm.check('PUR','U')`): **독립 액션** — 미입고 PR도 호출 가능, 입고 모달 close 플래그와 동일 경로.
  - `getRequests`: 플랜트 스코프(일반=본인 플랜트 고정, 멀티=선택/전체). 입고 대상 조회는 추가로 발주/배송 이후·미종료(`proc_status != 'E'`) 필터(수량 무판정). 타 플랜트 PR은 서버 차단.
- **전표번호 docNo 적재**: `InventoryTxDto.TxItem`에 optional `docNo`/`refNo`/`refModule` 추가, **이벤트마다 `SequenceService.generateNextNo(companyId,"STK", operator.departmentId)` 채번**(IN/OUT/MOVE/ADJ 공통, GR/GI 구분 없음) → `executeCheckIn/Out/Adjustment/Transfer`가 history에 세팅. `unitPrice null→0`.
- **이력 조회**: `/api/inventory-tx/history`에 `docNo` 검색 + 전표번호별 그룹화(라인 표시 순번 부여).
- **입고 취소(역분개)**: 신규 엔드포인트(`POST /api/procurement/receive-cancel/{docNo}` 또는 `/api/inventory-tx/cancel/{docNo}`). 해당 전표의 IN을 같은 docNo로 OUT 역분개 + PR `receivedQty` 차감. **조건: 그 입고 이후 동일 `(company, warehouse, inventory)` history에 어떤 transaction(IN/OUT/MOVE/ADJ)도 없을 때만 가능**. `InventoryHistoryRepository`에 후속-tx 존재 체크 메서드 추가. 위반 시 명시적 에러("후속 거래가 있어 취소 불가").
- **`MdmService`** (`service/MdmService.java`): `saveUser`/`updateUser`(L212/227)에 `lastLoginPlantId`(관리자 지정 — 지정 시 로그인 자동매핑보다 우선); `saveWarehouse`/`updateWarehouse`(L262/275)에 `plantId`; 역할 저장/수정에 `multiPlant` 반영.
- **`CompanyService.seedRoles`**(L121): ADMIN 역할 `multiPlant("Y")` (현재 전부 N). `createDefaultRoleDetails`(L138)는 `AppModule.values()` 순회라 `PUR` 자동 포함.
- **`AuthService.login`**(L47): 응답에 (해소된)`lastLoginPlantId` + (역할에서 resolve)`multiPlant` + `companyName`(Company 조회) 추가. **로그인 시 플랜트 자동 해소**(L80~85 최종 로그인 갱신 블록): `lastLoginPlantId` null이면 회사 첫 활성 플랜트(`plant.deleteYn='N'`, id 정렬) 자동 매핑·저장 → 다음 로그인부터 재사용. 플랜트 0개면 null 유지(전체). 관리자 지정값 있으면 건드리지 않음.
- **`ApprovalService.java:55`**: `"DEPT_ROOT"` → `drafter.departmentId`(P2와 묶음).
- **플랜트 스코프 규칙(서버 강제)**:
  - 비멀티: 클라이언트 plantId 무시, 항상 `lastLoginPlantId` 고정, 타 플랜트 차단. `lastLoginPlantId` null이면 전체 조회 허용(데이터 미은닉). PR 생성은 plantId 해소 후 가능.
  - 멀티(`Role.multiPlant='Y'`): 요청 plantId 사용 또는 '전체'(필터 없음).
- **현황 플랜트 필터**: 대상 창고 = `warehouse.plant_id ∈ {유저플랜트, null(공통)}` → status 필터. '전체'(멀티)는 전 플랜트 합산.

### [ ] P6. 프론트엔드: 구매 화면 + 출력폼 + 플랜트 UI

- **신규 `components/PrintSignBox.tsx`**: 빈 수기 결재칸(Approval 박스 형식 차용, 칸당 ~96px) — **2열×4행 고정, 라벨 없는 빈 8칸**. PrintHeader와 조합.
- **`components/PrintHeader.tsx` 변경**: 표시 회사값 `companyId`→`companyName` (현재 `:24`가 코드 출력 중). 값은 `useAuthStore` `user.companyName`. 기존 사용처(Inventory/Equipment) 공통 반영, 그 외 자체 헤더 페이지 불변.
- **활성 플랜트 컨텍스트**: `store/useAuthStore.ts` — `User`에 `companyName`, `lastLoginPlantId`, `multiPlant`(역할에서 resolve) 노출 + `activePlantId` 상태/setter(로그인 시 `lastLoginPlantId`로 초기화, **변경은 멀티만**).
- **`components/Header.tsx`**: `multiPlant==='Y'`만 플랜트 셀렉터([플랜트 ▼ | 전체총괄]), 일반 유저는 플랜트 라벨(읽기전용; null이면 '전체').
- **MDM (`pages/MdmLayout.tsx`)**: 창고 폼에 플랜트 선택(공통부문 포함); 사용자 폼에 **지정 플랜트**(멀티 토글 없음); **`RoleManager`에 `multiPlant` 토글**. 구매요청유형은 기존 `CodeManager`(공통코드)로 관리. **모듈 라벨맵 제거**(BE `/api/meta/modules` 사용). **벤더는 MDM 아님** — 구매 화면으로.
- **신규 `pages/Procurement.tsx`** (내부 탭 **구매요청 / 벤더 관리**, `Approval.tsx`/`MdmLayout.tsx` 탭 패턴):
  - ▸**구매요청**: 목록(플랜트 스코프 — 일반=본인 플랜트만) + 상태 액션 — 신규/수정(자재+수량, 구매요청유형 선택, 플랜트→저장소 조합)·확정·**구매요청서 출력**(`PrintHeader`+`PrintSignBox`)[현장], **발주·배송 모달**(벤더·발주일·예정도착일)[구매자], **입고 모달**(입고 대상 PR=발주/배송 이후·미종료, 본인 플랜트 한정; 라인별 요청수량/기입고수량/잔여 표시 + **입고수량 입력란 기본값=잔여 프리필, 편집 가능(부분/과소·초과; 초과 시 경고만)**, 단가 미입력 허용 + **'이 요청 종료' 체크박스**)·**입고증 출력**·**독립 '종료' 액션**[현장]. **문서/절차 상태 뱃지 분리** — 라벨맵: 문서 `T:저장 / S:확정` ; 절차 `O:발주 / D:배송중 / I:입고 / E:종료` (null=발주대기).
  - ▸**벤더 관리**: 거래처 CRUD(`PlantManager` 패턴).
- **`pages/InventoryTransaction.tsx`**: 현황 **플랜트→저장소** 브레이크다운 + [내 저장소/전체] 토글 + 활성 플랜트 필터, 금액 0/미입력 graceful. 이력 뷰 **전표번호(docNo) 검색·그룹**. **입고증/출고증 = 기존 전표(Slip) 인쇄 모달(`:640~750`)을 docNo 단위로 확장** — 제목 `txTypeCode` 분기(IN→입고증/OUT→출고증/MOVE·ADJ→전표), 본문 헤더(전표번호·거래일자·창고·구매입고면 `refModule=PUR`/`refNo=PR번호`) + 라인표(표시순번|품목|수량|단위|단가|금액, 단가 0 graceful)+합계 + `PrintHeader`(회사명)+`PrintSignBox`(2×4). **목록 헤더는 기존 유지**. 수동 입출고는 STK 전표 채번. **입고 취소(역분개) 액션 추가**(전표 단위, 후속 tx 있으면 비활성).
- **`pages/Dashboard.tsx`** switch에 `case 'procurement'` + **`components/Sidebar.tsx`** '업무 트랜잭션' 그룹에 `procurement`(구매) 단일 항목 추가. 벤더는 별도 메뉴 없이 `Procurement.tsx` 내부 '벤더 관리' 탭.

### [ ] P7. 검증

- **빌드/기동**: `cd backend && ./gradlew build`(Flyway V4), `cd frontend && npm run build`. 통합은 8082 포트.
- **마이그레이션**: V4 적용 + role_detail 모듈 rename 5건 + PUR seed + `warehouse.plant_id`/`inventory_history.doc_no`/신규 테이블 + `role.multi_plant(ADMIN='Y')` 확인.
- **플랜트 스코핑(핵심 회귀)**: 일반 역할(plantA, multi=N)은 셀렉터 없음·plantA만, plantB 강제 전송해도 서버가 plantA 고정. 멀티 역할은 plantA/plantB/전체 전환·합산. **`lastLoginPlantId` null 유저**: 첫 로그인 시 기본 플랜트 자동 매핑·기록 → 재로그인 시 그 값 재사용; 플랜트 0개면 null(전체 조회).
- **번호 체계 회귀(핵심)**: WO/WP/PM/APR 새 번호 = `{모듈}-{부서}-{년월}-{순번}` 포맷 + **다부서 동시 생성 시 PK 충돌 없음** 확인.
- **엔드투엔드**: 벤더 등록 → (현장)구매요청 작성·확정(`status=S`)·**구매요청서 출력(수기결재칸)** → (구매자)발주(`proc_status=O`)→배송시작(`D`) → (현장)**분할/과소 입고**(`proc_status=I`, 수량 무판정 확인) → 재고 현황 수량↑ + `InventoryHistory` IN(docNo=STK, refModule=PUR/refNo=PR번호)·**입고증 출력** → **입고 취소** 시도(후속 tx 없을 때만 성공, 있으면 차단) → 미입고분 남긴 채 **종료(`proc_status=E`)** + **0 입고 PR 독립 종료** 확인. **단가 미입력**도 수량만 잡히는지 확인.
- **모듈 코드 통일 회귀**: 기존 `APPROVAL`/`STOCK`/`EQUIPMENT`/`INVENTORY`/`BOARD` 권한 매트릭스가 `APR`/`STK`/`EQP`/`INV`/`BRD`로 정상 동작(role_detail UPDATE 후 기존 회사 권한 유지).
- **테넌트 격리 회귀**: 타 회사 비노출, 기존 입출고/월마감 정상.

---

## 우선순위 MEDIUM — 운영 안정성

### [ ] C1-sub. updateUser() 자가 roleId/useYn 변경 차단 미적용

- **파일**: `backend/src/main/java/com/cmms/service/MdmService.java` — `updateUser()`
- **현상**: `operator == id`(본인이 본인 수정) 분기 없이 `roleId`·`useYn`을 무검증 갱신. 일반 USER가 `PUT /api/mdm/users/{본인id}`로 `roleId: "ADMIN"` 자가 승격 가능
- **조치**: operator와 id가 같을 경우 `roleId`·`useYn` 변경 불가 처리 추가
- **비고**: 컨트롤러 `@PreAuthorize` 적용(C1 완료)으로 ADMIN 이상만 호출 가능하나, ADMIN 본인이 SYSTEM으로 승격하는 케이스는 여전히 가능 (→ C5 참조: 회사에 SYSTEM 역할이 있으면 단순 사내 상승이 아니라 **교차 테넌트 탈취**로 이어짐)

### [~] C5. 회사별 SYSTEM 역할 생성 = 교차 테넌트 탈취 (보안)

- **파일**: `backend/src/main/java/com/cmms/service/AuthService.java` — `setupNewCompany()`
- **현상**: 신규 회사마다 SYSTEM 역할까지 생성 → 사내 ADMIN이 `updateUser`로 사용자 `roleId`를 SYSTEM으로 올리면, `PermissionChecker` 매트릭스 우회 + `hasRole('SYSTEM')`로 열리는 Company API(`getAllCompanies`가 전 테넌트 반환)를 통해 **타 회사 데이터 조회/생성/삭제** 가능
- **조치**:
  - [x] `setupNewCompany` 표준 롤에서 SYSTEM 제외(ADMIN/MANAGER/USER만) — 코드수정 완료(커밋 보류)
  - [ ] 기존에 이미 생성된 회사별 SYSTEM 역할 정리(마이그레이션 — 시드 SYSTEM 테넌트는 제외)
  - [ ] 롤 배정 시 SYSTEM 할당 차단(C1-sub와 함께 처리)
- **비고**: SYSTEM 역할은 시드 SYSTEM 테넌트(sysadmin) 전용. 일반 회사엔 두지 않는다

### [ ] M3. 전역 findAll() 풀스캔 — 멀티테넌트 성능 저하

- **파일**: 아래 13곳
  - `ApprovalService.java` : 115, 129, 215, 224, 233, 246, 258, 267, 278, 287, 296 (11곳)
  - `PmService.java` : 37 (1곳)
  - `MdmService.java` : 50, 242 (2곳)
- **현상**: `findAll().stream().filter(companyId)` 패턴으로 전체 테넌트 데이터를 메모리 적재 후 필터. 테넌트·데이터 증가 시 치명적 성능 저하
- **조치**: 각 Repository에 `findByCompanyId...()` 쿼리 메서드 추가 후 교체

### [ ] M4. 재고 마감 산식 오류 — 월말 잔액 부정확

- **파일**: `backend/src/main/java/com/cmms/service/InventoryTransactionService.java` — `closeMonth()` (397-398번 줄)
- **현상**: 마감수량을 `status.getQty()`(호출 시점 현재 재고)로 설정. 과거 월을 나중에 마감하면 이후 거래가 반영된 현재 재고가 그 달 마감값으로 기록됨 → 월말 잔액 부정확
- **조치**: 마감수량을 해당 `closingYm`의 `InventoryHistory` 레코드 집계(입고-출고+이동+조정)로 계산하도록 수정

### [~] 코드 상수화 (매직 스트링 → enum) + 정본 코드표

- **정본**: `docs/db_specification.md §5` (상태·유형 코드 단일 소스)
- **Phase 1 (완료, 동작 무변경)**:
  - [x] enum 4종 생성 `com.cmms.constant`: `SeqModule`, `RoleType`, `DocStatus`, `ApprovalStepType`
  - [x] ApprovalService 외부 리터럴 치환 — 채번모듈(WO/WP/PM 호출부), `PermissionChecker`의 SYSTEM, `PmService`의 status
  - [x] `db_specification §5` 정본 코드표 작성 + 기존 불일치 주석(approval_result `D/A/R/W`) 정정
- **Phase 2 (코드 구현 완료, ⚠️ 런타임 테스트 보류 — 동작 변경)**:
  - [x] `approval_result`를 빈칸(대기)/`Y`(승인)/`N`(반려)로 전환 — `submitApproval`/`getPendingApprovals`/`processApprovalAction` 로직 재작성("현재 차례"를 저장 대신 계산), 엔티티 `nullable`, `V4` 마이그레이션
  - [x] ApprovalService 내부 `DocStatus`/`ApprovalStepType`/`SeqModule` 리터럴 치환
  - [x] 결재자 없이 상신 → 임시저장(`TEMP`), 라우팅 안 함 / 상신함은 임시저장도 조회됨
  - [x] 재상신: 기존 임시저장 문서 id로 상신 시 단계 재생성 후 라우팅
  - [ ] **런타임 테스트**(다단계 진행·반려 후 비노출·완결 전파·V4 데이터 변환·임시저장/재상신) — 추후 함께
  - [ ] (FE) 상신함에서 임시저장 표시 + 편집/재상신 UI 연동 확인

---

## 참고 — 검토 완료·보류 항목

| 항목 | 상태 | 비고 |
|------|------|------|
| C2 hasRole 접두사 버그 | ✅ 완료 | PermissionChecker + hasRole('SYSTEM') |
| C1 전 엔드포인트 @PreAuthorize | ✅ 완료 | AppModule enum, 100% 커버리지 |
| C1 PM status="S" 결재 우회 | ✅ 완료 | checkSave() A권한 검증 |
| M2 프론트 세션 영속화 | ✅ 완료 | zustand persist + 401 인터셉터 |
| M5 전역 예외처리 | ✅ 완료 | GlobalExceptionHandler |
| L2 민감정보 환경변수화 | ✅ 완료 | application.yml 자리표시자화 |
| CORS 설정 | ✅ 보류 | Bearer 헤더 기반, 실효위험 낮음 판단. 쿠키 기반 인증 전환 시 즉시 재검토 필요 |
