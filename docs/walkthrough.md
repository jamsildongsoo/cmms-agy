# CMMS 인증 & 기준 정보 설정 (MDM) 개발 및 검증 결과

본 문서는 3단계 마일스톤 중 **인증(Auth)**, **내 정보 관리(My Page)** 및 **기준 정보 설정(MDM)** 통합 구현에 대한 요약 및 검증 결과를 제공합니다.

## 구현 사항

### 1. Spring Security & JWT 인증 시스템 (Backend)
- **Multi-Tenant JWT 디자인**: JWT 토큰의 `subject`에 `companyId:userId` 형태의 식별자를 주입하여 스프링 시큐리티에서 각 테넌트를 동적으로 인지하도록 구현했습니다.
- **CustomUserDetailsService**: 테넌트 정보를 포함한 유저 로드 로직을 개발했습니다.
- **SecurityConfig**: 
  - JWT 토큰을 매 요청마다 파싱하고 컨텍스트에 담는 `JwtAuthenticationFilter` 적용.
  - BCrypt 해싱 방식을 통한 비밀번호 해싱 처리 및 시큐리티 설정.
  - 로컬 CORS 무력화 및 상태 비저장형(Stateless) 세션 정책 적용.
- **AuthService & AuthController**:
  - `login`: 사용자 인증 후 JWT 토큰 및 회원 메타데이터 리턴, 로그인 히스토리 기록.
  - `signUp`: 중복 체크 및 존재하지 않는 회사일 경우 신규 회사 자동 생성 및 표준 부서/권한/권한상세 매트릭스(`SYSTEM`, `ADMIN`, `MANAGER`, `USER`) 초기 세팅.
  - `refresh`: 만료 토큰 연장 처리.
  - `me`, `me/password` (패스워드 변경 API): 사용자 정보 제공 및 현재 패스워드 검증 후 신규 패스워드 변경 처리.

### 2. Multi-Tenant 기준 정보 (MDM) API 및 비즈니스 로직 (Backend)
- **MDM 엔티티 정의**: `Plant`, `Warehouse`, `CodeGroup`, `CodeItem` 엔티티 정의 및 복합 기본키(`PlantId`, `WarehouseId`, `CodeGroupId`, `CodeItemId`)를 처리하기 위한 IdClass 매핑을 구현했습니다.
- **MdmService**:
  - 회사(Company), 플랜트(Plant), 부서(Department), 사용자(User), 권한 그룹(Role) 및 상세 매트릭스(RoleDetail), 창고(Warehouse), 공통코드그룹/아이템(CodeGroup/Item)에 대한 테넌트 격리형 CRUD 로직을 모두 구현했습니다.
  - 신규 권한 그룹 생성 시 디폴트 모듈별 권한을 자동 셋업하고, 부서 생성 시 계층 구조를 지원하기 위한 parentId 필드를 매핑했습니다.
- **MdmController**:
  - `@AuthenticationPrincipal UserPrincipal`을 활용해 테넌트 식별자(`companyId`)를 안전하게 주입받아 데이터베이스의 논리 격리 처리를 구현했습니다.
  - 회사(Company) 관련 민감 API에 대해서는 `@PreAuthorize("hasRole('ROLE_SYSTEM')")` 보안 처리를 적용했습니다.

### 3. 프론트엔드 UI 및 상태 제어 (Frontend)
- **Zustand 글로벌 스토어 (`useAuthStore.ts`)**: 
  - 사용자 세션, JWT 토큰 상태 관리.
  - 30분(1800초) 세션 타이머 내장 및 초 단위 실시간 카운트다운. 만료 시 자동 로그아웃.
  - 연장 API(`refresh`)와 동기화된 세션 연장 기능.
- **로그인 & 회원가입 화면 (`Login.tsx`)**:
  - 슬레이트 다크 계열의 현대적이고 고급스러운 디자인.
  - 로그인 폼과 회원가입 폼 간의 모던한 토글 전환 및 필수값 검증 기능.
  - 회원가입 시 신규 회사 코드를 기재하면 백엔드에서 신규 회사 생성 및 표준 권한 매트릭스를 자동 적재하도록 연동.
- **내 정보 관리 화면 (`MyPage.tsx`)**:
  - 사용자 상세 정보 실시간 로드 및 수정 폼.
  - 비밀번호 변경 전용 폼 내장 (현재 비밀번호, 새 비밀번호, 확인 입력값 검증).
- **대시보드 메인 레이아웃 및 껍데기 (`Dashboard.tsx` & `Sidebar.tsx`, `Header.tsx`)**:
  - 좌측 사이드바 네비게이션: 기획서에 따른 메뉴 목록(설비, 재고, 예방점검, 지시서, 허가서, 재고처리, 결재함, 게시판) 구성 및 기준정보 설정 포털 연동.
  - 상단 헤더: 로그인 유저 식별 정보(`[회사] 이름 (직급/직책)`) 표시, 실시간 세션 만료 시간 및 연장 버튼, 로그아웃 버튼 배치.
