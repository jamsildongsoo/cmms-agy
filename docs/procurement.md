# 재고 관리 강화: 구매요청 → 발주/배송 → 입고(히스토리) → 플랜트별 재고 가시화

> ⚠️ **임시 작업 문서 (구현 기간 한정)** — 본 문서는 재고 관리 강화(구매요청·입고·플랜트 스코핑) 기능의 종합 설계서로, **구현 단계 동안만 유지**된다. 구현 완료 후 폐기 예정이며, 내용은 이미 `docs/product_requirements.md`(요구사항) · `docs/db_specification.md`(스키마) · `docs/ui_specification.md`(화면) · `docs/todo.md`(실행 과제 P1~P7)로 흡수돼 있다. **신규 작업 시 4개 문서를 우선 참조**, 본 문서는 구현 컨텍스트가 필요할 때만 참조.

## Context (배경)

현재 재고 모듈은 회사 단위로만 동작하고(`Inventory`/`Warehouse`/`InventoryStatus` 모두 플랜트 차원 없음), 자재 유입 기록 진입점이 입출고 화면 직접 입력뿐이며, 거래에 **업무 문서번호가 없어 이력 추적이 어렵다.** 사용자는 **현장 담당자가 구매할 자재를 전산에 요청 → (견적·입찰·선정·발주는 오프라인 수작업) → 구매자가 발주/배송 절차 상태 기록 → 물건이 현장 도착 시 입고(재고 반영) → 사업장(플랜트)별/총괄 재고 현황과 문서번호별 입고/출고 이력을 조회**하는 흐름을 원한다.

핵심 제약: **풀 워크플로우(승인/견적/입찰/선정/발주서/회계)는 전산화하지 않는다.** 전산이 책임지는 것은 ① 구매요청 기록·출력(수기결재), ② 구매자 발주/배송 절차상태·벤더·예정도착일 기록, ③ 입고를 기존 재고 히스토리에 반영, ④ 플랜트별/총괄 재고 현황 + 문서번호 기반 입고/출고 이력뿐이다.

플랜트 스코핑은 **창고를 플랜트의 하위**로 두어 달성한다. **사업장 = 플랜트(동일 단위, 신규 조직 엔티티 없음)**, 현황은 플랜트→저장소로 브레이크다운. 일반 담당자는 지정 플랜트에 고정, **멀티 권한 역할(`Role.multiPlant='Y'`)** 사용자만 플랜트 전환/전체 총괄. **기존 휴면 스캐폴딩 재사용** — 지정 플랜트=`User.lastLoginPlantId`(유저 단위), 멀티 여부=`Role.multiPlant`(역할 단위). 신규 `User` 컬럼 없음.

## 행위자 · 업무 흐름 · 상태

**행위자(별도 역할 신설 없이 `PUR` 권한 + 상태 + 플랜트 스코프로 게이팅):**
- **현장 담당자**: 구매요청 작성·확정(출력→수기결재), 입고(현장 도착분), 구매요청 종료.
- **구매자**: 발주(벤더·발주일·예정도착일) → 배송시작. 견적·선정·발주는 오프라인, 시스템은 기록만.

**구매요청 = 그 자체로 종결되는 문서(결재 비연계).** 문서 상태는 기존 문서들과 동일하게 `status`(CHAR(1)) 컬럼 + **기존 `DocStatus` enum 재사용**, 절차 상태는 별도 `proc_status`(CHAR(1)) 컬럼. **신규 enum/분기 최소화** — 절차 코드도 `DocStatus`에 합쳐 **단일 enum**으로 관리(`DocStatus`에 없는 글자 사용 → 충돌 없음).

