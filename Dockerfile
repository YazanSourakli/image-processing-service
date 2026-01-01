# Stage: 1
FROM maven:3.8.6-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

# Stage: 2
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/image-processing-service-0.0.1-SNAPSHOT.jar ./image-processing-service.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "image-processing-service.jar"]
