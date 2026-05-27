# CMMS 개발 태스크 목록

- [x] 1단계: 개발 환경 세팅 (Infrastructure Setup)
    - [x] docker-compose.yml 및 docker 디렉토리 구성 (PostgreSQL 16 + Nginx)
    - [x] backend/ Spring Boot 3.3.x Gradle 프로젝트 생성 및 기본 세팅
    - [x] frontend/ React + Vite + TS + Tailwind CSS v4 프로젝트 생성 및 세팅
    - [x] Nginx 리버스 프록시 로컬 연동 및 CORS 해결 테스트
- [x] 2단계: 데이터베이스 스키마 및 마이그레이션 적용 (Database Setup)
    - [x] Flyway DDL 마이그레이션 파일 작성 (V1__init.sql 완료)
    - [x] 공통코드 및 기초 사용자/회사/부서 데이터 시드 스크립트 작성 (V2__seed_data.sql 완료)
- [x] 3단계: 인증 & 기준정보 모듈 개발 (Auth & MDM)
    - [x] Spring Security + JWT 로그인/인증 및 토큰 갱신 백엔드 개발
    - [x] React Zustand 글로벌 스토어 세팅 및 로그인 화면 구현
    - [x] 회사, 플랜트, 부서(계층형), 사용자, 권한(C/R/U/D/A), 공통코드, 창고 등록/수정/조회 API 및 UI 구현
- [x] 4단계: 마스터 정보 & 트랜잭션 개발 (Master & Transaction)
    - [x] 설비마스터, 재고마스터 CRUD 및 가로 리스트 출력, CSV 다운로드
    - [x] 예방점검 기록/점검항목/점검주기(스케줄) 관리 및 결재 연동 로직
    - [x] 작업지시 계획/실적 등록 및 결재 연동
    - [x] 작업허가 체크시트 JSON 데이터 처리 및 개별/종합 출력 양식
    - [x] 재고처리(입출고/이동) 평균단가 비관적 락(정렬잠금+3초 타임아웃) 및 재고마감 로직
- [x] 5단계: 결재 & 게시판 모듈 개발 (Approval & Board)
    - [x] 4x2 결재박스 화면 렌더링 및 출력(Print)용 레이아웃, 상신/결재/참조함 구현
    - [x] 게시판 공지글 고정 및 단층 댓글 시스템 구현
- [x] 6단계: 검증 및 배포 준비 (Verification & Tuning)
    - [x] 전체 연동 통합 테스트 및 예외 처리
- [x] 7단계: 추가 보완 사항 (Extra Fixes)
    - [x] 화면 테마 토글(라이트 모드/다크 모드) 전환 버튼 및 localstorage 상태 유지 구현
    - [x] 설비마스터 점검주기 등록 UI/백엔드 추가 (CheckCycle CRUD, nextCheckDate 자동계산)

- [x] **[추가 수령 태스크] 로그인 페이지 아이디/회사코드 저장 기능**
    - [x] `Login.tsx` 화면에 "아이디/회사명 저장" 체크박스 추가
    - [x] `localStorage`를 활용하여 회사코드 및 사용자 ID를 브라우저에 저장하고 재진입 시 자동 복원

- [x] **[추가] 환경변수 외부화 (설정 strict 정리)** — 상세: [walkthrough.md](./walkthrough.md)
    - [x] `application.yml` 자리표시자화: DB/JWT 자격증명 기본값 제거(fail-fast), 프로파일 단일화(개발·운영 Supabase 통일)
    - [x] `.env`/`.env.template`를 `DB_*` / `JWT_SECRET` / `STORAGE_*` 키 체계로 재정비 (변수명 `SUPABASE_*` → 일반화)

- [x] **[추가] 서버측 인가(RBAC) 적용** — 상세: [walkthrough.md](./walkthrough.md)
    - [x] `AppModule` enum으로 권한 모듈 단일화(MDM/EQUIPMENT/INVENTORY/STOCK/PM/WO/WP/APPROVAL/BOARD), 재고처리 전용 `STOCK` 신설 + `V3` 백필
    - [x] `PermissionChecker`(`@perm`) + `@PreAuthorize`로 전 엔드포인트 인가: SYSTEM 전체통과 / Company=SYSTEM role 전용 / 그 외 모듈별 C·R·U·D 매트릭스 검사
    - [x] `A`(자체확정 S) = PM/WO/WP를 결재 우회 확정 시 요구(`checkSave`). 결재 승인은 결재선(결재자 본인) 기반으로 매트릭스 미관리

