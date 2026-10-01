# Build context: repository root (needs gradlew + backend-common)
#   docker build -f admin-console/backend/Dockerfile -t wedding_admin_backend .

# Stage 1: build the boot jar with Gradle + JDK 21
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --version --no-daemon
COPY backend-common ./backend-common
COPY admin-console/backend ./admin-console/backend
RUN ./gradlew :admin-console:backend:bootJar -x test --no-daemon

# Stage 2: slim JRE runtime as non-root
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=builder /workspace/admin-console/backend/build/libs/admin-backend.jar app.jar
USER appuser
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseZGC -XX:MaxRAMPercentage=75"
EXPOSE 8082
HEALTHCHECK --interval=15s --timeout=5s --retries=5 CMD wget -qO- http://localhost:8082/actuator/health/readiness || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
