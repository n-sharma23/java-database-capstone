# Multi-stage build: compile with the full JDK and Maven, run on a small JRE-only image.

# --- Build stage ---
FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /app
# Copy the pom first so dependency downloads are cached between code changes.
COPY app/pom.xml .
RUN mvn -B dependency:go-offline
COPY app/src ./src
RUN mvn -B clean package -DskipTests

# --- Runtime stage ---
FROM eclipse-temurin:17-jre
WORKDIR /app
# Run as a non-root user.
RUN useradd --system --uid 1001 clinic
COPY --from=builder /app/target/*.jar app.jar
USER clinic
EXPOSE 8080
# Configuration (MYSQL_HOST, MYSQL_PASSWORD, MONGODB_URI, JWT_SECRET, ...) is passed as environment variables.
ENTRYPOINT ["java", "-jar", "app.jar"]
