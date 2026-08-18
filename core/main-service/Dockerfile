FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY target/main-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 9092

ENTRYPOINT ["java", "-jar", "app.jar"]
