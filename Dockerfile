# ─── Stage 1 : Build ──────────────────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copier les descripteurs en premier pour exploiter le cache des couches Docker :
# tant que pom.xml et mvn-settings.xml n'ont pas changé, cette couche est réutilisée.
COPY pom.xml mvn-settings.xml ./

# Pré-téléchargement des dépendances (couche cachée indépendante des sources).
# --no-transfer-progress : logs propres sans barre de progression verbeuse.
RUN mvn dependency:go-offline -s mvn-settings.xml --no-transfer-progress

# Copier les sources et compiler (skip tests : déjà exécutés dans le pipeline CI).
COPY src ./src
RUN mvn package -DskipTests -s mvn-settings.xml --no-transfer-progress

# ─── Stage 2 : Runtime ────────────────────────────────────────────────────────
# JRE Alpine seul (pas le JDK) : ~90 Mo vs ~330 Mo.
FROM eclipse-temurin:17-jre-alpine AS runtime

ARG BUILD_DATE
ARG VCS_REF
ARG IMAGE_VERSION=0.0.1-SNAPSHOT

LABEL org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.revision="${VCS_REF}" \
      org.opencontainers.image.version="${IMAGE_VERSION}" \
      org.opencontainers.image.title="hr-backend" \
      org.opencontainers.image.description="HR Project Backend — Spring Boot 3.4.5 / Java 17" \
      org.opencontainers.image.vendor="iteam"

# Utilisateur non-root : limite la surface d'attaque si le container est compromis.
RUN addgroup -S hrgroup && adduser -S hruser -G hrgroup

WORKDIR /app

# Copier uniquement le fat-JAR depuis le stage builder.
COPY --from=builder /app/target/*.jar app.jar
RUN chown hruser:hrgroup app.jar

# Dossier pour le fichier de credentials GCP (monté en volume, pas embarqué dans l'image).
RUN mkdir -p /app/secrets && chown hruser:hrgroup /app/secrets

USER hruser

EXPOSE 8080

# Healthcheck via l'endpoint Actuator — Kubernetes utilisera sa propre probe,
# mais ce check reste utile en Docker standalone.
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# Flags JVM optimisés pour les containers :
# -XX:+UseContainerSupport    : JVM lit les cgroups (CPU/RAM) du container, pas du host.
# -XX:MaxRAMPercentage=75.0   : laisse 25 % de RAM au système (ex: 768 Mo sur 1 Go).
# -XX:+ExitOnOutOfMemoryError : tue proprement le container au lieu de le laisser zombie.
# -Djava.security.egd=...     : accélère la génération de nombres aléatoires (JWT).
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
