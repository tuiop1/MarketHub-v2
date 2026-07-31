FROM eclipse-temurin:25-jdk-alpine
LABEL authors="tuiop"

WORKDIR /app

COPY target/catalog-service.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
