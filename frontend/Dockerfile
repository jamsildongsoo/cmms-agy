# 빌드 컨텍스트 = 레포 루트 (docker/nginx/prod.conf 접근 위해). docker-compose.prod.yml 참조.
# ===== build =====
FROM node:20-alpine AS build
WORKDIR /app
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# ===== serve (nginx + 정적 React) =====
FROM nginx:1.25-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY docker/nginx/prod.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
