# syntax=docker/dockerfile:1

# ===== Stage 1: Build =====
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy POM files first de tan dung Docker layer cache cho dependency download
COPY pom.xml .
COPY common/pom.xml common/
COPY facility/pom.xml facility/
COPY auth-rbac/pom.xml auth-rbac/
COPY form-builder/pom.xml form-builder/
COPY medical-record/pom.xml medical-record/
COPY media/pom.xml media/
COPY lesion-tracking/pom.xml lesion-tracking/
COPY lab/pom.xml lab/
COPY prescription/pom.xml prescription/
COPY notification/pom.xml notification/
COPY app/pom.xml app/

RUN mvn -B -q dependency:go-offline || true

# Copy toan bo source code roi build
COPY common/src common/src
COPY facility/src facility/src
COPY auth-rbac/src auth-rbac/src
COPY form-builder/src form-builder/src
COPY medical-record/src medical-record/src
COPY media/src media/src
COPY lesion-tracking/src lesion-tracking/src
COPY lab/src lab/src
COPY prescription/src prescription/src
COPY notification/src notification/src
COPY app/src app/src

RUN mvn -B -q clean package -DskipTests

# ===== Stage 2: Runtime =====
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S drmayday && adduser -S drmayday -G drmayday
USER drmayday

COPY --from=build /workspace/app/target/drmayday-app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
