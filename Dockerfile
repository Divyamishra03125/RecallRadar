# ==========================================
# Root Dockerfile for RecallRadar Backend
# Enables zero-config Render / Cloud deployment
# ==========================================

# Stage 1: Build the Maven artifact
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy Maven descriptor and source code from recallradar-backend
COPY recallradar-backend/pom.xml .
COPY recallradar-backend/src ./src

# Build production executable JAR (skipping unit tests for deployment)
RUN mvn clean package -DskipTests

# Stage 2: Hardened Alpine JRE runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run container as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy JAR from build stage
COPY --from=build --chown=appuser:appgroup /app/target/recallradar-backend-*.jar app.jar

ENV PORT=8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD wget -q --spider http://localhost:${PORT:-8080}/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
