# syntax=docker/dockerfile:1

FROM maven:3.9.16-eclipse-temurin-25-alpine AS build

WORKDIR /workspace

COPY . .

RUN --mount=type=cache,target=/root/.m2 \
    mvn --batch-mode --no-transfer-progress \
    -pl services/catalog-service -am \
    package -DskipTests

FROM eclipse-temurin:25-jre-alpine
LABEL authors="tuiop"

WORKDIR /app

COPY --from=build /workspace/services/catalog-service/target/catalog-service.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
