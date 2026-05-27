# ===== build =====
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew clean bootJar -x test --no-daemon

# ===== run =====
FROM eclipse-temurin:21-jre
WORKDIR /app
# 실행형 fat jar만 복사(-plain.jar 제외)
COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar
EXPOSE 8080
# DB_*/JWT_SECRET/STORAGE_*/SPRING_PROFILES_ACTIVE 는 런타임 env로 주입(.env)
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