- **기준 정보 관리 통합 화면 (`MdmLayout.tsx`)**:
  - 플랜트, 부서(계층 구조), 사용자, 권한 매트릭스(C/R/U/D/A 토글 제어), 창고, 공통코드그룹/아이템에 대한 CRUD 및 API 호출 로직을 미학적으로 정돈된 세그먼트 탭 구조로 통합 개발 완료했습니다.

## 검증 결과 (Verification)

### 1. 백엔드 빌드 및 Spring Boot 기동 성공
- `./gradlew bootRun` 컴파일 및 의존성 주입이 성공적으로 완료되었습니다.
- 포트 `8080` 정상 서비스 중이며 신규 API 바인딩이 완벽하게 완료되었습니다.

### 2. 프론트엔드 타입스크립트 빌드 성공
- 타입 검사 및 CSS 최적화 검증 통과 후 `npm run build`에 대성공했습니다.
- Vite 개발 서버 `npm run dev`를 통해 포트 `5173`에서 정상 동작 중이며 Nginx `8082` 포트로의 프록시가 CORS 없이 유연하게 맞닿아 있습니다.

---

## 환경변수 외부화 및 설정 관리

자격증명·환경별 설정을 코드/형상관리에서 분리하기 위해 `application.yml`을 정리했습니다.

### 설정 원칙
- **단일 설정 (프로파일 분리 없음)**: 개발·운영 모두 Supabase DB를 사용하므로, 기존 `default`(로컬 docker) / `supabase` 프로파일 이원화를 단일 설정으로 통합했습니다. 환경 간 차이는 **주입되는 환경변수 값**뿐입니다.
- **자리표시자만 유지 + fail-fast**: `application.yml`에는 `${DB_URL}` 같은 자리표시자만 두고 실제 값은 두지 않습니다. 민감/필수 항목은 **기본값을 두지 않아** 미주입 시 애플리케이션이 기동되지 않습니다.

### 환경변수 목록
| 변수 | 필수 | 비고 |
|------|:---:|------|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | ✅ | 미설정 시 기동 실패 |
| `JWT_SECRET` | ✅ | HS256용 base64 256bit+ 키. 미설정 시 기동 실패 (`openssl rand -base64 32`) |
| `DB_POOL_MAX`(5) / `DB_POOL_MIN`(2) | ⬜ | Hikari 풀, 기본값 존재 |
| `JPA_SHOW_SQL`(true) / `JWT_EXPIRATION`(1800000) | ⬜ | 기본값 존재 |
| `STORAGE_*` | ⬜ | **미구현** Object Storage용. [task.md 9단계](./task.md) 구현 시 사용 |

### 주입 방법
- **로컬**: 프로젝트 루트의 `.env`(gitignore됨)를 셸에 로드 — `set -a; source .env; set +a` 후 `./gradlew bootRun`. (또는 IDE Run Config의 EnvFile.)
- **운영**: systemd `EnvironmentFile=` / docker `--env-file` / Kubernetes Secret으로 주입. jar는 고정 산출물, 환경별 차이는 env 파일/Secret만 교체.
- 키 목록·가이드는 `.env.template`(커밋됨)에 유지.

### 검증
- 환경변수 미주입 상태로 기동 시 `Could not resolve placeholder 'JWT_SECRET'`로 즉시 실패함을 확인 (fail-fast 정상 동작, DB 접속/​Flyway 실행 이전 단계).

---

## 서버측 인가(RBAC)

기존엔 회사(Company) API 외 모든 변경 엔드포인트에 서버 인가가 없어, 인증만 되면 누구나 호출 가능하고 `role_detail` C/R/U/D/A 매트릭스는 프론트 장식에 불과했다. 이를 서버에서 실제 강제하도록 적용했다.

### 모듈 정의
권한 모듈은 `AppModule` enum 단일 소스로 관리한다(시드와 `@PreAuthorize`가 이 이름을 공유):
`MDM, EQUIPMENT, INVENTORY, STOCK, PM, WO, WP, APPROVAL, BOARD`.
- 재고 **마스터**(`INVENTORY`)와 재고 **처리**(입출고·이동·월마감, `STOCK`)는 별도 모듈로 분리. 기존 롤은 `V3__add_stock_permission.sql`로 INVENTORY 권한을 복사해 STOCK 행을 백필.
- 회사(Company)는 매트릭스에 넣지 않는다(아래 참조).

