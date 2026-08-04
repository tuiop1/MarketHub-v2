# syntax=docker/dockerfile:1

FROM maven:3.9.16-eclipse-temurin-25-alpine AS build

WORKDIR /workspace

COPY . .

RUN --mount=type=cache,target=/root/.m2 \
    mvn --batch-mode --no-transfer-progress \
    -pl services/account-service -am \
    package -DskipTests

FROM eclipse-temurin:25-jre-alpine
LABEL authors="tuiop"

WORKDIR /app

COPY --from=build /workspace/services/account-service/target/account-service.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
