# Kubernetes Setup — HR Backend
## Minikube + Helm (Bitnami) + NGINX Ingress

---

## Architecture cible

```
                        ┌─────────────────────────────────────────┐
                        │           Namespace : hr-project          │
                        │                                           │
  hr-backend.local ──► │  NGINX Ingress                            │
  (navigateur / curl)   │      │                                    │
                        │      ▼                                    │
                        │  Service : hr-backend (ClusterIP:8080)   │
                        │      │                                    │
                        │      ▼                                    │
                        │  Deployment : hr-backend                  │
                        │  ┌─────────────────────────────────┐     │
                        │  │ Pod : hr-backend                 │     │
                        │  │  initContainers:                 │     │
                        │  │    wait-for-postgres             │     │
                        │  │    wait-for-kafka                │     │
                        │  │  container: Spring Boot :8080    │     │
                        │  │  volume: gcp-key.json (secret)   │     │
                        │  └─────────────────────────────────┘     │
                        │      │              │                     │
                        │      ▼              ▼                     │
                        │  PostgreSQL      Kafka (KRaft)            │
                        │  (Bitnami Helm) (Bitnami Helm)            │
                        │                                           │
                        │  HPA : 1–3 replicas (CPU 70% / RAM 80%)  │
                        └─────────────────────────────────────────┘
```

---

## Prérequis

| Outil | Version min | Vérification |
|---|---|---|
| Docker | 20+ | `docker --version` |
| Minikube | 1.32+ | `minikube version` |
| kubectl | 1.28+ | `kubectl version --client` |
| Helm | 3.14+ | `helm version` |

---

## Étape 1 — Démarrer Minikube

```powershell
# Démarrer avec suffisamment de ressources pour Postgres + Kafka + Spring Boot
minikube start --cpus=4 --memory=6g --disk-size=20g

# Vérifier que le cluster est prêt
minikube status
kubectl get nodes
```

---

## Étape 2 — Activer les addons Minikube

```powershell
# NGINX Ingress Controller (expose l'app vers l'extérieur)
minikube addons enable ingress

# Metrics Server (requis pour le HPA — autoscaling)
minikube addons enable metrics-server

# Vérifier
minikube addons list | grep -E "ingress|metrics"
```

---

## Étape 3 — Créer le Namespace

```powershell
kubectl apply -f k8s/00-namespace.yaml

# Vérifier
kubectl get namespace hr-project
```

---

## Étape 4 — Ajouter le repo Helm Bitnami

```powershell
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update
```

---

## Étape 5 — Déployer PostgreSQL via Helm

```powershell
helm install hr-postgres bitnami/postgresql `
  --namespace hr-project `
  -f k8s/helm/postgresql-values.yaml

# Attendre que PostgreSQL soit prêt (pod Running)
kubectl get pods -n hr-project -w
# Ctrl+C quand hr-postgres-postgresql-0 est en état "Running"

# Vérifier le service créé
kubectl get svc -n hr-project
# → hr-postgres-postgresql   ClusterIP   <IP>   5432/TCP
```

---

## Étape 6 — Déployer Kafka via Helm

```powershell
helm install hr-kafka bitnami/kafka `
  --namespace hr-project `
  -f k8s/helm/kafka-values.yaml

# Attendre que Kafka soit prêt
kubectl get pods -n hr-project -w
# Ctrl+C quand hr-kafka-controller-0 est "Running"

# Vérifier le service
kubectl get svc -n hr-project
# → hr-kafka   ClusterIP   <IP>   9092/TCP
```

---

## Étape 7 — Construire et charger l'image Docker dans Minikube

```powershell
# Option A : charger l'image locale directement dans Minikube (sans registry)
docker build -t ichahbani/hr-backend:latest .
minikube image load ichahbani/hr-backend:latest

# Vérifier que l'image est disponible dans Minikube
minikube image ls | grep hr-backend

# Option B : pousser sur Docker Hub (si tu veux que d'autres puissent pull)
# docker login
# docker push ichahbani/hr-backend:latest
# (Dans ce cas, garder imagePullPolicy: IfNotPresent dans deployment.yaml)
```

---

## Étape 8 — Créer les Secrets

### Secret applicatif (JWT + DB)

```powershell
# Éditer k8s/02-secret.yaml avec tes vraies valeurs, puis :
kubectl apply -f k8s/02-secret.yaml

# OU créer directement en ligne de commande (recommandé : pas de fichier avec secrets)
kubectl create secret generic hr-backend-secret `
  --from-literal=JWT_SECRET="404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970" `
  --from-literal=DB_USERNAME="hr_user" `
  --from-literal=DB_PASSWORD="hr_password" `
  --namespace hr-project
```

### Secret GCP (Vertex AI Gemini)

```powershell
# Créer le secret depuis le fichier gcp-key.json local
# (Le fichier n'est jamais embarqué dans l'image Docker)
kubectl create secret generic gcp-credentials `
  --from-file=gcp-key.json=./gcp-key.json `
  --namespace hr-project

# Si tu n'as pas encore le fichier GCP, skip cette étape :
# le Deployment a optional: true sur le volume, il démarrera quand même.
```

---

## Étape 9 — Déployer l'application

