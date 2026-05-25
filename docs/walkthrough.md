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