### 인가 규칙 (`PermissionChecker`, 빈 이름 `perm`)
1. **SYSTEM role → 전부 통과**.
2. **Company API(`/api/mdm/companies*`) → `hasRole('SYSTEM')`** (매트릭스가 아닌 role 기반 전용 통제).
3. **그 외 → `@PreAuthorize("@perm.check('<MODULE>','<ACTION>')")`** 로 `role_detail[company, role, module]`의 해당 액션 플래그(C/R/U/D) 검사. 행 없으면 거부.
4. 액션 매핑: 조회=R, 생성/저장=C, 수정=U, 삭제=D. (월마감은 마감 레코드 생성이므로 `STOCK,C`.)

### 권한 `A`(자체 확정)의 의미
`A`는 결재 모듈의 승인 권한이 **아니다**. 결재 연계 모듈(PM/WO/WP)에서 결재를 거치지 않고 `status="S"`로 **자체 확정**할 수 있는 권한이다.
- 저장 엔드포인트는 `@perm.checkSave('<MODULE>', status)` 사용: 저장은 C, 단 `status="S"`면 A를 추가로 요구. A가 없으면 자체 확정 불가 → 결재 상신 필요.
- 기본 시드상 USER는 A 없음(상신 필요), MANAGER/ADMIN은 A 보유(자체 확정 가능).

### 결재 승인 권한
결재 처리(`POST /api/approval/{id}/action`)는 **권한 매트릭스로 관리하지 않는다.** 인증된 사용자 중 "해당 문서의 대기 단계 결재자 본인"인지를 `ApprovalService`가 행단위로 검증한다(결재선 기반).

### 검증
- 컴파일 통과, Auth(permitAll)·결재 action(인증 전용) 제외 전 엔드포인트 `@PreAuthorize` 커버리지 100% 확인.
- `-parameters` 컴파일 적용(Spring Boot 3.2+ Gradle 플러그인) 확인 → SpEL `#request`/`#permit` 파라미터명 해석 가능.

### 예외 응답 일원화
사용자에게 일관된 JSON(`{status, message}`)을 주도록 처리(이전 리뷰 M5):
- `GlobalExceptionHandler`(`@RestControllerAdvice`): 비즈니스 검증 실패 400, `@PreAuthorize` 거부 403, 무결성 위반(동시 입고 PK 충돌 등) 409 "재시도", 그 외 500.
- `SecurityConfig`의 필터 단계 핸들러: 미인증/토큰만료 `AuthenticationEntryPoint` 401, 필터 단계 권한거부 `AccessDeniedHandler` 403.

### 입력 형식 오류 일괄 400
잘못된 입력 예외(타입 불일치·필수 파라미터 누락·본문 파싱 실패·`@Valid` 위반·`DateTimeParse`)를 `GlobalExceptionHandler`에서 400으로 일괄 매핑. `closeMonth`의 `closingYm`은 `YYYYMM` 형식 검증 추가(미부합 시 400, 기존 substring/parse 500 방지).

---

## 멀티테넌트 격리: 회사별 SYSTEM 역할 제거

`setupNewCompany`가 신규 회사마다 생성하던 표준 역할에서 **SYSTEM 제거** → `ADMIN/MANAGER/USER`만 생성. 회사별 SYSTEM 역할은 사내 ADMIN이 `updateUser`로 사용자를 SYSTEM으로 올리면 권한 매트릭스 우회 + `hasRole('SYSTEM')`로 열리는 Company API(`getAllCompanies` 전 테넌트 반환)를 통해 **타 회사 데이터 접근**이 가능한 격리 위반 경로였다. SYSTEM 역할은 시드 SYSTEM 테넌트(`sysadmin`) 전용.
> 잔여(todo C5): 기존에 생성된 회사별 SYSTEM 역할 정리(마이그레이션), 롤 배정 시 SYSTEM 할당 차단.

---

## 코드 상수화 (매직 스트링 → enum) + 정본 코드표

상태/유형 코드를 타입 안전하게 정리. **정본은 `db_specification.md §5`** (상태·유형 코드 단일 소스).
- enum 4종(`com.cmms.constant`): `SeqModule`(WO/WP/PM/APR), `RoleType`, `DocStatus`(T/P/C/S/R/X), `ApprovalStepType`(D/A/G/R). 엔티티 컬럼은 `String` 유지(DB 무변경), 서비스 로직 리터럴만 치환.
- 원칙: **상호배타 다중상태 = 단일 코드(enum)**, **이분값 = `Y`/`N` 플래그**. 채번 모듈(`APR`)과 권한 모듈(`APPROVAL`)은 별개 네임스페이스.