- **`PurchaseRequest.status`** (문서; 컬럼명·타입·전이로직 모두 WO/WP/PM과 동일): `T`(저장/작성) → `S`(확정; 현장; **결재 우회 직접확정** = 기존 `DocStatus.SELF_CONFIRMED`). **PR은 결재 비연계라 `P`/`C`/`R`/`X` 모두 미사용** — `T` 폐기는 `deleteYn` 소프트 삭제, `S` 이후 폐기는 절차 종료(`E`). 저장/확정은 **기존 `PermissionChecker.checkSave('PUR', status)` 그대로** — 확정(`S`)은 C+A 권한. **확정(`S`) 이후 요청 내용(자재/수량/저장소) 불변.**
- **`PurchaseRequest.procStatus`** (절차, 확정 이후 별도 변경; `proc_status` 컬럼): `(null)` → `O`(발주; 구매자) → `D`(배송시작; 구매자) → `I`(입고; 현장, 1회 이상 입고되면 진입) → `E`(종료; 현장). 코드는 `DocStatus`에 추가(**O**rder/**D**elivery/**I**ncoming/**E**nd, 기존 T/P/C/S/R/X와 비충돌). **완료 여부를 수량으로 판정하지 않는다** — 입고가 있으면 `I`, 현장이 명시적으로 닫으면 `E`. 과소/초과 잔여는 라인 수량에만 표시.

**입고 = 기존 히스토리 기반(별도 입고 테이블 없음):** 현장이 PR 기준으로 입고(구매요청 화면 내 입고 모달) → `InventoryTransactionService`로 `InventoryHistory` IN 기록 + 재고 반영 + PR 라인 `receivedQty` 누적 + `proc_status=I`. 각 입고 이벤트엔 **고유 전표번호(`docNo`, 단일 STK 채번)**, 출처는 `refModule='PUR'`/`refNo=PR번호`로 PR에 연결. **과소/초과·분할(여러 번) 입고 가능** — 분할 입고는 매번 새 전표번호(동일 PR로 누적). 단가 미입력 허용(amount=0). **완료 여부를 수량으로 판정하지 않는다** — 입고 모달의 '종료' 체크 시 입고 후 곧바로 `E`(종료), 아니면 `I`(입고) 유지. 입고가 한 번도 없는 PR도 닫을 수 있도록 **목록의 독립 '종료' 액션**을 별도 제공(잔여는 종료로 닫음, 라인 취소수량 불필요).

**번호 체계(중요):** 입고/출고는 `txTypeCode`(IN/OUT/MOVE/ADJ)로 이미 구분되므로 **전표번호는 GR/GI로 나누지 않고 단일 체계(`STK`)**, 방향은 `txTypeCode`로 판별.
- ① `historyNo`(BIGSERIAL, 시스템 행 PK — 업무번호 아님, 노출 안 함)
- ② **`docNo`(전표번호, STK)** — 입고/출고/이동/조정 **이벤트(배치)마다 채번**. 전표 내 라인은 `docNo` 그룹 내 **표시 순번(그룹번호)** 으로 노출.
- ③ `refNo`/`refModule`(출처 링크 — 구매입고=`PUR`+PR번호; 이동=기존 페어링).

**입고/출고 이력·증빙:** 히스토리를 **전표번호(`docNo`)별로 그룹·검색**. 입고증/출고증 = 전표(docNo) 단위 출력(방향은 txTypeCode). PR 단위 입고내역은 `refNo=PR번호`로 필터.

**구매오더(합본) 문서·입고 문서 테이블 모두 두지 않음** — PR(=단일 창고)이 입고 단위라 "본인 창고 요청분" 자동 성립. 다건 일괄 발주는 목록 다중선택 상태변경(UI).

## 범위

**포함**: 벤더 마스터 신설 · 구매요청(현장, 자재+수량, 입고 저장소 지정, 저장/확정, **구매요청서 출력(수기결재칸)**) · 구매자 발주/배송 절차상태(벤더·발주일·예정도착일) · **입고(히스토리 기반, 부분/과소, 입고증 출력)** · 구매요청 종료 · **단가 미입력 허용**(수량만, amount=0) · **모든 IN/OUT/MOVE/ADJ에 전표번호(docNo) → 문서번호 기반 이력 + 출고증 출력** · 플랜트 스코핑(창고 plant_id, 유저 지정 플랜트, 역할 멀티) · 현황 **플랜트→저장소 + 내 저장소/전체 토글** · 인쇄 수기결재칸 공통(`PrintSignBox`).

**제외(수작업/별도)**: 결재·승인(전산)·견적·입찰·선정·발주서(PO)·회계 / **메일 발송(별도 처리, 이번 범위 밖 — 백엔드 SMTP 없음 확인)** / 자재 마스터(`Inventory`)는 회사 공용 유지(플랜트 차원 추가 안 함) / 별도 입고 문서 테이블(히스토리로 대체).

## 결정 사항

1. **사업장 = 플랜트(동일)**. 신규 조직 엔티티 없음. 현황 플랜트→저장소.
2. **멀티 = 역할 단위**(`Role.multiPlant`, **기존 필드 재사용 — 신규 User 컬럼 없음**). 역할 관리에서 멀티 토글, 사용자 관리에선 [지정 플랜트](`lastLoginPlantId`). 총괄담당자=멀티 역할 부여. ADMIN 역할 `multiPlant='Y'`.
3. **입고 저장소 = 요청 헤더 1개, 불변**(`PurchaseRequest.warehouseId`).
4. **현황 '내 저장소' = 내 플랜트 창고+공통부문**, '전체' = 전 플랜트(멀티 역할만). **지정 플랜트(`lastLoginPlantId`)가 null이면 전체로 동작(허용·데이터 미은닉)**. 로그인 시 null이면 **기본 플랜트(회사의 첫 활성 플랜트)로 자동 매핑·기록 → 다음 로그인부터 그 값 사용**; 관리자가 사용자 정보에서 지정한 값이 있으면 그 값 우선(자동 매핑 건너뜀). **플랜트 변경은 멀티 역할 사용자만**(셀렉터), 비멀티는 관리자만 변경.
5. **저장소 = '플랜트+저장소' 조합** — 창고 `plant_id` 속성, 선택·표시는 (플랜트→저장소). **창고 PK 재설계 안 함**(`(companyId,id)`), 공통부문=`plant_id` null. ※ 플랜트별 저장소 코드 중복 허용 필요 시만 PK 재설계 별도 협의.
6. **상태 = 단일 `DocStatus` enum, 컬럼 2개(최소 상수화·분기)**: 문서 `status`(CHAR(1), `DocStatus` 재사용 — PR은 결재 비연계라 `T`저장/`S`확정(직접확정)만 사용, **`X`/`P`/`C`/`R` 미사용** — `T` 폐기=soft delete, `S` 이후 폐기=절차 종료 `E`), 절차 `proc_status`(CHAR(1), `DocStatus`에 `O`발주/`D`배송/`I`입고/`E`종료 **추가** — 기존 T/P/C/S/R/X와 비충돌). 저장/확정은 기존 `checkSave('PUR', status)` 그대로(확정 `S`=C+A). **절차상태는 수량 무판정** — 입고 있으면 `I`, 명시 종료 시 `E`.
7. **입고/출고 = 히스토리 기반(GoodsReceipt 테이블 없음)**. 입고 시 `proc_status=I`(수량 무관). 종료 = PR `proc_status=E` — 입고 모달 '종료' 체크 또는 목록 **독립 종료 액션**(0 입고 PR도 종료 가능).
8. **전표번호 docNo(단일 STK) 신설** — `inventory_history`에 `doc_no` 컬럼 추가, 이벤트마다 `SequenceService` 채번. **GR/GI 구분 없음**(입고/출고는 `txTypeCode`). 라인은 docNo 그룹 내 표시 순번, `historyNo`(전역 PK)는 비노출. 이력 화면 `docNo` 검색·그룹, PR 단위 `refNo` 필터.
9. **출력 수기결재칸 = `PrintSignBox` 신설** — 기존 `Approval` 결재박스 형식 차용(칸당 ~96px), **2열×4행(8칸)·라벨 없는 빈 수기 서명칸**. 구매요청서/입고증/출고증 공통. **`PrintHeader`는 회사코드→회사명 표시로 변경**(인증/스토어에 `companyName` 추가; 기존 사용처 Inventory/Equipment 자동 반영). **기존 자체 헤더(WO/WP/PM/Approval/InventoryTransaction 목록)는 이번 범위 외 — 기존 유지**(전사 헤더 통일은 별도 작업).
10. **권한 모듈 상수화 + 2축(모듈×액션)**: `AppModule`에 `PUR` 추가(단일 소스 — `@perm.check` + role_detail 시드 공유). 권한은 **모듈 × 액션(C/R/U/D/A)** 2축(`role_detail`). 매핑은 아래 [권한 매핑] 표 참조. 핵심: **재고 반영 처리(구매 입고 포함, 입출고/이동/조정/마감)=`STK` C, 현황·이력 조회=`STK` R**로 조회/처리가 액션으로 구분됨. 채번도 `AppModule`을 그대로 사용(별도 SeqModule 없음). **구매오더(PO) 문서 없음.**
11. **입고 처리 = 구매요청 화면(`Procurement.tsx`) 내 모달(PR 기준)**. 입고 모달은 구매 화면에 있으나 **입고 endpoint 권한은 `STK` C**(재고 반영이므로 수동 입출고와 동일 통제), PR 조회는 `PUR` R + 플랜트 스코프. 기존 `InventoryTransaction.tsx`는 PR 없는 수동 입출고/이동/조정 + 현황/이력 전용. 둘 다 같은 `InventoryHistory` 원장에 STK 전표로 기록.
12. **구매요청유형 = 공통코드(CodeGroup/CodeItem) 재사용** — 코드그룹 `PR_TYPE`을 `company_id='SYSTEM'`(전 회사 공유, 기존 `*_TYPE`과 동일)로 시드. 아이템(정렬순): `NORMAL`일반(10)/`ROUTINE`경상(20)/`PLANNED_PM`계획예방정비(30)/`URGENT`긴급(40)/`ETC`기타(99). `PurchaseRequest.requestType`가 코드아이템 `id` 참조(기존 `Inventory.itemTypeCode` 패턴). 관리=기준정보 설정>공통코드(`CodeManager`). V4 1회 `INSERT`(신규 회사 추가 시드 불필요).
13. **메뉴 배치**: '업무 트랜잭션' 그룹에 단일 '구매' 항목, `Procurement.tsx` 내부 탭으로 **구매요청 / 벤더 관리** 구성(벤더는 업무 성격이라 기준정보 설정 아님). 기준정보 설정(MDM)엔 벤더 없음.

## 권한 매핑 (요약)

권한은 **모듈(`AppModule`) × 액션(`C/R/U/D/A`)** 2축. `role_detail`에 모듈별 `permC/permR/permU/permD/permA`, 호출부는 `@perm.check('모듈','액션')`. 조회(R)와 처리(C)가 액션으로 구분되므로 "조회 전용 역할"과 "처리 가능 역할"을 분리할 수 있다.

| 기능 | 모듈 | 액션 |
|---|---|---|
| 재고 **현황/이력 조회** | `STK` | **R** |
| **입고·출고·이동·조정·마감 처리**(재고 반영, 구매 입고 포함) | `STK` | **C** |
| 구매요청 작성/수정/확정/조회 | `PUR` | C / U / R |
| 발주/배송/종료(절차상태 전이) | `PUR` | **U** (확정) |
| 벤더 마스터 | `PUR` | C/R/U/D |
| 자재 마스터(품목) | `INVENTORY` | C/R/U/D |
| 창고·플랜트·부서·역할·사용자·공통코드 | `MDM` | C/R/U/D |

- **입고/출고는 둘 다 '처리'(`STK` C)로 동일 취급** — 입고만/출고만 별도 통제는 현재 C/R/U/D/A 모델로는 불가(필요 시 별도 설계).
- 예시 역할 구성: 현장 입고담당 = `PUR` R(PR 조회) + `STK` C(입고) + `STK` R(현황); 구매자 = `PUR` C/U/R; 조회 전용 = `STK` R만.
- **구매오더(PO) 문서 없음** — 발주는 오프라인, 시스템은 PR 절차상태/벤더/발주일만 기록.

## 데이터 모델 변경

신규/변경 엔티티는 기존 패턴(`BaseEntity` 상속, `@IdClass` 복합키, `(companyId, ...)`)을 따른다.

- **`Warehouse`** (`model/Warehouse.java`): `plantId`(`plant_id`, len 50, **nullable**=공통부문) 추가. PK 불변.
- **`User`** (`model/User.java`): **컬럼 추가 없음** — `lastLoginPlantId`를 지정 플랜트로 재사용.
- **`Role`** (`model/Role.java`): 기존 `multiPlant`(`multi_plant`) 필드 **활성화**(스키마 변경 없음).
- **`InventoryHistory`** (`model/InventoryHistory.java`): **`docNo`(`doc_no`, len 50) 컬럼 추가**(전표번호). `refNo`/`refModule`은 출처 링크 유지. 모든 IN/OUT/MOVE/ADJ에 `docNo`+(있으면)`refNo` 세팅.
- **신규 `Vendor`** + `VendorId(companyId, id)`: `name`(NOT NULL), `bizNo`, `contact`, `manager`, `remarks`.
- **신규 `PurchaseRequest`** + `PurchaseRequestId(companyId, id)`: `id`=채번(`PUR-yyyyMM-####`), `plantId`(NOT NULL), `warehouseId`, `requesterId`, `requestDate`, `requestType`(공통코드 `PR_TYPE` 참조), `vendorId`(nullable), `orderDate`, `etaDate`(예정도착일), `shipStartDate`, `status`(CHAR(1), `DocStatus`; 기본 `T`), `procStatus`(`proc_status` CHAR(1), nullable), `remarks`.
- **신규 `PurchaseRequestItem`** + `PurchaseRequestItemId(companyId, requestId, lineNo)`: `inventoryId`, `qty`(불변), `unit`, `receivedQty`(0; 입고 시 누적), `remarks`.
- **`AppModule`**: 신규 `PUR`/`STK` 추가 + 기존 `APPROVAL`/`STOCK`/`EQUIPMENT`/`INVENTORY`/`BOARD` → **`APR`/`STK`/`EQP`/`INV`/`BRD`로 단축**(전 모듈 ≤3자) + **`label()` 한글 라벨** 메서드 추가(FE 라벨맵 대체). **채번도 같은 enum 사용**(`SeqModule` 제거됨). **`DocStatus`**: 절차상태 `O`(ORDERED)/`D`(SHIPPING)/`I`(RECEIVED)/`E`(CLOSED) 추가(문서상태 T/S는 재사용 — 신규 enum 없음).

## DB 마이그레이션 — `backend/src/main/resources/db/migration/V4__procurement.sql`

스키마 단일 소스는 Flyway(`ddl-auto=none`).
- `ALTER TABLE warehouse ADD COLUMN plant_id varchar(50);`
- `ALTER TABLE inventory_history ADD COLUMN doc_no varchar(50);`
- `CREATE TABLE vendor / purchase_request / purchase_request_item` — 복합 PK + BaseEntity 컬럼.
- **기존 회사 권한 시드**: 모든 기존 `role_detail`에 `PUR` 행 삽입(ADMIN=전권, MANAGER/USER=CRUD 등 `createDefaultRoleDetails` 매트릭스 동일). V2 데모 회사 포함.
- `UPDATE role SET multi_plant='Y' WHERE id='ADMIN';` (멀티는 역할 단위 — 신규 `users` 컬럼 없음)
- **공통코드 시드**: `PR_TYPE` 코드그룹 + 아이템(`NORMAL`일반10/`ROUTINE`경상20/`PLANNED_PM`계획예방정비30/`URGENT`긴급40/`ETC`기타99)을 **`company_id='SYSTEM'`** 로 `INSERT`(기존 `EQ_TYPE`/`WO_TYPE` 등과 동일 — 전 회사 공유, 신규 회사 추가 시드 불필요).

## Backend 변경

- **모델/IdClass/Repository**: 신규 엔티티 3종 + `XxxId` + `JpaRepository`(회사/플랜트/상태/PR/docNo 필터). 기존 `InventoryStatusRepository`/`WarehouseRepository`/`UserRepository`/`RoleRepository`/`InventoryHistoryRepository` 재사용.
- **DTO**: `dto/VendorDto.java`, `dto/PurchaseRequestDto.java`(`SaveRequest{header,List<ItemLine>}`, `OrderRequest{requestId,vendorId,orderDate,etaDate}`, `ReceiveRequest{requestId,close(bool),List<ReceiveLine>{lineNo,qty,unitPrice}}`, 절차상태 전이).
- **`VendorService`+`VendorController`** (`/api/vendors`, `@perm.check('PUR',...)`): 마스터 CRUD 패턴 복제(벤더는 구매 업무 도메인).
- **`ProcurementService`+`ProcurementController`** (`/api/procurement`):
  - `createRequest`(저장 `status=T`)/`confirmRequest`(확정 `status=S`, 현장; `checkSave('PUR','S')` → C+A): `SequenceService.generateNextNo(companyId, AppModule.PUR.name(), requester.getDepartmentId())`(부서 없으면 fallback `DEPT_ROOT`); plantId 컨텍스트 주입(클라이언트 입력 금지).
  - `placeOrder`(→`proc_status=O`)/`startShipping`(→`proc_status=D`)(구매자): 벤더·발주일·예정도착일·배송 기록.
  - `receive`(현장, `@perm.check('STOCK','C')` — 재고 반영): STK 전표번호 채번 → 라인별 `InventoryTransactionService.processTransactions()`에 `TxItem{txTypeCode:"IN", warehouseId(=PR), inventoryId, qty, unitPrice, docNo, refModule:"PUR", refNo:requestId}` 위임 → PR 라인 `receivedQty` 누적 → `proc_status=I`. **수량 과소/초과 판정 없음**. `ReceiveRequest.close=true`면 입고 후 곧바로 `proc_status=E`. (그 외 `ProcurementController` 엔드포인트는 `PUR` 권한.)
  - `closeRequest`(→`proc_status=E`, 현장; `@perm.check('PUR','U')`): 잔여 닫고 종료. **독립 액션** — 입고가 한 번도 없는(미입고) PR도 호출 가능, 입고 모달의 `close` 플래그와 동일 경로.
  - `cancelReceive(docNo)`(현장, `@perm.check('STK','C')` — 재고 반영): **입고 역분개**. 해당 전표의 IN을 같은 `docNo`로 OUT 역분개 + PR 라인 `receivedQty` 차감. **조건: 그 입고 이후 동일 `(company, warehouse, inventory)` history에 어떤 transaction(IN/OUT/MOVE/ADJ)도 없을 때만 허용**(`InventoryHistoryRepository`에 후속-tx 존재 체크). 위반 시 명시적 에러("후속 거래가 있어 취소 불가").
  - `getRequests`: 플랜트 스코프 적용(일반=본인 플랜트 고정, 멀티=선택/전체). **입고 대상 조회는 추가로 발주/배송 이후·미종료(`proc_status != E`) 필터**(수량 무판정) — 타 플랜트 PR은 서버에서 차단(클라이언트가 PR id를 보내도 스코프 검증).
- **전표번호(docNo) 적재**: `InventoryTxDto.TxItem`/요청에 optional `docNo`/`refNo`/`refModule` 추가, **이벤트마다 `SequenceService.generateNextNo(companyId,'STK',...)` 채번**(입고/출고/이동/조정 공통, GR/GI 구분 없음) → `executeCheckIn/Out/Adjustment/Transfer`(`service/InventoryTransactionService.java`)가 history `docNo`(+출처 `refNo`)에 세팅. `unitPrice` null→0(기존 지원).
- **이력 조회**: `/inventory-tx/history`에 `docNo` 검색 + 전표번호별 그룹(라인 표시 순번).
- **`MdmService`** (`service/MdmService.java`): `saveUser`/`updateUser`(L212/227)에 `lastLoginPlantId`(관리자 지정 — optional, 지정 시 로그인 자동 매핑보다 우선); `saveWarehouse`/`updateWarehouse`(L262/275)에 `plantId`; **역할 저장/수정에 `multiPlant` 반영**(역할 관리).
- **`CompanyService.seedRoles`**(`service/CompanyService.java:121`): ADMIN 역할 `multiPlant("Y")`(현재 전부 'N'). `createDefaultRoleDetails`(L138)는 `AppModule.values()` 순회라 `PUR` 자동 포함. (PR_TYPE는 SYSTEM 공유 코드라 회사별 시드 불필요.)
- **`ApprovalService.java:55`**: `SequenceService.generateNextNo(... , "DEPT_ROOT")` → `drafter.getDepartmentId()` 사용으로 변경(전 모듈 단일 채번 포맷 일관성).
- **문서번호 포맷 변경** (`SequenceService.java:42`): `String.format("%s-%s-%04d", ...)` → `"%s-%s-%s-%04d"`로 변경, `departmentId` 세그먼트 포함 → 출력 `{모듈}-{부서}-{년월}-{순번}`. WO/WP/PM 호출부 무변경(이미 dept 전달).
- **`AppModule` rename + label**: `APPROVAL`→`APR`, `STOCK`→`STK`, `EQUIPMENT`→`EQP`, `INVENTORY`→`INV`, `BOARD`→`BRD`; 신규 `PUR`. `label()` 메서드 추가(한글 라벨). 콜사이트(`@PreAuthorize` 26곳) 일괄 치환 + V4에 `UPDATE role_detail SET module_detail=...` 5건.
- **인증 응답**: `AuthDto.LoginResponse`+`AuthService.login`(`service/AuthService.java:47`)에 (해소된) `lastLoginPlantId` + (유저 역할에서 resolve한) `multiPlant` + `companyName`(Company 조회, PrintHeader 표시용) 추가. 아래 '로그인 시 플랜트 해소'를 최종 로그인 갱신 블록(`AuthService.java:80~85`)에서 수행 후 그 값을 응답에 담는다.
- **플랜트 스코프 리졸버**(헬퍼): `companyId,userId`→`{multiPlant(=역할), assignedPlantId(=lastLoginPlantId)}`를 `UserRepository`+`RoleRepository`로 조회. JWT/`UserPrincipal` 불변.
- **현황 플랜트 필터**: 대상 창고 = `warehouse.plantId ∈ {유저플랜트, null(공통)}` → status 필터. '전체'(멀티 역할)는 전 플랜트.

### 플랜트 스코프 규칙 (서버 강제)
- 멀티 아님: 클라이언트 plantId 무시, **항상 `lastLoginPlantId` 고정**, 타 플랜트 차단. **`lastLoginPlantId`가 null이면 필터 없음(전체 조회) — 허용**(데이터 미은닉). 단 PR 생성은 주입할 plantId가 없으므로 플랜트가 해소된 뒤 가능.
- 멀티(`Role.multiPlant='Y'`): 요청 plantId 사용 또는 '전체'(필터 없음=합산).
- **로그인 시 플랜트 해소**(`AuthService.login`): `lastLoginPlantId`가 null이면 회사의 첫 활성 플랜트(`plant.deleteYn='N'`, id 정렬)를 골라 `lastLoginPlantId`에 기록·저장(L80~85 최종 로그인 갱신 블록) → 다음 로그인부터 재사용. 플랜트가 0개면 null 유지(전체). 관리자가 지정해 둔 값이 있으면 건드리지 않음(지정값 우선).

## Frontend 변경

- **신규 `components/PrintSignBox.tsx`**: 빈 수기 결재칸(Approval 박스 형식 차용, 칸당 ~96px) — **2열×4행 고정, 라벨 없는 빈 8칸**. `PrintHeader`와 조합.
- **`components/PrintHeader.tsx` 변경**: 표시 회사값 `companyId`→`companyName`(현재 `PrintHeader.tsx:24`가 코드 출력 중). 값은 `useAuthStore` `user.companyName`. **기존 사용처(Inventory/Equipment) 공통 반영**, 그 외 자체 헤더 페이지는 불변.
- **활성 플랜트 컨텍스트**: `store/useAuthStore.ts` — `User`에 `companyName`(PrintHeader용), `lastLoginPlantId`,`multiPlant`(역할에서 resolve) 노출 + `activePlantId` 상태/setter(로그인 시 `lastLoginPlantId`로 초기화, **변경은 멀티만**). API에 plantId 전달(서버 최종 강제).
- **`components/Header.tsx`**: `multiPlant==='Y'`만 플랜트 셀렉터([플랜트 ▼ | 전체총괄]), 일반 유저는 플랜트 라벨(읽기전용; `lastLoginPlantId`가 null이면 '전체').
- **MDM (`pages/MdmLayout.tsx`)**: 창고 폼에 플랜트 선택(공통부문 포함); 사용자 폼에 **지정 플랜트**(멀티 토글 없음); **`RoleManager`에 `multiPlant` 토글**(기존 Role 인터페이스에 이미 필드 존재). 구매요청유형은 기존 `CodeManager`(공통코드)로 관리. **벤더는 MDM 아님** — 구매 화면으로.
- **신규 `pages/Procurement.tsx`** (내부 탭 **구매요청 / 벤더 관리**, `Approval.tsx`/`MdmLayout.tsx` 탭 패턴):
  - ▸**구매요청**: 목록(플랜트 스코프 — 일반=본인 플랜트만) + 상태기반 액션 — 신규/수정(자재+수량, **구매요청유형 선택**, 플랜트→저장소 조합)·확정·**구매요청서 출력**(`PrintHeader`+`PrintSignBox`)[현장], **발주·배송 모달**(벤더·발주일·예정도착일)[구매자], **입고 모달**(입고 대상 PR=발주/배송 이후·미종료(`proc_status != E`), 본인 플랜트 한정; 라인별 **요청수량/기입고수량/잔여** 표시 + **입고수량 입력란 기본값=잔여 프리필, 편집 가능(부분/과소·초과; 초과 시 경고만)**, 단가 미입력 허용 + **'이 요청 종료' 체크박스**)·**입고증 출력**·**독립 '종료' 액션**[현장]. 문서/절차 상태 뱃지 분리.
  - ▸**벤더 관리**: 거래처 CRUD(`PlantManager` 패턴).
- **`pages/InventoryTransaction.tsx`**: 현황 **플랜트→저장소** 브레이크다운 + [내 저장소/전체] 토글 + 활성 플랜트 필터, 금액 0/미입력 graceful. 이력 뷰 **전표번호(docNo) 검색·그룹**. **입고증/출고증 = 기존 전표(Slip) 인쇄 모달(`InventoryTransaction.tsx:640~750`)을 docNo 단위로 확장** — 제목 `txTypeCode` 분기(IN→입고증/OUT→출고증/MOVE·ADJ→전표), 본문 헤더(전표번호·거래일자·창고·구매입고면 `refModule=PUR`/`refNo=PR번호`) + 라인표(표시순번|품목|수량|단위|단가|금액, 단가 0 graceful)+합계 + `PrintHeader`(회사명)+`PrintSignBox`(2×4). **목록 헤더는 기존 유지**. 수동 입출고는 STK 전표 채번.
- **`pages/Dashboard.tsx`** switch(`case 'procurement'`) + **`components/Sidebar.tsx`** '업무 트랜잭션' 그룹에 `procurement`(구매) 단일 항목 추가. 벤더는 별도 메뉴 없이 `Procurement.tsx` 내부 '벤더 관리' 탭.

## 검증 (Verification)

1. **빌드/기동**: `cd backend && ./gradlew build`(Flyway V4), `cd frontend && npm run build`. 통합은 8082 포트.
2. **마이그레이션**: V4 적용 + role_detail PUR 행 + `warehouse.plant_id`/`inventory_history.doc_no`/신규 테이블 + `role.multi_plant`(ADMIN='Y') 확인.
3. **플랜트 스코핑(핵심 회귀)**: 일반 역할(plantA, multi=N)은 셀렉터 없음·plantA만, plantB 강제 전송해도 서버가 plantA 고정. 멀티 역할은 plantA/plantB/전체 전환·합산. **`lastLoginPlantId` null 유저**: 첫 로그인 시 기본 플랜트 자동 매핑·기록 → 재로그인 시 그 값 재사용; 플랜트 0개면 null(전체 조회).
4. **엔드투엔드**: 벤더 등록 → (현장)구매요청 작성(자재+수량, 플랜트→저장소)·확정·**구매요청서 출력(수기결재칸)** → (구매자)발주(벤더·발주일·예정도착일)→배송시작(절차상태 분리 확인) → (현장)**분할/과소 입고**(입고 시 `proc_status=I`, 수량 무판정 확인) → 재고 현황 수량↑ + `InventoryHistory` IN(docNo=STK, refModule=PUR/refNo=PR번호)·**입고증 출력** → **입고 취소(역분개)** 시도(후속 tx 없으면 성공·재고/`receivedQty` 롤백, 있으면 명시 에러로 차단) → 미입고분 남긴 채 **종료(`proc_status=E`)** + **0 입고 PR 독립 종료** 확인. **단가 미입력**도 수량만 잡히는지 확인.
5. **번호 포맷 회귀(핵심)**: WO/WP/PM/APR/PUR 새 번호 모두 `{모듈}-{부서}-{년월}-{순번}` 포맷 확인 + **같은 회사·플랜트의 다른 부서 사용자가 같은 달 동시 생성** 시 PK 충돌 없음.
6. **모듈 코드 통일 회귀**: `APPROVAL`→`APR`/`STOCK`→`STK`/`EQUIPMENT`→`EQP`/`INVENTORY`→`INV`/`BOARD`→`BRD` rename 후 기존 회사의 권한 매트릭스 정상 동작(role_detail UPDATE 후 권한 유지). `/api/meta/modules` 한글 라벨 응답 확인.
5. **번호 체계**: 분할 입고 2회 → 입고마다 별도 전표번호(STK) 부여(시스템 `historyNo`와 구분, GR/GI 구분 없이 `txTypeCode`로 IN 확인); 수동 출고 → STK 전표·`txTypeCode=OUT`·**출고증 출력**. 이력 화면 `docNo` 검색·그룹.
6. **테넌트 격리 회귀**: 타 회사 비노출, 기존 입출고/월마감 정상.
