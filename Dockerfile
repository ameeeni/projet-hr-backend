# ─── Stage 1 : Build ─────────────────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copier les descripteurs avant les sources pour bénéficier du cache Docker
COPY pom.xml mvn-settings.xml ./
RUN mvn dependency:go-offline -s mvn-settings.xml -q 2>/dev/null || true

COPY src ./src
RUN mvn package -DskipTests -s mvn-settings.xml -q

# ─── Stage 2 : Runtime ───────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime

ARG BUILD_DATE
ARG VCS_REF
LABEL org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.revision="${VCS_REF}" \
      org.opencontainers.image.title="hr-backend" \
      org.opencontainers.image.description="HR Project Backend — Spring Boot 3"

# Utilisateur non-root pour la sécurité
RUN addgroup -S hrgroup && adduser -S hruser -G hrgroup
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar
RUN chown hruser:hrgroup app.jar

USER hruser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