- [x] **[추가] 프론트 세션 영속화 (새로고침 시 강제 로그아웃 결함 수정)**
    - [x] `useAuthStore`에 zustand `persist`(sessionStorage) 적용, `{token, user, expiresAt}`만 저장
    - [x] `init()`(main.tsx에서 1회 호출)로 새로고침 시 axios 헤더·세션 타이머 재설정, 절대 만료시각(`expiresAt`) 기준 카운트다운
    - [x] 서버 401 응답 시 자동 로그아웃하는 axios 인터셉터 추가
    - 비고: 저장소는 sessionStorage(탭/브라우저 종료 시 재로그인). 토큰은 여전히 Bearer 헤더 인증

- [x] **[추가] 프론트 리스트 로드 실패 무피드백 보완**
    - [x] 목록 로드 catch(기존 `console.error`만)에 사용자 피드백 추가 — 빈 화면을 "데이터 없음"으로 오인하는 문제 방지
    - [x] 페이지 컨벤션대로: Equipment·Inventory·WorkOrder·PM·WorkPermit는 `setMessage`, Approval·Board는 `alert` ("목록을 불러오지 못했습니다.")
    - 비고: null 가드는 추가 수정 불필요로 검토 종결(리스트 state `[]` 초기화, 상세 블록 조건부 렌더로 이미 안전)


- [x] **8단계: 출력(인쇄) 양식 전체 점검 및 통일화 (Print Layout Review)** — 코드 완료(브라우저 시각 검증 대기)
    > 방식: named page(Edge/Chrome). 중앙 정책 `index.css`(`@page portrait/landscape` size+12mm margin, `.print-landscape`/`.print-portrait`, UI 숨김·빈페이지 방지) + 공통 `<PrintHeader/>`(목록용 타이틀/회사/출력자/출력일시, 날짜형식 `toLocaleString('ko-KR')` 단일화).
    > 문서형은 인라인 `@page`가 없어 기본 세로로 인쇄됨 → 중앙 margin만 통일(개별 레이아웃 유지). 목록+문서가 한 페이지에 공존하는 PM/WO/InventoryTx는 목록 컨테이너에 `print-landscape` + 문서 모달/슬립 열림 시 `print:hidden`으로 격리.
    - [x] **공통 점검 기준 (모든 출력물 공통)**
        - [x] 브라우저 기본 머리글/바닥글 — `@page margin` 통일 + 인쇄 대화상자에서 해제(최대 억제)
        - [x] 인쇄 전용 타이틀 헤더 (문서명, 회사명, 출력자, 출력일시) — 목록 `<PrintHeader/>`, 문서 기존 서식 헤더(출력일시 형식 통일)
        - [x] 빈 페이지 생성 방지 — `@media print` 높이/오버플로 제약 해제
    - [x] **가로(Landscape) 출력 대상 - 목록(List)** — 루트/목록 컨테이너에 `.print-landscape`
        - [x] 설비마스터 목록 (Equipment.tsx) — 기준 템플릿
        - [x] 재고마스터 목록 (Inventory.tsx)
        - [x] 예방점검 이력 목록 (PreventiveMaintenance.tsx) — history 탭 `가로 목록 인쇄` 버튼 추가
        - [x] 작업지시 목록 (WorkOrder.tsx) — `가로 목록 인쇄` 버튼 추가
        - [x] 재고 수불 대장 목록 (InventoryTransaction.tsx) — `가로 목록 인쇄` 버튼 추가
    - [x] **세로(Portrait) 출력 대상 - 문서 양식** — 기본 portrait + 중앙 12mm margin (개별 서식 헤더 유지)
        - [x] 예방점검 보고서 (PreventiveMaintenance.tsx)
        - [x] 작업지시서 (WorkOrder.tsx)
        - [x] 작업허가 체크시트 (WorkPermit.tsx)
        - [x] 입출고 전표 (InventoryTransaction.tsx)
        - [x] 전자결재 문서 (Approval.tsx)

