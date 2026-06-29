# Docker Setup — HR Backend

## 1. Analyse du projet

### Stack technique
| Composant | Version |
|---|---|
| Spring Boot | 3.4.5 |
| Java | 17 |
| Base de données | PostgreSQL 15 |
| Messaging | Apache Kafka (Confluent 7.5.0) |
| IA | Spring AI 1.1.0 + Vertex AI Gemini 2.0 Flash |
| Sécurité | Spring Security + JWT (jjwt 0.12.6) |
| Monitoring | Actuator + Micrometer + Prometheus + Grafana |
| Docs API | Springdoc OpenAPI (Swagger UI) |

### Port exposé
```
8080  (server.port dans application.yaml)
```

---

## 2. Variables d'environnement

### Requises en production

| Variable | Valeur par défaut | Criticité |
|---|---|---|
| `JWT_SECRET` | clé base64 faible (dev only) | **CRITIQUE** — remplacer obligatoirement |
| `DB_URL` | `jdbc:postgresql://localhost:5432/hr_db` | **CRITIQUE** |
| `DB_USERNAME` | `hr_user` | **CRITIQUE** |
| `DB_PASSWORD` | `hr_password` | **CRITIQUE** |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | **CRITIQUE** |
| `GOOGLE_APPLICATION_CREDENTIALS` | *(absent)* | **CRITIQUE** — requis pour Gemini |

### Optionnelles

| Variable | Valeur par défaut |
|---|---|
| `JWT_EXPIRATION` | `3600000` (1 heure en ms) |
| `SPRING_PROFILES_ACTIVE` | `dev` |
| `SPRING_AI_VERTEX_AI_GEMINI_PROJECT_ID` | `gen-lang-client-0640135018` (hardcodé dans YAML) |
| `SPRING_AI_VERTEX_AI_GEMINI_LOCATION` | `us-central1` (hardcodé dans YAML) |

> **Note :** `project-id` et `location` Vertex AI sont actuellement hardcodés dans `application.yaml`.
> Ils peuvent être overridés sans modifier le code via les variables d'environnement Spring Boot ci-dessus.

### Fichier secret à externaliser

| Fichier | Raison | Solution |
|---|---|---|
| `gcp-key.json` | Credentials GCP — ne doit jamais être dans l'image | Monté en volume (`-v ./gcp-key.json:/app/secrets/gcp-key.json:ro`) |

---

## 3. Dockerfile — Changements apportés

### Avant → Après

| # | Avant | Après | Raison |
|---|---|---|---|
| 1 | `2>/dev/null \|\| true` | Supprimé | Cachait les erreurs réseau silencieusement |
| 2 | `-q` sur les commandes Maven | Remplacé par `--no-transfer-progress` | Garde les logs d'erreur, supprime seulement la barre de progression |
| 3 | Absent | `-XX:+ExitOnOutOfMemoryError` | Tue proprement le container sur OOM au lieu de le laisser zombie |
| 4 | 2 `ARG` | 3 `ARG` (ajout `IMAGE_VERSION`) | Traçabilité de la version dans les labels OCI |
| 5 | Absent | Création de `/app/secrets/` | Dossier dédié pour le mount du fichier GCP |

### Dockerfile final

```dockerfile
# Stage 1 : Build
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY pom.xml mvn-settings.xml ./
RUN mvn dependency:go-offline -s mvn-settings.xml --no-transfer-progress
COPY src ./src
RUN mvn package -DskipTests -s mvn-settings.xml --no-transfer-progress

# Stage 2 : Runtime (JRE seul ~90 Mo vs JDK ~330 Mo)
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
RUN addgroup -S hrgroup && adduser -S hruser -G hrgroup
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
RUN chown hruser:hrgroup app.jar
RUN mkdir -p /app/secrets && chown hruser:hrgroup /app/secrets
USER hruser
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
```

### Explication des flags JVM

| Flag | Rôle |
|---|---|
| `-XX:+UseContainerSupport` | La JVM lit les cgroups du container (CPU/RAM) et non ceux du host |
| `-XX:MaxRAMPercentage=75.0` | La JVM utilise max 75 % de la RAM allouée au container |
| `-XX:+ExitOnOutOfMemoryError` | Crash propre sur OOM — Kubernetes redémarre le pod automatiquement |
| `-Djava.security.egd=...` | Accélère la génération aléatoire (entropie) — important pour JWT |

---

## 4. .dockerignore — Changements apportés

