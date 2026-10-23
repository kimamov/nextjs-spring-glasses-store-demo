FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B verify

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S store && adduser -S store -G store
COPY --from=build /app/target/store-0.0.1-SNAPSHOT.jar app.jar
USER store
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