- [ ] **9단계: 파일 첨부 기능 구현 (File Attachment)** — 확정 계획
    > 기반: DB 스키마(`file_attachment`/`file_attachment_item`, 도메인 6테이블 `file_group_id`) + `application.yml` `cloud.aws.*`(STORAGE_* env) + multipart(20MB/100MB) 준비됨. AWS SDK·코드만 신규.
    >
    > **확정 결정**: 업로드·다운로드 **백엔드 경유**(presigned 미사용) / **AWS SDK v2**(`software.amazon.awssdk:s3`, endpoint override + path-style) / 삭제 시 **메타 삭제(동기, 트랜잭션 내) + S3 객체 제거(@Async, 커밋 후, 실패 로깅·재시도)** + 고아 정리(reconciliation) 안전망.
    >
    > **1차 범위(확정)**: P2 연동은 **게시판·결재만**(설비/PM/WO/WP는 P4 후속). **코드만 구현, 런타임(실 Supabase Storage) 검증은 추후**(컴파일/빌드까지).
    >
    > **스키마 확인 보정**: `file_attachment_item`엔 `delete_yn`·감사필드 없음 → **그룹(`file_attachment`)만 `BaseEntity` 상속(soft-delete+감사)**, **아이템 단건 삭제는 물리 삭제** 후 @Async S3 제거. `item_no`는 시퀀스 없음 → 그룹 내 `max(item_no)+1` 채번. `group_no`는 `BIGSERIAL`(DB 자동, insert 후 반환값 사용).

    - [x] **P0. 의존성·설정** — 컴파일 검증 완료
        - [x] `build.gradle.kts`에 AWS SDK v2 s3 추가 (BOM 2.28.16)
        - [x] `StorageProperties`(record, @ConfigurationProperties `cloud.aws`, region.static는 @Name 매핑) + `StorageConfig`의 `S3Client` 빈(endpoint override + path-style)
        - [x] `AsyncConfig`: `@EnableAsync` + `s3TaskExecutor` 빈 (S3 삭제 비동기용)
    - [x] **P1. 백엔드 코어** — 컴파일 검증 완료
        - [x] 엔티티 `FileAttachment(+Id, BaseEntity)`, `FileAttachmentItem(+Id, 비감사)` + 리포지토리(`findMaxItemNo`, `findByCompanyIdAndGroupNoOrderByItemNoAsc`)
        - [x] `FileStorageService`: 업로드(빈파일/MIME 화이트리스트(와일드카드 `image/*` 지원) 검증, `stored=UUID+ext`, `storage_path={companyId}/{refModule}/{groupNo}/{stored}`, SHA-256, S3 putObject, 메타 저장 / 예외 시 put된 객체 보상 삭제) / 다운로드(**S3 스트림→InputStreamResource**, Content-Disposition UTF-8, mime) / 삭제(메타 물리삭제 동기 → **afterCommit `@Async` S3 deleteObject**, `S3Cleaner` 실패 로깅)
        - [x] `FileController`(`/api/files`, 인증): `POST`(multipart, refModule/refNo/groupNo→UploadResponse), `GET /{groupNo}`(목록), `GET /{groupNo}/{itemNo}/download`, `DELETE /{groupNo}/{itemNo}`
        - [x] 테넌트 격리: 조회/다운로드/삭제 모두 복합키에 `companyId` 포함(타 테넌트 조회 시 not found), S3 key 접두사 `companyId`. 파일명 traversal 차단(baseName), 응답에 storage_path/checksum 미노출
    - [ ] **P2. 도메인 연동(file_group_id) — 1차: 게시판·결재만**
        - [ ] 게시판/결재 저장 시 `file_group_id` 세팅, 상세 조회 시 첨부 목록 로드
    - [ ] **P3. 프론트엔드**
        - [ ] 공통 `FileUpload.tsx`(드래그&드롭·다중·진행률·목록/삭제) — 업로드 후 fileGroupId를 폼에 보관
        - [ ] 결재(Approval.tsx)·게시글(Board.tsx) 첨부 UI + 다운로드 연동
    - [ ] **P4(선택/후속)**: 설비/PM/WO/WP 확대 + **고아 객체 정리(reconciliation) 작업**(soft-delete됐으나 S3 삭제 실패분, 업로드 중단 고아 주기 정리)
    - **보안 체크**: 테넌트 격리, MIME/확장자/크기 검증, stored UUID·경로 traversal 차단, (선택) 매직넘버 검사. 첨부 권한은 우선 인증만(후속: 대상 모듈 권한 연계).

- [ ] **[보류/보안] CORS 설정 강화** (검토 완료, 현재 변경 보류)
    > `SecurityConfig`: `setAllowedOriginPatterns("*")` + `setAllowCredentials(true)` — 임의 오리진의 자격증명 동반 요청을 허용하는 오설정(Spring이 Origin을 반사 + credentials 허용).
    > **실효 위험 낮음**: 인증이 `Authorization: Bearer` 헤더 기반(백엔드 쿠키 미사용, 프론트 `withCredentials` 미설정=false)이라 교차 오리진으로 자격증명이 오가지 않고, 운영은 nginx 동일 출처. → 토큰 탈취/요청 위조 성립 안 함.
    > **조치안(미적용)**: ① `allowCredentials(false)`(현 Bearer 모델에 부합, Bearer 동작 영향 없음) 또는 ② 오리진 env 허용목록화(`CORS_ALLOWED_ORIGINS`).
    > ⚠️ **재검토 트리거**: 쿠키 기반 인증(예: httpOnly refresh 토큰)으로 전환 시 즉시 CSRF/응답 탈취 구멍이 되므로, 그 전에 **반드시 명시 오리진 + `allowCredentials` 재검토**.