```powershell
# ConfigMap (variables non-sensibles)
kubectl apply -f k8s/01-configmap.yaml

# Deployment (pods Spring Boot)
kubectl apply -f k8s/03-deployment.yaml

# Service (ClusterIP)
kubectl apply -f k8s/04-service.yaml

# Ingress (exposition externe via NGINX)
kubectl apply -f k8s/05-ingress.yaml

# HPA (autoscaling)
kubectl apply -f k8s/06-hpa.yaml

# OU tout en une commande (kubectl respecte l'ordre alphabétique des fichiers)
kubectl apply -f k8s/
```

---

## Étape 10 — Configurer /etc/hosts (Windows)

```powershell
# Obtenir l'IP de Minikube
minikube ip
# Exemple : 192.168.49.2

# Ajouter dans C:\Windows\System32\drivers\etc\hosts (en admin) :
# 192.168.49.2  hr-backend.local
```

---

## Vérifications et Tests

### Statut général

```powershell
# Voir tous les pods du namespace
kubectl get all -n hr-project

# Détail d'un pod (si problème)
kubectl describe pod -l app=hr-backend -n hr-project

# Logs de l'application
kubectl logs -l app=hr-backend -n hr-project --follow

# Logs des init containers (si le pod ne démarre pas)
kubectl logs <nom-du-pod> -c wait-for-postgres -n hr-project
kubectl logs <nom-du-pod> -c wait-for-kafka -n hr-project
```

### Tester l'application

```powershell
# Health via Ingress (après config /etc/hosts)
curl http://hr-backend.local/actuator/health

# Health via port-forward (sans Ingress, pour debug)
kubectl port-forward svc/hr-backend 8080:8080 -n hr-project
curl http://localhost:8080/actuator/health

# Métriques Prometheus
curl http://hr-backend.local/actuator/prometheus | Select-String "http_server"

# Swagger UI dans le navigateur
# http://hr-backend.local/swagger-ui.html
```

### Vérifier le HPA

```powershell
# Voir l'état de l'autoscaler
kubectl get hpa -n hr-project

# Résultat attendu :
# NAME             REFERENCE               TARGETS          MINPODS  MAXPODS  REPLICAS
# hr-backend-hpa   Deployment/hr-backend   12%/70%, 8%/80%   1        3        1
```

### Vérifier l'Ingress

```powershell
kubectl get ingress -n hr-project
# HOSTS              ADDRESS          PORTS   AGE
# hr-backend.local   192.168.49.2     80      1m
```

---

## Commandes utiles au quotidien

```powershell
# Redémarrer le deployment (forcer un re-pull de l'image)
kubectl rollout restart deployment/hr-backend -n hr-project

# Voir l'historique des déploiements
kubectl rollout history deployment/hr-backend -n hr-project

# Rollback vers la version précédente
kubectl rollout undo deployment/hr-backend -n hr-project

# Scaler manuellement (ex: passer à 2 replicas)
kubectl scale deployment hr-backend --replicas=2 -n hr-project

# Voir les variables d'environnement injectées dans le pod
kubectl exec -it <nom-du-pod> -n hr-project -- env | sort

# Accéder au shell du pod (debug)
kubectl exec -it <nom-du-pod> -n hr-project -- sh

# Supprimer tout et recommencer
kubectl delete namespace hr-project
helm uninstall hr-postgres -n hr-project
helm uninstall hr-kafka -n hr-project
```

---

## Mise à jour de l'image (workflow itératif)

```powershell
# 1. Rebuilder l'image
docker build -t ichahbani/hr-backend:latest .

# 2. Recharger dans Minikube
minikube image load ichahbani/hr-backend:latest

# 3. Forcer le redémarrage (Minikube ne re-pull pas automatiquement avec IfNotPresent)
kubectl rollout restart deployment/hr-backend -n hr-project

# 4. Suivre le déploiement
kubectl rollout status deployment/hr-backend -n hr-project
```

---

## Résolution des problèmes fréquents

| Symptôme | Cause probable | Solution |
|---|---|---|
| Pod en `CrashLoopBackOff` | App ne démarre pas | `kubectl logs <pod> -n hr-project` |
| Init container bloqué | Postgres ou Kafka pas prêt | `kubectl get pods -n hr-project` — attendre |
| `ImagePullBackOff` | Image introuvable | `minikube image load ichahbani/hr-backend:latest` |
| `OOMKilled` | Pas assez de RAM | Augmenter `limits.memory` dans deployment.yaml |
| Ingress ne répond pas | /etc/hosts mal configuré | Vérifier `minikube ip` et le fichier hosts |
| HPA bloqué à `<unknown>` | metrics-server absent | `minikube addons enable metrics-server` |
| Kafka connection refused | Service name incorrect | `kubectl get svc -n hr-project` — vérifier le nom |

---

## Structure finale des fichiers

```
projet-hr-backend/
├── Dockerfile                   ← Image multi-stage optimisée
├── .dockerignore                ← Contexte build propre
├── k8s/
│   ├── 00-namespace.yaml        ← Namespace hr-project
│   ├── 01-configmap.yaml        ← Variables non-sensibles
│   ├── 02-secret.yaml           ← Template secrets (ne pas committer avec vraies valeurs)
│   ├── 03-deployment.yaml       ← Pods Spring Boot + probes + ressources
│   ├── 04-service.yaml          ← ClusterIP:8080
│   ├── 05-ingress.yaml          ← NGINX → hr-backend.local
│   ├── 06-hpa.yaml              ← Autoscaling 1-3 pods
│   └── helm/
│       ├── postgresql-values.yaml
│       └── kafka-values.yaml
├── DOCKER_SETUP.md              ← Guide Docker
└── K8S_SETUP.md                 ← Ce fichier
```
