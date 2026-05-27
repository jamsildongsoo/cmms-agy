#!/usr/bin/env bash
# 운영(-동등) 기동: 이미지 빌드 + 실행. .env는 compose env_file로 주입(접속/비밀).
set -euo pipefail
cd "$(dirname "$0")/.."
exec docker compose -f docker-compose.prod.yml up -d --build
