FROM eclipse-temurin:25-jdk-alpine
LABEL authors="tuiop"

WORKDIR /app

COPY target/gateway-service.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
