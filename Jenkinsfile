// ─────────────────────────────────────────────────────────────────────────────
//  HR Project — Pipeline CI/CD Jenkins
//  Prérequis Jenkins (plugins) :
//    - Pipeline, Git, Credentials Binding
//    - SonarQube Scanner, Sonar Quality Gates
//    - JaCoCo, HTML Publisher, Coverage
//    - Docker Pipeline
//  Prérequis Tools (Manage Jenkins > Global Tool Configuration) :
//    - Maven  : nom "Maven-3.9"
//    - JDK    : nom "JDK-17"
//    - NodeJS : nom "Node-22"
//  Credentials Jenkins (Manage Jenkins > Credentials) :
//    - jwt-secret           → Secret text       → valeur du JWT_SECRET
//    - sonarqube-token      → Secret text       → token SonarQube
//    - dockerhub-credentials→ Username/Password → Docker Hub
//    - github-credentials   → Username/Password → GitHub (si repos privés)
//    - db-password          → Secret text       → mot de passe PostgreSQL prod
//    - gcp-service-account  → Secret file       → clé JSON Vertex AI
//  SonarQube (Manage Jenkins > Configure System > SonarQube servers) :
//    - Name : SonarQube | URL : http://localhost:9000 | Token : sonarqube-token
//    - Webhook dans SonarQube : http://<jenkins-url>/sonarqube-webhook/
// ─────────────────────────────────────────────────────────────────────────────

