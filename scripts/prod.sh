#!/usr/bin/env bash
# 운영 기동(B안: 레지스트리 pull). 이미지는 CI가 빌드/푸시, 서버는 pull→up만.
# .env(repo 루트)는 compose env_file로 주입(접속/비밀). IMAGE_REGISTRY 필수.
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose -f docker-compose.prod.yml pull   # 가변 태그(latest 등) 최신화
exec docker compose -f docker-compose.prod.yml up -d
