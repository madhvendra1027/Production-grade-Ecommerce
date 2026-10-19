
# Stage 1: Build the Spring Boot application
FROM maven:3.9-eclipse-temurin-24 AS build

WORKDIR /app

# Copy Maven configuration
COPY pom.xml .
RUN mvn -B dependency:go-offline || true

# Copy application source code
COPY src ./src

# Build the executable JAR
RUN mvn -B clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:24-jre

WORKDIR /app

# Copy the packaged JAR from the build stage
COPY --from=build /app/target/sb-ecommerce-0.0.1-SNAPSHOT.jar app.jar

# Application port (Render default is 10000, Spring default is 8080)
EXPOSE 8080 10000

# Launch the application with Render PORT binding and low-memory JVM ergonomics for 512MB RAM free tier
ENTRYPOINT ["sh", "-c", "java -XX:+UseSerialGC -Xss512k -XX:MaxRAMPercentage=70.0 -Dserver.port=${PORT:-8080} -jar app.jar"]