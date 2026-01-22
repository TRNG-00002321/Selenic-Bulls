# Java Manager App Dockerfile
# Multi-stage build:
# - builder: compiles jar
# - runtime: slim production image (JRE + wget)
# - testrunner: Maven + JDK image that runs the app and can execute mvn tests inside the SAME container

# =========================
# Build stage
# =========================
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src/ ./src/
RUN mvn clean package -DskipTests -B


# =========================
# Production stage (runtime)
# =========================
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Install wget for health checks
RUN apk add --no-cache wget

# Create non-root user for security
RUN addgroup -g 1001 appgroup && \
    adduser -u 1001 -G appgroup -D appuser

# Copy the built JAR from builder stage
COPY --from=builder /app/target/manager-1.0-SNAPSHOT.jar app.jar

# Create directory for database (shared volume mount point)
RUN mkdir -p /data && chown appuser:appgroup /data

# Switch to non-root user
USER appuser

# Environment variables
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV DATABASE_PATH=/data/expense_manager.db

# Expose Javalin port
EXPOSE 5001

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:5001/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]


# =========================
# Test runner stage (Option B)
# Contains mvn + JDK and runs the same app jar
# =========================
FROM maven:3.9-eclipse-temurin-21 AS testrunner

WORKDIR /app

# Install wget for health checks (Debian-based image)
RUN apt-get update && apt-get install -y --no-install-recommends wget ca-certificates && rm -rf /var/lib/apt/lists/*

# Create non-root user similar to runtime (good practice)
RUN groupadd -g 1001 appgroup && useradd -m -u 1001 -g appgroup appuser

# Copy jar built by builder
COPY --from=builder /app/target/manager-1.0-SNAPSHOT.jar /app/app.jar

# Create data directory for DB volume
RUN mkdir -p /data && chown -R appuser:appgroup /data

USER appuser

ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV DATABASE_PATH=/data/expense_manager.db

EXPOSE 5001

# Keep it simple: run app (Jenkins will docker exec into this container to run mvn tests)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