### 코드 ID 정규화 (`CodeUtil`)
모든 코드성 식별자를 **대문자 + `[A-Z0-9_-]`** 로 정규화(`CodeUtil.normalize`, 선택값은 `normalizeOptional`). 대상: companyId, roleId, departmentId, plantId, warehouseId, codeGroupId, codeItemId. 제외: userId(로그인 아이디), 이름, 생성 문서번호.
- 적용: `CompanyService`(companyId), `AuthService`(signup/login companyId), `MdmService`(save* 코드 ID + user roleId/deptId). 기존 `CompanyCodeUtil` → `CodeUtil`로 통합.
- 효과: 대소문자 중복 회사 방지 + **롤ID 케이스 불일치 권한 버그 해소**(signUp 대문자 vs 롤 생성 케이스 불일치).
- 금지문자 사유: `:`(JWT subject), `/?#%& 공백`(URL), 따옴표류(SpEL).

### SYSTEM 콘솔 — 전 테넌트 사용자/로그인이력 관리 (BE+FE)
- BE: `SystemAdminController`(`/api/system`, SYSTEM 전용) + `SystemAdminService`. `companyId`를 파라미터로 받아 **교차 테넌트** 동작.
  - `GET /api/system/users?companyId=`(옵션), `PUT /api/system/users/{companyId}/{userId}/use-yn`, `GET /api/system/login-history`.
  - `SystemUserResponse` DTO(passwordHash 미포함). 안전장치: 플랫폼(`SYSTEM`) 테넌트 계정은 useYn 변경 불가.
- FE: `SystemAdmin.tsx`(사용자 관리/로그인 이력 탭), Sidebar에 **SYSTEM 롤만** "시스템 관리" 메뉴 노출(`roleId` 대소문자 무시).
- 온보딩 모델 완성: sysadmin이 회사 생성(롤·코드 시드) → 직원 참여 가입(useYn='N') → sysadmin이 SYSTEM 콘솔에서 활성화.

### 결재 결과 코드 정리 + 임시저장/재상신 (⚠️ 컴파일 검증, 런타임 테스트 보류)
- **`approval_result`를 빈칸(대기)/`Y`(승인)/`N`(반려)로 전환**(enum 없이 Y/N 플래그). "현재 차례"는 저장하지 않고 **단계 순서 + 직전 단계 승인 여부로 계산**. 엔티티 nullable + `V4` 마이그레이션(`A`→`Y`, `R`→`N`, `T`/`P`→NULL, DROP NOT NULL).
- `processApprovalAction`: 종료 문서 가드(진행중만 처리), 승인 시 남은 미처리 단계 없으면 완결확정, 반려 시 문서 `REJECTED`.
- **결재자(결재/합의) 없이 상신 시 → 임시저장(`TEMP`)**(라우팅·연계모듈 결재중 처리 안 함). 상신함(`getSentApprovals`)은 상태 무관 반환이라 임시저장도 조회됨.
- **재상신**: `submitApproval`이 기존 문서 id를 받으면(임시저장 상태에 한해) 단계 제거 후 재생성하여 다시 상신. 결재자가 추가되면 `IN_PROGRESS`로 라우팅.
- 검증: 컴파일 통과. **결재 워크플로 동작 변경분은 런타임 테스트 추후 진행 예정**(다단계 진행/반려 후 비노출/완결 전파/V4 데이터 변환).
- **결재함 4종**(`Approval.tsx` 탭 / `/approval/*`): **결재대기함**(내 차례·미처리·진행중) / **기안·상신함**(내가 기안, 상태무관·임시저장 포함) / **참조문서함**(참조자 R) / **결재·반려함**(`/processed`, 내가 결재/합의자로 처리한 Y·N 전체, 기안 D·참조 R 제외). 순수 결재자가 처리 후 문서를 다시 조회하는 공백 보완. 엔드포인트 200 검증 완료(실데이터 워크플로는 추후).

---

## 8단계: 출력(인쇄) 양식 통일 (Frontend)

named page 방식(Edge/Chrome 기준)으로 인쇄 정책을 중앙화. 기준 템플릿은 설비마스터.
- **중앙 CSS**(`index.css`): `@page portrait/landscape`(A4 size + **margin 12mm 통일**), `.print-landscape`/`.print-portrait`, `@media print`에서 UI 숨김(nav/aside/button 등)·높이/오버플로 제약 해제(빈 페이지 방지). 문서형은 인라인 `@page`가 없어 기본 portrait → 중앙 margin만 통일(개별 서식 유지).
- **공통 헤더** `PrintHeader.tsx`: 목록용 타이틀/회사/출력자/출력일시. 출력일시 형식 `toLocaleString('ko-KR')`로 **전 문서 통일**(기존 `toISOString` 제각각 → 일원화).
- **목록 가로**: 설비·재고(루트 `.print-landscape`), 작업지시·예방점검 이력·재고 수불대장(목록 컨테이너에 `.print-landscape` + `가로 목록 인쇄` 버튼). 목록+문서 모달이 공존하는 PM/WO/InventoryTx는 **문서 모달/슬립 열림 시 목록을 `print:hidden`으로 격리**하여 방향 충돌 방지.
- **문서 세로**: 작업지시서·예방점검 보고서·작업허가서·입출고 전표·전자결재(기본 portrait + 12mm).
- 검증: 프론트 빌드(타입체크) 통과. **브라우저 인쇄 미리보기 시각 검증은 추후**(머리글/바닥글은 인쇄 대화상자에서 해제).

