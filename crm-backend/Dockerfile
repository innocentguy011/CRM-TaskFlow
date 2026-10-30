# ═══════════════════════════════════════════════════════════════════════════
# Optimized Multi-stage Dockerfile for CRM Backend (Railway-friendly)
# ═══════════════════════════════════════════════════════════════════════════

# Registry for the official Docker Library base images. All of these serve the
# same images (identical digests); only the source and its rate limits differ:
#   mirror.gcr.io/library        Google's Docker Hub cache (default)
#   docker.io/library            Docker Hub (anonymous: ~10 pulls/hour per IP)
#   public.ecr.aws/docker/library  AWS ECR Public (anonymous data cap; returned
#                                  429 "Data limit exceeded" on shared CI runners)
# Override with: docker build --build-arg BASE_REGISTRY=docker.io/library ...
ARG BASE_REGISTRY=mirror.gcr.io/library

# ───────────────────────────────────────────────────────────────────────────
# Stage 1: Build with dependency caching
# ───────────────────────────────────────────────────────────────────────────
FROM ${BASE_REGISTRY}/maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy pom.xml first (better layer caching)
COPY pom.xml .

# Download dependencies BEFORE copying source code
# This layer is cached unless pom.xml changes
RUN mvn dependency:resolve dependency:resolve-plugins -q || true

# Copy source code
COPY src ./src

# Build application (skip tests for faster builds)
RUN mvn clean package -DskipTests -q

# ───────────────────────────────────────────────────────────────────────────
# Stage 2: Runtime
# ───────────────────────────────────────────────────────────────────────────
FROM ${BASE_REGISTRY}/eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Create upload directory with correct ownership BEFORE switching user
RUN mkdir -p /tmp/uploads/chat /tmp/uploads/task-attachments && \
    chown -R spring:spring /tmp/uploads && \
    chmod -R 755 /tmp/uploads

USER spring:spring

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=120s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider "http://localhost:${PORT:-8080}/actuator/health" || exit 1

# Run application — defaults to prod profile; override via SPRING_PROFILES_ACTIVE env var
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=prod", \
    "-jar", \
    "app.jar"]
