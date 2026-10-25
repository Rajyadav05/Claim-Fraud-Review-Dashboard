# ==============================================================================
# Dockerfile for ClaimGuard Fraud Review Dashboard
# Task 11: Production-Conscious Runtime Image
# Runs the pre-built Spring Boot executable JAR on Eclipse Temurin JRE 21 (Alpine)
# ==============================================================================

FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="ClaimGuard DevOps Team" \
      application="claimguard-dashboard" \
      version="1.0.0-SNAPSHOT" \
      task="Task-11-Docker-Lifecycle"

# Create application directory
WORKDIR /app

# Create a non-root group and user for secure, unprivileged execution
RUN addgroup -S -g 1001 claimguard && \
    adduser -S -u 1001 -G claimguard claimguard && \
    mkdir -p /app/data && \
    chown -R claimguard:claimguard /app

# Copy the pre-built production JAR with appropriate ownership
COPY --chown=claimguard:claimguard target/claimguard-dashboard-1.0.0-SNAPSHOT.jar app.jar

# Run as non-root user
USER claimguard:claimguard

# Container listens on standard Spring Boot port
EXPOSE 8080

# Default persistent database storage path inside container
ENV CLAIMGUARD_DB_PATH=/app/data/claimguard

# Production JVM flags with container cgroup awareness and pure JSON-form ENTRYPOINT
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