---

## 9단계: 파일 첨부 (Object Storage, BE+FE)

S3 호환 스토리지(Supabase) 기반 첨부. **1차 범위: 게시판·결재만**(설비/PM/WO/WP 제외). 업로드·다운로드는 **백엔드 경유**.

### P0. 의존성·설정
- AWS SDK v2 `s3`(BOM 2.28.16). `StorageProperties`(record, `@ConfigurationProperties("cloud.aws")`, `region.static`는 `@Name` 매핑) — 죽은 설정을 실제 바인딩으로 전환.
- `StorageConfig`의 `S3Client` 빈: **endpoint override + path-style**(S3 호환). `AsyncConfig`: `@EnableAsync`(s3 삭제용 `s3TaskExecutor`) + `@EnableScheduling`(P4용).

### P1. 백엔드 코어 (`com.cmms`)
- 엔티티: `FileAttachment`(`(company_id, group_no)` 복합키, `group_no` IDENTITY, `BaseEntity` 상속=감사/소프트삭제) + `FileAttachmentItem`(`(company_id, group_no, item_no)`, **감사/`delete_yn` 없음 → 단건 물리삭제**). `item_no`는 그룹 내 `max+1` 채번.
- `FileStorageService`: 업로드(빈파일/**MIME 화이트리스트**(`STORAGE_ALLOWED_MIMES`, 와일드카드 `image/*` 지원) 검증, `stored=UUID+ext`, `storage_path={companyId}/{refModule}/{groupNo}/{stored}`, SHA-256, S3 putObject → 메타 저장 / **예외 시 put된 객체 보상 삭제**) / 다운로드(**S3 스트림 → InputStreamResource**, Content-Disposition UTF-8) / 삭제(메타 물리삭제 동기 → **afterCommit `@Async` S3 deleteObject**, `S3Cleaner` 실패 로깅).
- `FileController`(`/api/files`, 인증): `POST`(multipart, refModule/refNo/groupNo → `UploadResponse`), `GET /{groupNo}`(목록), `GET /{groupNo}/{itemNo}/download`, `DELETE /{groupNo}/{itemNo}`.
- **테넌트 격리**: 조회/다운로드/삭제 모두 복합키에 `companyId` 포함(타 테넌트 → not found), S3 key 접두사 `companyId`. 파일명 traversal 차단(`baseName`), 응답에 `storage_path`/checksum 미노출.

### P2. 도메인 연동 (게시판·결재)
백엔드 **무변경** — `Board.fileGroupId`(엔티티 컬럼, `@RequestBody Board` 저장)와 `Approval.fileGroupId`(`ApprovalSubmitRequest.approval` 엔티티째 저장)가 **기존 라운드트립으로 충족**. 상세 응답도 엔티티째 반환하여 노출.

### P3. 프론트엔드
- 공통 `FileUpload.tsx`: 드래그&드롭·다중·진행률·목록/다운로드/삭제. 첫 업로드로 그룹 생성 시 `onGroupNoChange`로 상위 폼에 `fileGroupId` 전달. 다운로드는 인증 헤더 필요 → **axios `responseType:'blob'` → ObjectURL**. `readOnly` 모드(상세조회).
- 결재(`Approval.tsx` 상신/상세, `refModule=APR`)·게시판(`Board.tsx` 작성/상세, `refModule=BOARD`) 연동. 결재 인쇄 문서엔 첨부 `print:hidden`.

### P4. 고아 객체 정리(reconciliation)
`FileReconciliationService`(`@Scheduled` cron): 버킷을 스캔해 **메타 없는 S3 객체를 유예시간 경과 후 제거**(삭제 시 @Async 실패분·업로드 중단 잔여물 청소). **기본 비활성**(`cloud.aws.reconcile-enabled=false`), cron·유예시간 설정화. 설비/PM/WO/WP 확대는 범위 제외.

### 검증
백엔드 **컴파일** + 프론트 **빌드** 통과. **런타임(실 Supabase Storage) 검증은 추후** — `STORAGE_*` 환경변수·버킷 준비 후 업로드/다운로드/삭제 전체 플로우 + (활성화 시) reconciliation 확인 필요.

---

## 10단계: 구매(Procurement) 도입 + 부수 정리

재고 관리 흐름을 **구매요청 → 발주/배송 → 입고(히스토리) → 플랜트별 재고 가시화**로 확장. 풀 구매 워크플로우(승인·견적·입찰·선정·발주서·회계)는 전산화하지 않고 시스템은 ① PR 기록·출력 ② 발주/배송 절차 상태 ③ 입고 재고 반영 ④ docNo 기반 이력만 책임. 설계 단일 소스는 `docs/product_requirements.md` 2.4·2.0 + `docs/db_specification.md` §1.4·§2.7·§5.1·§5.4·§5.6 + `docs/ui_specification.md` /procurement·§3.6~3.9. 실행 과제 P1~P7은 `docs/todo.md`.

### P1·P1b. 모듈명 통일 + label() + `SeqModule` 제거 (BE+FE)
- `AppModule` enum **전 모듈 ≤3자 짧은 코드로 통일**: `MDM/EQP/INV/STK/PM/WO/WP/APR/BRD/PUR`. 기존 `APPROVAL`/`STOCK`/`EQUIPMENT`/`INVENTORY`/`BOARD` → `APR`/`STK`/`EQP`/`INV`/`BRD` rename, **신규 `PUR`** 추가. 콜사이트 `@PreAuthorize` 26곳 일괄 치환(Master 11/Approval 5/InvTx 4/Board 6) + V4의 `UPDATE role_detail SET module_detail=...` 5건으로 기존 데이터 정렬.
- `AppModule.label()` 메서드로 한글 라벨 단일 소스화 → `GET /api/meta/modules` 엔드포인트 신규(`MetaController`). FE `MdmLayout.tsx`의 정적 `MODULE_NAME_MAP`(낡은 키) 폐기 후 동적 로드.
- **`SeqModule` enum 제거**: 모듈명 통일로 채번 prefix와 권한 키가 동일해져 별도 enum 불필요. 호출부 4곳(`PmService:112`, `WorkPermitService:40`, `WorkOrderService:54`, `ApprovalService:55`) `SeqModule.X.code()` → `AppModule.X.name()` 치환.

### P2. 문서번호 포맷 `{모듈}-{부서}-{년월}-{순번}` (BE)
- 사유: WO/WP/PM PK가 `(company, plant, id)`인데 채번 카운터는 `(company, module, dept, yyyyMM)`이라, 같은 회사·플랜트의 **다른 부서 사용자가 같은 달 동일 모듈을 생성하면 PK 충돌**(잠재 INSERT 실패). 부서 세그먼트를 출력 문자열에 포함시켜 차단.
- `SequenceService.java:42` `String.format("%s-%s-%04d", ...)` → `"%s-%s-%s-%04d"`로 변경(`departmentId` 포함). 회사는 PK·UI 컨텍스트에 이미 있어 문자열에서 제외.
- `ApprovalService.java:55` 채번 호출의 부서를 `"DEPT_ROOT"` 고정 → **기안자 부서**(`drafter.departmentId`, `UserRepository` 주입) 사용 — 전 모듈 단일 채번 포맷 일관성.
- WO/WP/PM 호출부는 이미 dept 전달 중이라 무변경. 부서 없는 사용자 fallback `DEPT_ROOT`. 기존 데이터(구포맷 id) 그대로 두고 신규부터 새 포맷(혼재 허용).

### P3. V4 마이그레이션 (`V4__procurement.sql`)
- DDL: `ALTER warehouse ADD plant_id varchar(50)`(nullable=공통부문), `ALTER inventory_history ADD doc_no varchar(50)`, `CREATE vendor` / `purchase_request` / `purchase_request_item`(복합 PK + BaseEntity + FK CASCADE).
- 데이터 정렬: 모듈명 rename UPDATE 5건, 기존 회사들에 `PUR` `role_detail` INSERT(ADMIN/MANAGER CRUDA Y, USER CRUD Y·A N — 매트릭스 동일), `UPDATE role SET multi_plant='Y' WHERE id='ADMIN'`.
- 공통코드: `PR_TYPE` 코드그룹 + 아이템(NORMAL/ROUTINE/PLANNED_PM/URGENT/ETC)을 `company_id='SYSTEM'`으로 시드 — 신규 회사는 `copySystemCommonCodes`로 자동 복사되어 별도 시드 불필요.

### P4. `DocStatus` enum 단일 소스 확장 (BE)
- 별도 `ProcStatus` 만들지 않고 `DocStatus`에 절차상태 4개 **추가**: `ORDERED("O")` / `SHIPPING("D")` / `RECEIVED("I")` / `CLOSED("E")` — mnemonic Order/Delivery/Incoming/End. 기존 `T/P/C/S/R/X`와 비충돌. 분기/상수 최소화 원칙.

### P5. 백엔드 구매 도메인 (모델/Repo/DTO/Service/Controller)
- 신규 모델 + IdClass + Repository: `Vendor`, `PurchaseRequest`, `PurchaseRequestItem`. `Warehouse`에 `plantId`(nullable), `InventoryHistory`에 `docNo` 필드 추가. `InventoryTxDto.TxItem`에 `docNo`/`refNo`/`refModule` 추가.
- `VendorService` + `VendorController`(`/api/vendors`, `@perm.check('PUR',...)`): CRUD.
- `ProcurementService` + `ProcurementController`(`/api/procurement`):
  - `createOrUpdate`(저장 `status=T`)/`confirm`(확정 `status=S`, 현장; `checkSave('PUR','S')` → C+A 권한). PR은 결재 비연계라 `T`/`S`만 사용(X/P/C/R 미사용 — T 폐기는 deleteYn 소프트 삭제, S 이후 폐기는 절차 종료 E).
  - `placeOrder`(`proc_status=O`, vendor·orderDate·etaDate)/`startShipping`(`D`, shipStartDate) — `@perm.check('PUR','U')`.
  - `receive`(현장, `@perm.check('STK','C')` — 재고 반영): 확정(`S`) PR만. STK 전표 채번 → `InventoryTransactionService.processTransactions` 위임(`TxItem{txTypeCode:"IN", docNo, refModule:"PUR", refNo:requestId}`) → `receivedQty` 누적 + `proc_status=I`. `ReceiveRequest.close=true`면 곧바로 `E`. **수량 과소/초과 판정 안 함**.
  - `close`(`E`, 현장; `@perm.check('PUR','U')`): 독립 액션 — 미입고 PR도 호출 가능.
  - **`cancelSlip(docNo)`(현장, `@perm.check('STK','C')`)**: IN/OUT 둘 다 역분개(같은 docNo로 묶음). IN 취소 시 PR `received_qty` 차감, OUT 취소 시 원본 단가로 정확 복원. **공통 조건: 그 거래 이후 동일 `(company, warehouse, inventory)`에 어떤 transaction도 없을 때만**(이동평균 손상 방지). MOVE/ADJ 미지원. 엔드포인트 `POST /api/procurement/slips/cancel/{docNo}` + 호환용 `/receipts/cancel/{docNo}` 별칭.
- `InventoryTransactionService`: `SequenceService`+`UserRepository` 주입. `processTransactions` 시 items 중 docNo가 있으면 사용, 없으면 STK 전표 자동 채번(조작자 부서). `executeCheckIn/Out/Adjustment/Transfer`에서 history `docNo`/`refNo`/`refModule` 세팅. MOVE 페어링의 `refModule`을 `"INVENTORY"` → `"MOVE"`로 의미 명확화.
- `InventoryHistoryRepository`: `existsByCompanyIdAndWarehouseIdAndInventoryIdAndHistoryNoGreaterThan`(역분개 후속거래 체크), `findByCompanyIdAndDocNo`(전표 묶음), `findByCompanyIdAndRefModuleAndRefNo`(PR별 입고 이력).
- `AuthService.login`: `LoginResponse`에 `companyName`/`lastLoginPlantId`/`multiPlant` 추가. **로그인 시 플랜트 자동 해소**(L80~85 최종 로그인 갱신 블록) — `lastLoginPlantId`가 null이면 회사 첫 활성 플랜트(`plant.deleteYn='N'`, id 정렬)를 자동 매핑·기록 → 다음 로그인부터 재사용. 플랜트 0개면 null 유지(전체). 관리자가 지정한 값이 있으면 건드리지 않음(우선).
- `MdmService.updateUser`에 `lastLoginPlantId` 반영(관리자 지정), `updateWarehouse`에 `plantId` 반영(공통부문=null). `CompanyService.seedRoles`: ADMIN 역할 `multi_plant='Y'`(그 외 N).

### P6·P6b. 프론트엔드 구매 화면 + 출력폼 + 플랜트 UI + MDM 폼 보강
- **신규 `Procurement.tsx`**(내부 탭 **구매요청/벤더 관리**): 목록(플랜트 스코프) + 상태 액션(신규/수정·확정·발주·배송·입고·종료) + 모달들. 입고 모달은 잔여 프리필·편집 가능(부분/과소/초과 UI 경고만)·단가 미입력 허용·'이 요청 종료' 체크박스. 문서/절차 상태 뱃지 분리. **구매요청서 인쇄**(`PrintHeader`+`PrintSignBox`).
- **신규 `PrintSignBox.tsx`**: 2열×4행(8칸)·라벨 없는 빈 수기 결재칸(Approval 박스 형식 차용). 구매요청서/입고증/출고증 공통.
- **`PrintHeader.tsx` 변경**: 표시 회사값 `companyId` → `companyName`(fallback `companyId`). 기존 사용처(Inventory/Equipment) 자동 반영, 그 외 자체 헤더 페이지(WO/WP/PM/Approval/InvTx 목록)는 본 범위 외(전사 헤더 통일은 별도 작업).
- **`useAuthStore.ts`**: User 인터페이스에 `companyName`/`lastLoginPlantId`/`multiPlant` 추가, `activePlantId` 상태(로그인 시 lastLoginPlantId로 초기화) + `setActivePlantId`(멀티만 변경 가능).
- **`Header.tsx`**: 멀티 권한자만 플랜트 드롭다운(전체총괄 + 플랜트 목록), 비멀티는 라벨(null이면 '전체'). `/api/mdm/plants` 로드.
- **`MdmLayout.tsx` 보강**(P6b-1):
  - `WarehouseManager`: 플랜트 select(공통부문=null 옵션) — 폼·목록·수정 모두 반영, 목록에 '플랜트' 컬럼.
  - `UserManager`: 지정 플랜트 select(자동매핑=null 옵션) — 관리자 지정 시 로그인 자동매핑보다 우선.
  - `RoleManager`: `multi_plant` 체크박스(신규 권한 그룹), 권한 목록에 '멀티' 뱃지. 정적 `MODULE_NAME_MAP` 폐기 → `/api/meta/modules` 동적 로드.
- **`InventoryTransaction.tsx` 확장**(P6b-2): `InventoryHistoryModel`에 `docNo` 추가, 이력 테이블 '이력번호' 컬럼 → '전표번호'(docNo 우선·없으면 `(NO.{historyNo})` 폴백). 전표 인쇄 모달 Slip:
  - 제목 `tx_type_code` 분기 — `IN`→**입고증** / `OUT`→**출고증** / `ADJ`→재고조정전표 / `MOVE_*`→재고이동전표.
  - 전표번호(docNo)를 큰 글씨로 노출, 이력번호는 보조 정보.
  - 출처 표기: `refModule='PUR'`이면 "구매요청 출처: PR번호", 그 외는 기존 이동 페어링 참조.
  - **전표 취소(역분개) 버튼** — `txTypeCode in {IN, OUT}` AND `docNo` 있을 때 푸터 노출. 라벨/확인메시지 거래타입에 따라 '입고 취소' / '출고 취소' 분기. `POST /api/procurement/slips/cancel/{docNo}` 호출.
- **`Sidebar.tsx`** '업무 트랜잭션' 그룹에 '구매(Procurement)' 단일 항목, **`Dashboard.tsx`** `case 'procurement'` switch 추가.
- UI 통일성: 12개 액션 버튼 라벨 단축(`신규 설비 등록` → `입력`, `예방점검 이력 목록` → `목록`, `일반 기안서 상신` → `기안문` 등 — 사이드바·헤더에 이미 모듈/탭 컨텍스트가 있어 버튼은 동작만 표시). `Procurement.tsx` `.input` 클래스 CSS 변수(`var(--color-slate-900)` 등)로 정정 — 라이트/다크 모드 자동 반전.

### 검증
- **빌드/컴파일**: 백엔드 `./gradlew compileJava` SUCCESS, 프론트 `npm run build` 통과(1818 modules).
- **마이그레이션 적용**: 앱 기동 시 Flyway V4 자동 적용. `role_detail.module_detail`이 신규 코드로 정렬, `warehouse.plant_id`/`inventory_history.doc_no` 컬럼 생성, 신규 3 테이블 생성, `role.multi_plant='Y' WHERE id='ADMIN'`, `PR_TYPE` 공통코드 SYSTEM 시드 확인.
- **런타임 엔드포인트 점검**: `POST /api/auth/login`(sysadmin) → 응답에 `companyName="시스템 관리본부"`/`multiPlant="Y"`/`lastLoginPlantId=null` 채워짐. `GET /api/meta/modules` → 10개 모듈+한글 라벨 정상. `/api/vendors`·`/api/procurement/requests` 200(빈 배열).
- **남은 검증**: 풀 사이클 E2E(벤더 등록→PR→발주→배송→입고→입고증 출력→입고/출고 취소→종료) + 다부서 번호 충돌 회귀(같은 회사·플랜트의 다른 부서가 같은 달 WO 동시 생성 시 PK 충돌 없음 확인)는 사용자 측 브라우저 검증 진행 중.

### 임시 종합 설계서 폐기
구현 기간 한정 임시 문서 `docs/procurement.md`는 폐기(`d0a66da`). 내용은 PRD(2.4 구매·2.0 공통)·DB명세(§1.4·§2.7·§5)·UI명세(/procurement·§3.6~3.9)·todo.md(P1~P7)로 분산 흡수. 신규 작업 시 이 4문서가 단일 소스.
