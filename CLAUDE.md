# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> 상위 `../CLAUDE.md`(작업 공통 원칙)도 함께 적용된다. 이 문서는 본 프로젝트의 스택·구조·코드 규칙만 다룬다.

CMMS Portal — 한국플랜트서비스용 설비관리시스템(전산 유지보수 관리). 멀티테넌트 SaaS 구조의 Spring Boot 백엔드 + React SPA 프론트엔드.

## Commands

### Backend (`backend/`, Java 21 + Gradle Kotlin DSL)
```bash
cd backend
./gradlew bootRun                          # 개발 서버 기동 (port 8080)
./gradlew build                            # 컴파일 + 테스트
./gradlew test                             # 전체 테스트
./gradlew test --tests "com.cmms.SomeTest" # 단일 테스트 클래스 실행
```
> 기동 전 환경변수 주입이 선행되어야 한다(미주입 시 fail-fast). 변수 목록·주입 방법은 `docs/walkthrough.md` 참조.

### Frontend (`frontend/`, React 19 + Vite + TS)
```bash
cd frontend
npm install
npm run dev      # Vite dev server, port 5173 (--host 바인딩)
npm run build    # tsc -b 타입체크 + vite build
npm run lint     # eslint
```

### Infra
```bash
docker compose up -d   # cmms-nginx(8082:80) 프록시 + cmms-postgres(5433, 선택적 로컬 DB)
```
- Nginx(8082)가 `/` → Vite(5173), `/api` → Spring(8080)로 프록시. 통합 동작 확인은 **8082 포트**로 접속.
- DB는 개발·운영 모두 Supabase. 앱 기동 시 Flyway가 대상 DB에 마이그레이션을 적용한다(스키마 단일 소스, 아래 Backend 계층 구조 참조).
- **환경변수/자격증명 등 설정 내용과 실행 방법은 이 문서에 두지 않는다** → `docs/walkthrough.md`(환경변수 외부화) 참조. Object Storage는 미구현이며 잔여 작업은 `docs/task.md`(9단계) 참조.

## Architecture

### Multi-Tenancy (핵심 — 모든 작업 시 반드시 인지)
모든 비즈니스 데이터는 `companyId`로 논리 격리된다. 테넌트는 **절대 클라이언트 입력으로 받지 않고** 인증 컨텍스트에서 주입한다.

- **JWT subject = `companyId:userId`** (`JwtTokenProvider.generateToken`). 로그인 시 발급, `JwtAuthenticationFilter`가 매 요청 파싱하여 `UserPrincipal`로 컨텍스트에 적재.
- 컨트롤러는 `@AuthenticationPrincipal UserPrincipal principal`로 받아 `principal.getCompanyId()`(테넌트), `principal.getUsername()`(작업자/감사 기록자)를 서비스에 전달한다. **쿼리/필터에 쓰는 companyId는 항상 principal에서 가져온다.**
- 회사 단위 민감 API는 `@PreAuthorize("hasRole('ROLE_SYSTEM')")`. 권한 등급: `SYSTEM` > `ADMIN` > `MANAGER` > `USER` (회원가입으로 신규 회사 생성 시 이 매트릭스가 자동 시딩됨).
- 인증 면제 경로: `/api/auth/**`, `/api/public/**` (그 외 전부 인증 필요). 세션은 Stateless, 토큰 만료 30분.

### Backend 계층 구조 (`com.cmms`)
표준 Controller → Service(`@Transactional`) → Repository(Spring Data JPA) → Entity. DTO는 도메인별 단일 클래스에 nested static 클래스로 묶음(예: `WorkOrderDto.WorkOrderSaveRequest`).

- **복합 기본키 패턴**: 대부분의 엔티티가 `(companyId, plantId, id)` 등 복합키. JPA `@IdClass`로 매핑하며, 각 엔티티마다 별도 `XxxId implements Serializable` 클래스(equals/hashCode 포함)가 짝으로 존재. 새 엔티티 추가 시 이 패턴을 따른다.
- **`BaseEntity`** (`@MappedSuperclass`): 모든 엔티티 상속. 감사 필드(`createdAt/By`, `updatedAt/By`) `@PrePersist`/`@PreUpdate` 자동 세팅, 그리고 **소프트 삭제 `deleteYn`('N'/'Y')**. 삭제는 물리 삭제가 아니라 `deleteYn='Y'` 처리이며 조회 시 필터링한다.
- **문서번호 채번**: `SequenceService.generateNextNo(companyId, refModule, departmentId)` → `{MODULE}-{yyyyMM}-{0001}` 형식. 회사·모듈·부서·연월 단위로 시퀀스 관리(`SequenceGenerator`). 모듈 코드: `WO`(작업지시), `WP`(작업허가), `PM`(예방점검), `APR`(결재). 새 문서 생성 시 직접 ID를 만들지 말고 이 서비스를 사용한다.
- **DB는 Flyway가 단일 소스**: `spring.jpa.hibernate.ddl-auto=none`. 스키마 변경은 엔티티가 아니라 `src/main/resources/db/migration/`에 새 `V{n}__*.sql` 추가로 한다.

### 도메인 모듈
조직/기준정보(MDM: Company, Plant, Department, User, Role/RoleDetail, Warehouse, CodeGroup/Item), 설비(Equipment + 점검 주기/항목), 재고(Inventory + 입출고 트랜잭션/이력/월마감), 예방점검(PM), 작업지시(WorkOrder), 작업허가(WorkPermit), 결재(Approval + ApprovalStep), 게시판(Board). 결재(`ApprovalService`)는 PM/WO/WP 등 타 문서를 참조하는 워크플로우 허브 — 기안자를 0순번 step으로 자동 추가하고 결재선을 순차 저장한다.

### Frontend (`frontend/src`)
- **라우터 없음**: `App.tsx`가 토큰 유무로 `Login` ↔ `Dashboard` 분기. `Dashboard.tsx`가 `activeTab` 상태 기반 `switch`로 페이지 컴포넌트를 스왑하는 단일 셸 구조(`Sidebar`가 탭 전환).
- **상태**: Zustand `useAuthStore.ts` — user/token, axios 기본 헤더 주입, **클라이언트 측 30분 세션 카운트다운**(1초 setInterval, 만료 시 자동 로그아웃, `refresh`로 연장).
- **API**: `api/axios.ts` 단일 인스턴스, `baseURL: '/api'` (Vite/Nginx 프록시 경유). 토큰은 로그인 시 `Authorization: Bearer` 기본 헤더로 설정.
- 스타일: Tailwind v4 (`@tailwindcss/vite`), 슬레이트 다크 테마. 아이콘 `lucide-react`. UI 텍스트는 한국어.

## Docs
`docs/`에 기획·설계 문서 존재: `product_requirements.md`,  `db_specification.md`(DB 명세), `ui_specification.md`, `task.md`, `walkthrough.md`(구현/검증 요약). 도메인 의도 파악 시 우선 참조.
`implementation_plan.md`는 초기 기획문서이며, 추가 업데이트 하지 않는다.