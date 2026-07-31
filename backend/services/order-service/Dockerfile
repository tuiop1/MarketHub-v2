FROM eclipse-temurin:25-jdk-alpine
LABEL authors="tuiop"

WORKDIR /app

COPY target/order-service.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
