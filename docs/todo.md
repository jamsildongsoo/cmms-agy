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
