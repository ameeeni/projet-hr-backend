pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = 'ichahbani'
        IMAGE_NAME      = "${DOCKER_REGISTRY}/hr-backend"
        SONAR_HOST_URL  = 'http://host.docker.internal:9000'
        MAVEN_OPTS      = '-Xmx512m -XX:+TieredCompilation -XX:TieredStopAtLevel=1'
    }

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    parameters {
        booleanParam(name: 'SKIP_SONAR',    defaultValue: false, description: 'Ignorer SonarQube')
        booleanParam(name: 'PUSH_TO_REGISTRY', defaultValue: false, description: 'Publier sur Docker Hub')
        booleanParam(name: 'DEPLOY',        defaultValue: false, description: 'Déployer')
    }

    stages {

        stage('1 — Checkout') {
            steps {
                script {
                    def commit = env.GIT_COMMIT ?: 'nogit'
                    env.SHORT_COMMIT = commit.length() > 7 ? commit.substring(0, 7) : commit
                    env.IMAGE_TAG    = "${env.BUILD_NUMBER}-${env.SHORT_COMMIT}"
                    env.BRANCH       = (env.GIT_BRANCH ?: 'main').replaceAll('origin/', '')
                }
                echo "Build #${env.BUILD_NUMBER} | Branch: ${env.BRANCH} | Tag: ${env.IMAGE_TAG}"
            }
        }

        stage('2 — Build Maven') {
            steps {
                sh 'mvn clean compile -s mvn-settings.xml -B -q'
            }
        }

        stage('3 — Tests & Coverage') {
            steps {
                sh 'mvn verify -s mvn-settings.xml -B'
            }
            post {
                always {
                    junit(testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true)
                    archiveArtifacts(artifacts: 'target/*.jar', fingerprint: true, allowEmptyArchive: true)
                }
            }
        }

        stage('4 — SonarQube') {
            when { not { expression { return params.SKIP_SONAR } } }
            steps {
                withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                    withSonarQubeEnv('SonarQube') {
                        sh '''
                            mvn sonar:sonar -s mvn-settings.xml -B \
                                -Dsonar.token="$SONAR_TOKEN"
                        '''
                    }
                }
            }
        }

        stage('5 — Quality Gate') {
            when { not { expression { return params.SKIP_SONAR } } }
            steps {
                script {
                    withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                        sleep 10
                        def qg = sh(
                            script: '''
                                curl -s -u "$SONAR_TOKEN:" \
                                    "$SONAR_HOST_URL/api/qualitygates/project_status?projectKey=hr-project-backend" \
                                    | grep -o '"status":"[^"]*"' | head -1
                            ''',
                            returnStdout: true
                        ).trim()
                        echo "Quality Gate : ${qg}"
                        if (qg.contains('ERROR')) {
                            error("Quality Gate FAILED")
                        }
                    }
                }
            }
        }

        stage('6 — Docker Build') {
            steps {
                sh """
                    docker build \
                        --build-arg BUILD_DATE=\$(date -u +%Y-%m-%dT%H:%M:%SZ) \
                        --build-arg VCS_REF=${env.SHORT_COMMIT} \
                        -t ${IMAGE_NAME}:${env.IMAGE_TAG} \
                        -t ${IMAGE_NAME}:latest \
                        .
                """
            }
        }

        stage('7 — Docker Push') {
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
                    sh '''
                        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                        docker push ''' + IMAGE_NAME + ''':''' + env.IMAGE_TAG + '''
                        docker push ''' + IMAGE_NAME + ''':latest
                        docker logout
                    '''
                }
            }
        }

        stage('8 — Deploy') {
            when {
                allOf {
                    expression { return env.BRANCH == 'main' }
                    expression { return params.PUSH_TO_REGISTRY }
                    expression { return params.DEPLOY }
                }
            }
            steps {
                withCredentials([
                    string(credentialsId: 'db-password',   variable: 'DB_PASSWORD'),
                    string(credentialsId: 'jwt-secret',    variable: 'JWT_SECRET'),
                    file(credentialsId:   'gcp-service-account', variable: 'GCP_KEY_FILE')
                ]) {
                    sh """
                        cp "\$GCP_KEY_FILE" ./gcp-key.json
                        DOCKER_REGISTRY=${DOCKER_REGISTRY} IMAGE_TAG=${env.IMAGE_TAG} \
                        JWT_SECRET="\$JWT_SECRET" DB_PASSWORD="\$DB_PASSWORD" \
                        GCP_KEY_PATH=./gcp-key.json \
                        docker compose -f docker-compose.prod.yml up -d --remove-orphans
                        docker image prune -f
                        rm -f ./gcp-key.json
                    """
                }
                echo "Backend déployé — http://localhost:8080"
            }
        }
    }

    post {
        always {
            script {
                try { cleanWs() } catch (ignored) { echo "cleanWs ignoré" }
            }
        }
        success  { echo "Backend pipeline réussie — tag: ${env.IMAGE_TAG ?: 'N/A'}" }
        failure  { echo "Backend pipeline échouée" }
        unstable { echo "Backend pipeline instable" }
    }
}
