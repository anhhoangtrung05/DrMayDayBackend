# syntax=docker/dockerfile:1

# ===== Stage 1: Build =====
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace

# Copy the root POM first to cache dependency downloads.
COPY pom.xml .

RUN mvn -B -q dependency:go-offline || true

# Copy the source tree and build the single-module application.
COPY src src

RUN mvn -B -q clean package -DskipTests

# ===== Stage 2: Runtime =====
FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S drmayday && adduser -S drmayday -G drmayday
USER drmayday

COPY --from=build /workspace/target/drmayday-app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
