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


- [ ] **8단계: 출력(인쇄) 양식 전체 점검 및 통일화 (Print Layout Review)**
    > 기준: 설비마스터 출력 양식 (가로 출력, 브라우저 헤더 제거, 커스텀 타이틀/메타 헤더)
    - [ ] **공통 점검 기준 (모든 출력물 공통)**
        - [ ] 브라우저 기본 머리글/바닥글(날짜, URL 등) 제거
        - [ ] 인쇄 전용 타이틀 헤더 (문서명, 회사명, 출력자, 출력일시) 표시
        - [ ] 빈 페이지 생성 방지
    - [ ] **가로(Landscape) 출력 대상 - 목록(List)**
        - [x] 설비마스터 목록 (Equipment.tsx)
        - [ ] 예방점검 이력 목록 (PreventiveMaintenance.tsx)
        - [ ] 작업지시 목록 (WorkOrder.tsx)
        - [ ] 재고 수불 대장 목록 (InventoryTransaction.tsx)
    - [ ] **세로(Portrait) 출력 대상 - 문서 양식**
        - [ ] 예방점검 보고서 (PreventiveMaintenance.tsx) - A4 세로
        - [ ] 작업지시서 (WorkOrder.tsx) - A4 세로
        - [ ] 작업허가 체크시트 (WorkPermit.tsx) - A4 세로 (유형별 페이지 구분)
        - [ ] 입출고 전표 (InventoryTransaction.tsx) - A4 세로
        - [ ] 전자결재 문서 (Approval.tsx) - A4 세로 (결재박스 포함)

- [ ] **9단계: 파일 첨부 기능 구현 (File Attachment)**
    > 준비된 DB 스키마(file_attachment, file_attachment_item) 및 API 연동
    - [ ] **백엔드**
        - [ ] FileAttachment, FileAttachmentItem 엔티티 생성
        - [ ] 파일 업로드 / 다운로드 / 삭제 API 개발 및 Supabase Storage(S3 SDK) 연계
        - [ ] 결재(Approval) 및 게시판(Board) 저장 시 파일그룹ID(file_group_id) 연동
    - [ ] **프론트엔드**
        - [ ] 드래그&드롭 파일 업로드용 공통 첨부 컴포넌트 개발
        - [ ] 결재(Approval.tsx) 및 게시글(Board.tsx)에 파일 첨부 UI 연동
