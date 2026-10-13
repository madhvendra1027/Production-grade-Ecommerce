
# Stage 1: Build the Spring Boot application
FROM maven:3.9-eclipse-temurin-24 AS build

WORKDIR /app

# Copy Maven configuration
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copy application source code
COPY src ./src

# Build the executable JAR
RUN mvn -B clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:24-jre

WORKDIR /app

# Copy the packaged JAR from the build stage
COPY --from=build /app/target/sb-ecommerce-0.0.1-SNAPSHOT.jar app.jar
# Application port
EXPOSE 8080

# Launch the application
ENTRYPOINT ["java", "-jar", "app.jar"]