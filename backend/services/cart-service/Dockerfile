FROM eclipse-temurin:25-jdk-alpine
LABEL authors="tuiop"

WORKDIR /app

COPY target/cart-service.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
