# 배포 & 인프라 메모

운영/배포 시 헷갈리기 쉬운 항목만 짧게. 환경변수 상세는 `walkthrough.md`(환경변수 외부화) 참조.

## 구성 (이미지 2개)

| 이미지 | 내용 | 노출 |
|---|---|---|
| `cmms-web` | 프론트(Vite 빌드 산출물) + **nginx** (정적 서빙 + `/api`→`api:8080` 프록시) | `80:80` (외부 진입점) |
| `cmms-api` | 백엔드 Spring fat jar | `expose 8080` (내부 전용, 외부 미노출) |

- DB·Object Storage는 외부(Supabase). `.env`는 `env_file`로 주입 — **이미지에 굽지 않음**.
- Dockerfile: `backend/Dockerfile`(멀티스테이지), `frontend/Dockerfile`(빌드 컨텍스트=레포 루트, `docker/nginx/prod.conf` 포함).

## 환경 파일 위치
- **`.env`** — repo **루트**. 실제 값 보유, `.gitignore`로 커밋 제외. dev.sh가 line-read로 export, prod compose가 `env_file: .env`로 주입(compose 파일 기준 = repo 루트).
- **`.env.template`** — repo 루트, **커밋됨**(키 가이드, 값 없음). 새 환경은 `cp .env.template .env` 후 값 채움.
- 키 분류: 비밀/필수(`DB_*`, `JWT_SECRET`)는 기본값 없음→미설정 시 fail-fast / 운영 튜닝값(`JWT_EXPIRATION`, `DB_POOL_*`, `PWD_*`)·`IMAGE_TAG`는 기본값 존재 / `IMAGE_REGISTRY`는 prod compose에서 필수(`:?`).

## 로컬 개발 — `scripts/dev.sh`
BE(`bootRun`, profile=dev) + FE(Vite `--host`, HMR) + nginx(dev 컨테이너)를 한 번에. 접속 `http://localhost:8082` (nginx 8082 → FE 5173 / BE 8080). Ctrl-C로 일괄 종료.

## 운영 배포 — `scripts/prod.sh` / `docker-compose.prod.yml` (B안: 레지스트리 pull)
이미지는 **CI가 빌드/푸시**, 서버는 pull만.
```bash
docker compose -f docker-compose.prod.yml pull        # latest 같은 가변 태그는 pull 먼저
docker compose -f docker-compose.prod.yml up -d
```
- 필요한 `.env` 키: `IMAGE_REGISTRY`(미설정 시 compose fail-fast), `IMAGE_TAG`(기본 latest, 운영은 불변 태그 권장), `DB_*`, `JWT_SECRET`(운영 전용 키), (첨부 쓰면) `STORAGE_*`.
- 헬스체크: actuator 미사용 → api는 **8080 TCP listen**으로 기동 판정, web은 `depends_on: condition: service_healthy`로 api 기동 후 시작(초기 502 방지).
- 서버에서 소스 직접 빌드하려면 `build:` 버전을 별도 compose로 분리(현재 prod compose엔 `build:` 없음).

## TLS / nginx (`docker/nginx/prod.conf`)
- **TLS는 앞단(LB/Cloudflare)에서 종단**. 이 nginx는 평문 HTTP 80만 수신. LB에서 443→`web:80` 전달 + `X-Forwarded-Proto: https` 세팅 + HTTP→HTTPS 리다이렉트·HSTS 처리.
- nginx가 LB의 원본 스킴을 백엔드로 전달하도록 `map $http_x_forwarded_proto` 폴백 사용.
- `/api` 프록시는 `resolver 127.0.0.11` + 변수 proxy_pass → api 컨테이너 재시작으로 IP가 바뀌어도 stale-IP 502 없음.

## Flyway (⚠️ 운영 첫 적용 주의)
- 마이그레이션은 `backend/.../db/migration/`의 `V{n}__*.sql`. **api 기동 시 대상 DB에 자동 적용**.
- 운영 첫 적용은 **빈 스키마** 전제 → V1부터 전부 적용됨. 테이블만 미리 있고 history 테이블이 없으면 `baseline-on-migrate:true`가 **V1을 스킵**하므로 주의(빈 DB로 시작할 것).
- **이미 적용된 마이그레이션 파일은 절대 수정 금지**(checksum mismatch로 기동 실패). 변경은 항상 새 `V{n}` 추가로.

## 첫 배포 체크리스트
1. 서버에 Docker + 레지스트리 접근(pull)
2. 운영 `.env` 배치(`DB_*`/`JWT_SECRET` 운영값, `IMAGE_REGISTRY`)
3. 운영 Supabase가 **비어있는지**·접근 가능한지 확인
4. `pull` → `up -d`
5. `docker compose logs api`로 Flyway V1~V3 적용 + 8080 기동 확인
6. 80 포트/방화벽 + 앞단 LB(TLS) 연결
7. sysadmin 로그인 → **시드 비밀번호 `admin123` 즉시 변경** (V2 시드 계정)
8. 아래 부트스트랩으로 실제 운영 회사·관리자 생성

## 최초 부트스트랩 (온보딩 흐름)
플랫폼 슈퍼관리자(sysadmin, 회사코드 `SYSTEM`)가 테넌트를 만든다.
1. **sysadmin 로그인** — 회사코드 `SYSTEM` / 초기 `admin123`(즉시 변경).
2. **시스템 관리 → 회사 관리 → 신규 회사 생성** — 회사 정보 + **초기 관리자(ID·이름·비밀번호)** 입력. 한 트랜잭션에서 회사·기본 롤(ADMIN/MANAGER/USER)·권한 매트릭스·공통코드 시드 + **관리자 계정(`roleId=ADMIN`, `use_yn=Y`)** 생성. (`CompanyService.createCompany`)
   - 회사 코드는 대문자 정규화. 관리자 계정은 `must_change_password=Y` → **첫 로그인 시 비밀번호 변경 안내 메시지**(현재는 차단 없는 안내 수준. 강제 게이트는 미구현).
3. **관리자 로그인** → 안내에 따라 [내 정보 수정]에서 비번 변경 → ADMIN 권한으로 기준정보(MDM)·결재 등 전체 사용.
4. **일반 사용자**: 로그인 화면에서 그 회사 코드로 회원가입 → `use_yn='N'`(미승인)이라 로그인 불가.
5. **승인/권한**: sysadmin이 시스템 관리 → 사용자 관리에서 **사용여부 Y**로 승인(`PUT /api/system/users/{companyId}/{userId}/use-yn`, SYSTEM 테넌트 계정은 잠금방지로 변경 불가), 또는 회사 ADMIN이 기준정보 → 사용자 관리에서 롤·사용여부 관리.

> ⚠️ 회사 생성 시 관리자 계정이 함께 생기지 않던 과거 데드락(첫 사용자가 USER뿐이라 ADMIN 승격 불가)은 위 2번 흐름으로 해소됨.

## 비밀번호 정책 (env, 기본값 존재 — 선택)
`PWD_EXPIRY_DAYS`(90) / `PWD_MAX_FAILED`(5) / `PWD_LOCK_MINUTES`(30). 미설정 시 기본값. 만료/실패잠금/강제변경은 `AuthService.login` 참조.
