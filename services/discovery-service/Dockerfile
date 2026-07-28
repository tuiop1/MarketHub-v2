FROM eclipse-temurin:25-jdk-alpine
LABEL authors="tuiop"

WORKDIR /app

COPY target/discovery-service.jar app.jar

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"]
