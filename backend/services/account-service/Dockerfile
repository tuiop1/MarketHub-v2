FROM eclipse-temurin:25-jdk-alpine
LABEL authors="tuiop"

WORKDIR /app

COPY target/account-service.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
