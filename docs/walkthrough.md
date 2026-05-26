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
