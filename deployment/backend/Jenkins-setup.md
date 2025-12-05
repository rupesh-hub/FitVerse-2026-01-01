pipeline {
agent any

    environment {
        DOCKER_HUB_CREDENTIALS_ID = 'docker-hub-credentials'
        DOCKER_REPO = 'rupesh1997'
        EMAIL_RECIPIENTS = 'devops-team@fitverse.com'
        PROJECT_VERSION = ''
    }

    stages {

        stage("Checkout repository"){
            steps{
                git url: 'https://github.com/rupesh-hub/FirVerse.git', branch: 'main'
            }
        }

        stage("Build project") {
            steps {
                echo 'Building the project JAR...'
                sh '''
                    cd backend
                    ./mvnw clean package
                '''
            }
        }

        stage("Unit tests") {
            steps {
                echo 'Running unit tests...'
                sh '''
                    cd backend
                    ./mvnw clean test
                    '''
                junit '**/target/surefire-reports/TEST-*.xml'
            }
        }

       stage("Extract project version") {
           steps {
               script {
                   env.PROJECT_VERSION = sh(
                       script: """
                           cd backend
                           mvn help:evaluate -Dexpression=project.version -q -DforceStdout
                       """,
                       returnStdout: true
                   ).trim()

                   echo "Extracted project version: ${env.PROJECT_VERSION}"
               }
           }
       }

        stage("Build docker image") {
            steps {
                echo 'Building Docker image...'
                sh """
                       docker build -t ${DOCKER_REPO}/fit-verse-backend:${PROJECT_VERSION} -t ${DOCKER_REPO}/fit-verse-backend:latest -f ./docker/backend/Dockerfile .
                    """
            }
        }

        stage("Login to docker hub") {
            steps {
                echo 'Logging into Docker Hub...'
                withCredentials([usernamePassword(
                    credentialsId: env.DOCKER_HUB_CREDENTIALS_ID,
                    passwordVariable: 'DOCKER_PASSWORD',
                    usernameVariable: 'DOCKER_USER'
                )]) {
                    sh "docker login -u ${DOCKER_USER} -p ${DOCKER_PASSWORD}"
                }
            }
        }

        stage("Push image to docker hub") {
            steps {
                echo 'Pushing Docker images...'
                sh "docker push ${DOCKER_REPO}/fit-verse-backend:${PROJECT_VERSION}"
                sh "docker push ${DOCKER_REPO}/fit-verse-backend:latest"
            }
        }

        stage("Deployment") {
            steps {
                echo 'Initiating deployment to staging/production...'
                
            }
        }

    }

    // Post-build actions: Email notification setup
    post {
        always {
            // Clean up workspace to save disk space
            cleanWs()
        }
        success {
            mail(
                to: env.EMAIL_RECIPIENTS,
                subject: "SUCCESS: FitVerse Backend Pipeline #${currentBuild.number}",
                body: "The FitVerse Backend build and deployment was successful! Version: ${env.PROJECT_VERSION}\n" +
                      "View details: ${env.BUILD_URL}"
            )
        }
        failure {
             mail(
                to: env.EMAIL_RECIPIENTS,
                subject: "FAILURE: FitVerse Backend Pipeline #${currentBuild.number}",
                body: "The FitVerse Backend pipeline failed!\n" +
                      "Stage: ${STAGE_NAME}\n" +
                      "View details: ${env.BUILD_URL}",
                // Send the email even if the mail server configuration itself is unstable
                failLogOnly: true
            )
        }
        unstable {
            mail(
                to: env.EMAIL_RECIPIENTS,
                subject: "UNSTABLE: FitVerse Backend Pipeline #${currentBuild.number}",
                body: "The FitVerse Backend pipeline finished unstable (e.g., tests failed or skipped, but build succeeded).\n" +
                      "View details: ${env.BUILD_URL}"
            )
        }
    }
}