| # | Changement | Raison |
|---|---|---|
| 1 | Supprimé `!target/*.jar` | Exception inutile : le builder ne copie jamais depuis `target/` du host |
| 2 | Ajouté `monitoring/` | Inutile pour le build de l'image |
| 3 | Ajouté `.claude/` | Dossier interne à l'outil de dev |
| 4 | Ajouté `Dockerfile` | Convention : ne pas inclure le Dockerfile dans le contexte build |
| 5 | Ajouté `gcp-key.json` | Sécurité : ne jamais embarquer ce fichier dans l'image |

---

## 5. Commandes Docker

### Build

```bash
# Dev local (rapide)
docker build -t hr-backend:local .

# Avec métadonnées complètes
docker build \
  --build-arg BUILD_DATE=$(date -u +%Y-%m-%dT%H:%M:%SZ) \
  --build-arg VCS_REF=$(git rev-parse --short HEAD) \
  --build-arg IMAGE_VERSION=0.0.1-SNAPSHOT \
  -t hr-backend:latest \
  -t hr-backend:$(git rev-parse --short HEAD) \
  .

# Avec registry Docker Hub
docker build \
  --build-arg BUILD_DATE=$(date -u +%Y-%m-%dT%H:%M:%SZ) \
  --build-arg VCS_REF=$(git rev-parse --short HEAD) \
  -t ichahbani/hr-backend:latest \
  .
```

### Run

```bash
# Sans Vertex AI (dev — postgres/kafka lancés séparément)
docker run -d \
  --name hr-backend \
  -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/hr_db \
  -e DB_USERNAME=hr_user \
  -e DB_PASSWORD=hr_password \
  -e KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092 \
  -e JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970 \
  hr-backend:local

# Avec Vertex AI (fichier gcp-key.json requis)
docker run -d \
  --name hr-backend \
  -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/hr_db \
  -e DB_USERNAME=hr_user \
  -e DB_PASSWORD=hr_password \
  -e KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092 \
  -e JWT_SECRET=<clé-base64-256-bits> \
  -e GOOGLE_APPLICATION_CREDENTIALS=/app/secrets/gcp-key.json \
  -e SPRING_AI_VERTEX_AI_GEMINI_PROJECT_ID=gen-lang-client-0640135018 \
  -e SPRING_AI_VERTEX_AI_GEMINI_LOCATION=us-central1 \
  -v $(pwd)/gcp-key.json:/app/secrets/gcp-key.json:ro \
  hr-backend:local

# Stack complète (prod)
export IMAGE_TAG=latest
export DOCKER_REGISTRY=ichahbani
export JWT_SECRET=<clé>
export DB_PASSWORD=<mot-de-passe>
export GCP_KEY_PATH=./gcp-key.json
docker compose -f docker-compose.prod.yml up -d
```

---

## 6. Tests de validation

### Santé du container
```bash
# Statut healthcheck
docker ps --format "table {{.Names}}\t{{.Status}}"

# Logs en temps réel
docker logs -f hr-backend
```

### Endpoints à valider

| Endpoint | Méthode | Attendu |
|---|---|---|
| `/actuator/health` | GET | `{"status":"UP"}` |
| `/actuator/prometheus` | GET | Métriques au format Prometheus |
| `/swagger-ui.html` | GET (navigateur) | Interface Swagger |
| `/actuator/info` | GET | Infos de l'application |

```bash
# Health
curl http://localhost:8080/actuator/health

# Métriques
curl http://localhost:8080/actuator/prometheus | grep "http_server"

# Test JWT (adapter le chemin selon le contrôleur)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
```

### Vérifications image

```bash
# Taille (attendu ~200-250 Mo)
docker images hr-backend

# Utilisateur non-root (attendu : hruser)
docker inspect hr-backend --format '{{.Config.User}}'

# Scanner les vulnérabilités (si Trivy installé)
trivy image hr-backend:local
```

---

## 7. Points ouverts

- [ ] **`project-id` Vertex AI hardcodé** dans `application.yaml` — à externaliser si multi-env
- [ ] **`gcp-key.json`** disponible localement pour les tests Gemini ?
- [ ] **Profil Spring sans IA** — désactiver Gemini via env var pour tester sans GCP ?

---

## 8. Prochaine étape : Kubernetes

Fichiers à créer pour le déploiement K8s :
- `k8s/deployment.yaml` — Deployment + ressources (requests/limits)
- `k8s/service.yaml` — ClusterIP ou LoadBalancer
- `k8s/configmap.yaml` — Variables d'environnement non sensibles
- `k8s/secret.yaml` — `JWT_SECRET`, `DB_PASSWORD`, `gcp-key.json`
- `k8s/ingress.yaml` — Exposition externe
- `k8s/hpa.yaml` — Autoscaling horizontal (optionnel)
