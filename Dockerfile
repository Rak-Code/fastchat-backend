# ----------------------
# Stage 1: Build
# ----------------------
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy only pom first (layer caching)
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -B

# ----------------------
# Stage 2: Runtime
# ----------------------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S app && adduser -S app -G app

# Copy jar
ARG JAR_FILE=fastchat-backend-0.0.1-SNAPSHOT.jar
COPY --from=builder /build/target/${JAR_FILE} app.jar

# Permissions
RUN chown app:app app.jar
USER app

EXPOSE 8080

# Container-aware JVM opts
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"

# Healthcheck without extra packages
HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
  CMD java -jar app.jar --spring.main.web-application-type=none || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