pipeline {
    agent any

    // ── Variables d'environnement globales ────────────────────────────────────
    environment {
        // Docker Hub
        DOCKER_REGISTRY = 'ichahbani'
        BACKEND_IMAGE   = "${DOCKER_REGISTRY}/hr-backend"
        FRONTEND_IMAGE  = "${DOCKER_REGISTRY}/hr-frontend"

        // Credentials — disponibles comme variables d'env dans tous les sh
        JWT_SECRET  = credentials('jwt-secret')
        SONAR_TOKEN = credentials('sonarqube-token')

        // SonarQube
        SONAR_HOST_URL = 'http://localhost:9000'

        // Maven
        MAVEN_OPTS = '-Xmx512m -XX:+TieredCompilation -XX:TieredStopAtLevel=1'
    }

    // ── Outils installés dans Jenkins ─────────────────────────────────────────
    tools {
        maven  'Maven-3.9'
        jdk    'JDK-17'
        nodejs 'Node-22'
    }

    // ── Options globales ──────────────────────────────────────────────────────
    options {
        timestamps()
        timeout(time: 60, unit: 'MINUTES')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '5'))
        ansiColor('xterm')
    }

    // ── Paramètres de lancement manuel ────────────────────────────────────────
    parameters {
        string(
            name: 'FRONTEND_REPO',
            defaultValue: 'https://github.com/ameeeni/hr-frontend-project.git',
            description: 'URL Git du dépôt frontend Angular'
        )
        string(
            name: 'FRONTEND_BRANCH',
            defaultValue: 'main',
            description: 'Branche frontend à construire'
        )
        booleanParam(
            name: 'PUSH_TO_REGISTRY',
            defaultValue: true,
            description: 'Publier les images sur Docker Hub'
        )
        booleanParam(
            name: 'DEPLOY',
            defaultValue: true,
            description: 'Déployer automatiquement (seulement sur main)'
        )
        booleanParam(
            name: 'SKIP_SONAR',
            defaultValue: false,
            description: 'Ignorer SonarQube (usage développement uniquement)'
        )
    }

    // ═════════════════════════════════════════════════════════════════════════
    stages {

        // ── STAGE 1 : CHECKOUT ───────────────────────────────────────────────
        stage('1 — Checkout') {
            steps {
                script {
                    def commit      = env.GIT_COMMIT ?: 'nogit'
                    env.SHORT_COMMIT = commit.length() > 7 ? commit.substring(0, 7) : commit
                    env.IMAGE_TAG   = "${env.BUILD_NUMBER}-${env.SHORT_COMMIT}"
                    env.BRANCH      = (env.GIT_BRANCH ?: 'main').replaceAll('origin/', '')
                }

                echo "╔══════════════════════════════════════╗"
                echo "║  HR Project — Pipeline CI/CD          ║"
                echo "╠══════════════════════════════════════╣"
                echo "║  Branch  : ${env.BRANCH}"
                echo "║  Commit  : ${env.SHORT_COMMIT}"
                echo "║  Tag     : ${env.IMAGE_TAG}"
                echo "║  Build   : #${env.BUILD_NUMBER}"
                echo "╚══════════════════════════════════════╝"

                // Checkout du frontend dans un sous-dossier
                dir('frontend') {
                    git(
                        url:           params.FRONTEND_REPO,
                        branch:        params.FRONTEND_BRANCH,
                        credentialsId: 'github-credentials'
                    )
                }
            }
        }

        // ── STAGES 2+3 et 4+5 en PARALLÈLE ──────────────────────────────────
        stage('Build & Test') {
            parallel {

                // ── STAGE 2+3 : BACKEND BUILD + TESTS ────────────────────────
                stage('2+3 — Backend Build & Tests') {
                    stages {

                        stage('2 — Backend Build Maven') {
                            steps {
                                sh 'mvn clean compile -s mvn-settings.xml -B -q'
                            }
                        }

                        stage('3 — Backend Tests & Coverage') {
                            steps {
                                sh 'mvn verify -s mvn-settings.xml -B'
                            }
                            post {
                                always {
                                    // Rapport JUnit
                                    junit(
                                        testResults:       '**/target/surefire-reports/*.xml',
                                        allowEmptyResults: true
                                    )
                                    // Rapport JaCoCo
                                    recordCoverage(
                                        tools: [[
                                            parser:  'JACOCO',
                                            pattern: '**/target/site/jacoco/jacoco.xml'
                                        ]],
                                        id:                  'backend-coverage',
                                        name:                'Backend — JaCoCo',
                                        sourceCodeRetention: 'EVERY_BUILD',
                                        qualityGates: [[
                                            threshold: 80.0,
                                            metric:    'LINE',
                                            baseline:  'PROJECT'
                                        ]]
                                    )
                                    // Archiver le JAR
                                    archiveArtifacts(
                                        artifacts:    'target/*.jar',
                                        fingerprint:  true,
                                        allowEmptyArchive: true
                                    )
                                }
                            }
                        }
                    }
                }

                // ── STAGE 4+5 : FRONTEND INSTALL + TESTS ─────────────────────
                stage('4+5 — Frontend Install & Tests') {
                    stages {

                        stage('4 — Frontend npm install') {
                            steps {
                                dir('frontend') {
                                    sh 'npm ci --prefer-offline'
                                }
                            }
                        }

                        stage('5 — Frontend Tests & Coverage') {
                            steps {
                                dir('frontend') {
                                    sh 'npm run test:coverage -- --passWithNoTests'
                                }
                            }
                            post {
                                always {
                                    publishHTML([
                                        allowMissing:         true,
                                        alwaysLinkToLastBuild: true,
                                        keepAll:              true,
                                        reportDir:            'frontend/coverage',
                                        reportFiles:          'index.html',
                                        reportName:           'Frontend Coverage',
                                        reportTitles:         'Angular Coverage'
                                    ])
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── STAGE 6 : SONARQUBE ANALYSIS ─────────────────────────────────────
        stage('6 — SonarQube Analysis') {
            when {
                not { expression { return params.SKIP_SONAR } }
            }
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh '''
                        mvn sonar:sonar -s mvn-settings.xml -B \
                            -Dsonar.token="$SONAR_TOKEN" \
                            -Dsonar.javascript.lcov.reportPaths=frontend/coverage/lcov.info
                    '''
                }
            }
        }

        stage('6b — Quality Gate') {
            when {
                not { expression { return params.SKIP_SONAR } }
            }
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        // ── STAGE 7 : DOCKER BUILD ────────────────────────────────────────────
        stage('7 — Docker Build') {
            parallel {

                stage('Build Backend Image') {
                    steps {
                        sh """
                            docker build \
                                --build-arg BUILD_DATE=\$(date -u +%Y-%m-%dT%H:%M:%SZ) \
                                --build-arg VCS_REF=${env.SHORT_COMMIT} \
                                -t ${BACKEND_IMAGE}:${env.IMAGE_TAG} \
                                -t ${BACKEND_IMAGE}:latest \
                                .
                        """
                    }
                }

                stage('Build Frontend Image') {
                    steps {
                        dir('frontend') {
                            sh """
                                docker build \
                                    --build-arg BUILD_DATE=\$(date -u +%Y-%m-%dT%H:%M:%SZ) \
                                    --build-arg VCS_REF=${env.SHORT_COMMIT} \
                                    -t ${FRONTEND_IMAGE}:${env.IMAGE_TAG} \
                                    -t ${FRONTEND_IMAGE}:latest \
                                    .
                            """
                        }
                    }
                }
            }
        }

        // ── STAGE 8 : DOCKER PUSH ─────────────────────────────────────────────
        stage('8 — Docker Push') {
            when {
                allOf {
                    expression { return env.BRANCH == 'main' }
                    expression { return params.PUSH_TO_REGISTRY }
                }
            }
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh 'echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin'

                    sh """
                        docker push ${BACKEND_IMAGE}:${env.IMAGE_TAG}
                        docker push ${BACKEND_IMAGE}:latest
                        docker push ${FRONTEND_IMAGE}:${env.IMAGE_TAG}
                        docker push ${FRONTEND_IMAGE}:latest
                    """
                }
            }
            post {
                always {
                    sh 'docker logout || true'
                }
            }
        }

        // ── STAGE 9 : DEPLOY ──────────────────────────────────────────────────
        stage('9 — Deploy') {
            when {
                allOf {
                    expression { return env.BRANCH == 'main' }
                    expression { return params.PUSH_TO_REGISTRY }
                    expression { return params.DEPLOY }
                }
            }
            steps {
                withCredentials([
                    string(credentialsId: 'db-password',        variable: 'DB_PASSWORD'),
                    string(credentialsId: 'jwt-secret',         variable: 'JWT_SECRET_DEPLOY'),
                    file(credentialsId:   'gcp-service-account', variable: 'GCP_KEY_FILE')
                ]) {
                    sh """
                        cp "\$GCP_KEY_FILE" ./gcp-key.json

                        DOCKER_REGISTRY=${DOCKER_REGISTRY} \
                        IMAGE_TAG=${env.IMAGE_TAG} \
                        JWT_SECRET="\$JWT_SECRET_DEPLOY" \
                        DB_PASSWORD="\$DB_PASSWORD" \
                        GCP_KEY_PATH=./gcp-key.json \
                        docker compose -f docker-compose.prod.yml pull

                        DOCKER_REGISTRY=${DOCKER_REGISTRY} \
                        IMAGE_TAG=${env.IMAGE_TAG} \
                        JWT_SECRET="\$JWT_SECRET_DEPLOY" \
                        DB_PASSWORD="\$DB_PASSWORD" \
                        GCP_KEY_PATH=./gcp-key.json \
                        docker compose -f docker-compose.prod.yml up -d --remove-orphans

                        docker image prune -f
                        rm -f ./gcp-key.json
                    """
                }

                echo "Déploiement réussi — http://localhost:80"
            }
        }
    }

    // ── POST-PIPELINE ─────────────────────────────────────────────────────────
    post {
        always {
            sh 'docker logout || true'
            cleanWs()
        }
        success {
            echo "Pipeline réussie — ${env.BACKEND_IMAGE}:${env.IMAGE_TAG}"
        }
        failure {
            echo "Pipeline échouée — Vérifier les logs ci-dessus"
        }
        unstable {
            echo "Pipeline instable — Qualité ou tests à corriger"
        }
    }
}
