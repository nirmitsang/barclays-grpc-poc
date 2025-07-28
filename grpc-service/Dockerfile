FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean compile dependency:copy-dependencies package -DskipTests

FROM eclipse-temurin:17-jre-jammy

# Copy the compiled classes and dependencies separately
COPY --from=builder /app/target/classes /app/classes
COPY --from=builder /app/target/dependency /app/lib
COPY --from=builder /app/target/*.jar /app/app.jar

WORKDIR /app

EXPOSE 9090

# Start the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]