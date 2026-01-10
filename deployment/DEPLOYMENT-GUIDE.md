# FitVerse: Complete CI/CD & Deployment Guide (2026)
## From Zero to Production - Step by Step

---

## Table of Contents
1. [Quick Start (5 minutes)](#quick-start)
2. [Part 1: Install & Setup Jenkins](#part-1-jenkins-setup)
3. [Part 2: Configure Jenkins Tools & Credentials](#part-2-jenkins-configuration)
4. [Part 3: SonarQube Setup](#part-3-sonarqube-setup)
5. [Part 4: Create CI/CD Pipelines](#part-4-cicd-pipelines)
6. [Part 5: Kubernetes Deployment](#part-5-kubernetes-deployment)
7. [Part 6: Testing & Troubleshooting](#part-6-testing)
8. [Reference: Commands & Checklists](#reference)

---

## Quick Start (5 minutes)

**Already have Jenkins/SonarQube running? Jump to:**
- [Create CI Pipeline](#ci-pipeline-setup)
- [Create CD Pipeline](#cd-pipeline-setup)
- [Deploy to Kubernetes](#helm-deployment)

---

# PART 1: JENKINS SETUP (UBUNTU/LINUX)

## Prerequisites
- Ubuntu 20.04 LTS or later
- 4GB+ RAM, 20GB+ disk space
- Sudo access
- Internet connection

## Step 1: Update System & Install Java 17

```bash
sudo apt-get update
sudo apt-get upgrade -y
sudo apt-get install -y openjdk-17-jdk openjdk-17-jre
java -version  # Should show Java 17.x.x
```

## Step 2: Install Jenkins

```bash
# Add Jenkins repository
curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io.key | sudo tee /usr/share/keyrings/jenkins-keyring.asc > /dev/null
echo deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] https://pkg.jenkins.io/debian-stable binary/ | sudo tee /etc/apt/sources.list.d/jenkins.list > /dev/null

# Install Jenkins
sudo apt-get update
sudo apt-get install -y jenkins
```

## Step 3: Start Jenkins Service

```bash
sudo systemctl start jenkins
sudo systemctl enable jenkins  # Auto-start on reboot
sudo systemctl status jenkins  # Verify running
```

## Step 4: Unlock Jenkins

```bash
# Get admin password
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```

1. Open browser: `http://localhost:8080`
2. Paste the password
3. Click **Continue**
4. Click **Install suggested plugins** (wait 5-10 minutes)
5. Create admin user:
    - Username: `admin`
    - Password: `<choose-strong-password>`
    - Email: `your-email@gmail.com`
6. Click **Save and Continue**
7. Jenkins URL: `http://<your-server-ip>:8080/`
8. Click **Save and Finish**

✅ **Jenkins is now ready!**

---

# PART 2: JENKINS CONFIGURATION

## Installing Required Plugins

Your pipelines need these plugins. Let's install them:

1. Go to **Manage Jenkins** → **Manage Plugins**
2. Click **Available plugins** tab
3. For each plugin below: Search → Check box → Click **Install without restart**

| Plugin Name | Search Term | Purpose |
|---|---|---|
| Pipeline | `Pipeline` | Declarative pipelines |
| Pipeline: Shared Groovy Libraries | `pipeline-model-definition` | Shared libraries |
| Docker | `docker-plugin` | Docker integration |
| Docker Pipeline | `docker-workflow` | Docker in pipelines |
| SonarQube Scanner | `sonar` | Code quality scanning |
| Email Extension | `email-ext` | Email notifications |
| GitHub | `github` | GitHub integration |
| Kubernetes | `kubernetes` | Kubernetes support |
| Kubernetes CLI | `kubernetes-cli` | kubectl commands |
| Helm | `helm` | Helm deployment |

**After installing all plugins:**
- Go to **Manage Jenkins** → **System Configuration**
- Restart if prompted

---

## Configuring Tools

### Configure Java 17

1. Go to **Manage Jenkins** → **Tools**
2. Find **JDK installations** section
3. Click **Add JDK**
4. Fill in:
    - **Name**: `Java17` (MUST match pipeline exactly)
    - **JAVA_HOME**: Leave empty (auto-detect)
    - Check **Install automatically**
    - Version: Select latest `jdk-17.x.x`
5. Click **Save**

### Configure Maven 3

1. In same **Tools** section
2. Find **Maven installations**
3. Click **Add Maven**
4. Fill in:
    - **Name**: `maven-3` (MUST match pipeline exactly)
    - **MAVEN_HOME**: Leave empty
    - Check **Install automatically**
    - Version: Select latest `3.9.x`
5. Click **Save**

---

## Setting Up Global Credentials

Your pipeline needs 3 credentials. Let's add them:

### Credential 1: GitHub SSH Key

**Step 1: Generate SSH key** (if you don't have one):

```bash
ssh-keygen -t ed25519 -C "jenkins@fitverse"
# Press Enter for default path
# Press Enter for no passphrase
cat ~/.ssh/id_ed25519.pub  # Copy this output
```

**Step 2: Add to GitHub:**
1. GitHub → Settings → SSH and GPG keys
2. Click **New SSH key**
3. Title: `Jenkins FitVerse`
4. Paste the public key
5. Click **Add SSH key**

**Step 3: Add to Jenkins:**
1. Go to **Manage Jenkins** → **Credentials**
2. Click **System** (on left)
3. Click **Global credentials (unrestricted)**
4. Click **+ Add Credentials**
5. Fill in:
    - **Kind**: `SSH Username with private key`
    - **ID**: `git-ssh-cred`
    - **Description**: `GitHub SSH Key for FitVerse`
    - **Username**: `git`
    - **Private Key**: Click **Enter directly**, paste entire content of `~/.ssh/id_ed25519`
6. Click **Create**

### Credential 2: Docker Registry

1. Go to **Manage Jenkins** → **Credentials** → **System** → **Global credentials**
2. Click **+ Add Credentials**
3. Fill in:
    - **Kind**: `Username with password`
    - **Scope**: `Global`
    - **Username**: `your-dockerhub-username`
    - **Password**: `your-dockerhub-token` (Get from DockerHub → Settings → Security)
    - **ID**: `docker-cred`
    - **Description**: `Docker Registry Credentials`
4. Click **Create**

### Credential 3: SonarQube Token

(We'll generate this in Part 3, then add here)

---

## Configuring Shared Libraries

Shared libraries contain reusable pipeline functions.

### Step 1: Create Shared Library Repository

```bash
mkdir -p ~/fitverse-shared-library/vars
cd ~/fitverse-shared-library

# Create directory structure
mkdir -p vars resources/groovy

# Create a sample function
cat > vars/checkoutRepository.groovy << 'EOF'
def call(Map config) {
    checkout([
        $class: 'GitSCM',
        branches: [[name: '*/main']],
        userRemoteConfigs: [[
            url: config.repo_url,
            credentialsId: config.credentials_id
        ]]
    ])
    echo "✓ Repository checked out successfully"
}
EOF

git init
git add .
git commit -m "Initial commit: shared library structure"
git remote add origin https://github.com/your-username/fitverse-shared-library.git
git push -u origin main
```

### Step 2: Configure in Jenkins

1. Go to **Manage Jenkins** → **System**
2. Scroll to **Global Pipeline Libraries**
3. Click **Add**
4. Fill in:
    - **Name**: `shared-lib`
    - **Default version**: `main`
    - **Repository URL**: `https://github.com/your-username/fitverse-shared-library.git`
    - **Credentials**: Select `git-ssh-cred`
5. Click **Save**

---

# PART 3: SONARQUBE SETUP

## Installation Options

### Option A: Docker (Quickest - Recommended)

```bash
docker run -d \
  --name sonarqube \
  -p 9000:9000 \
  --restart unless-stopped \
  sonarqube:lts

# Wait 30 seconds, then verify:
curl -s http://localhost:9000/api/system/status
# Should return: {"status":"UP"}
```

### Option B: Ubuntu Server Installation

```bash
# Install Java 11+ (if not already installed)
sudo apt-get install -y openjdk-17-jdk

# Create sonar user
sudo useradd -m -s /bin/bash sonar

# Download & install SonarQube
cd /opt
sudo wget https://binaries.sonarsource.com/Distribution/sonarqube/sonarqube-10.3.0.82913.zip
sudo unzip sonarqube-10.3.0.82913.zip
sudo mv sonarqube-10.3.0.82913 sonarqube
sudo chown -R sonar:sonar /opt/sonarqube

# Create systemd service
sudo tee /etc/systemd/system/sonarqube.service > /dev/null <<EOF
[Unit]
Description=SonarQube
After=network.target

[Service]
Type=forking
User=sonar
ExecStart=/opt/sonarqube/bin/linux-x86-64/sonar.sh start
ExecStop=/opt/sonarqube/bin/linux-x86-64/sonar.sh stop
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF

# Start service
sudo systemctl daemon-reload
sudo systemctl enable sonarqube
sudo systemctl start sonarqube
sudo systemctl status sonarqube
```

## Access SonarQube

Open browser: `http://localhost:9000` (or `http://<your-server-ip>:9000`)
- **Username**: `admin`
- **Password**: `admin`

### Change Default Password

1. Click top-right **admin** → **My Account**
2. Click **Security** tab
3. Change password to something secure
4. Save

---

## Generate SonarQube Token

**What is a token?** It's like a password that Jenkins uses to authenticate with SonarQube.

### Step 1: Create Token

1. In SonarQube, click top-right **admin** → **My Account**
2. Click **Security** tab
3. Under "Generate Tokens" section:
    - **Token name**: `jenkins-sonar-token`
    - Click **Generate**
4. **Copy the token immediately** (you won't see it again!)

Example token: `squ_abc123def456ghi789jkl012mno345pqr`

### Step 2: Add to Jenkins Credentials

1. Go to **Manage Jenkins** → **Credentials** → **System** → **Global credentials**
2. Click **+ Add Credentials**
3. Fill in:
    - **Kind**: `Secret text`
    - **Secret**: Paste your SonarQube token
    - **ID**: `sonar-token`
    - **Description**: `SonarQube Jenkins Token`
4. Click **Create**

---

## Configure SonarQube in Jenkins

### Step 1: Add SonarQube Server

1. Go to **Manage Jenkins** → **System**
2. Scroll to **SonarQube servers**
3. Click **Add SonarQube**
4. Fill in:
    - **Name**: `SonarQube`
    - **Server URL**: `http://localhost:9000` (or your server IP)
    - **Server authentication token**: Select `sonar-token`
5. Click **Save**

### Step 2: Install SonarQube Scanner Tool

1. Go to **Manage Jenkins** → **Tools**
2. Scroll to **SonarQube Scanner**
3. Click **Add SonarQube Scanner**
4. Fill in:
    - **Name**: `sonar-scanner`
    - Check **Install automatically**
    - **Version**: `Latest`
5. Click **Save**

---

## Setup SonarQube Webhook

Webhooks notify Jenkins when analysis completes.

### Step 1: Create Webhook in SonarQube

1. Go to **Administration** (gear icon bottom-left)
2. Click **Webhooks** under **Configuration**
3. Click **Create**
4. Fill in:
    - **Name**: `jenkins-webhook`
    - **URL**: `http://<your-jenkins-ip>:8080/sonarqube-webhook/`
    - **Secret**: Leave empty
5. Click **Create**

### Step 2: Enable in Jenkins

1. Go to **Manage Jenkins** → **System**
2. Find **SonarQube Servers**
3. Check **Enable SonarQube webhook**
4. Click **Save**

---

# PART 4: CI/CD PIPELINES

## Create CI Pipeline (Build & Test)

### Step 1: Create Pipeline Job in Jenkins

1. Click **New Item**
2. **Item name**: `fitverse-ci`
3. Select **Pipeline**
4. Click **OK**

### Step 2: Configure Pipeline

In the Pipeline section:

```groovy
@Library('shared-lib') _

pipeline {
    agent any
    
    environment {
        JAVA_HOME = "${tool 'Java17'}"
        MAVEN_HOME = "${tool 'maven-3'}"
        DOCKER_REGISTRY = "your-dockerhub-username"
        DOCKER_IMAGE = "${DOCKER_REGISTRY}/fitverse-backend"
        DOCKER_TAG = "${BUILD_NUMBER}-${GIT_COMMIT.take(7)}"
    }
    
    stages {
        stage('Checkout') {
            steps {
                script {
                    checkoutRepository([
                        repo_url: 'https://github.com/rupesh-hub/FitVerse.git',
                        credentials_id: 'git-ssh-cred'
                    ])
                }
            }
        }
        
        stage('Build') {
            steps {
                sh '''
                    echo "Building FitVerse Backend..."
                    cd Backend
                    mvn clean package -DskipTests
                '''
            }
        }
        
        stage('Code Quality - SonarQube') {
            steps {
                script {
                    withSonarQubeEnv('SonarQube') {
                        sh '''
                            ${SONAR_SCANNER_HOME}/bin/sonar-scanner \
                            -Dsonar.projectKey=fitverse-backend \
                            -Dsonar.projectName=FitVerse-Backend \
                            -Dsonar.sources=Backend/src \
                            -Dsonar.java.binaries=Backend/target/classes \
                            -Dsonar.exclusions=**/*Test.java,**/test/**
                        '''
                    }
                }
            }
        }
        
        stage('Quality Gate') {
            steps {
                script {
                    timeout(time: 5, unit: 'MINUTES') {
                        def qualityGate = waitForQualityGate()
                        if (qualityGate.status != 'OK') {
                            error "Quality Gate Failed: ${qualityGate.status}"
                        }
                    }
                }
            }
        }
        
        stage('Trivy Scan') {
            steps {
                sh '''
                    echo "Scanning for vulnerabilities..."
                    trivy fs --severity HIGH,CRITICAL Backend/
                '''
            }
        }
        
        stage('Build Docker Image') {
            steps {
                sh '''
                    echo "Building Docker image..."
                    cd Backend
                    docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} -t ${DOCKER_IMAGE}:latest .
                '''
            }
        }
        
        stage('Push to Registry') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'docker-cred', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        sh '''
                            echo "${DOCKER_PASS}" | docker login -u "${DOCKER_USER}" --password-stdin
                            docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
                            docker push ${DOCKER_IMAGE}:latest
                            docker logout
                        '''
                    }
                }
            }
        }
    }
    
    post {
        success {
            echo "✓ Build successful! Image: ${DOCKER_IMAGE}:${DOCKER_TAG}"
        }
        failure {
            echo "✗ Build failed"
        }
    }
}
```

Click **Save**

### Step 3: Run CI Pipeline

1. Click **Build Now**
2. Click the build number to see logs
3. Verify: Docker image pushed to registry

---

## Create CD Pipeline (Deploy to Kubernetes)

### Step 1: Create Pipeline Job

1. Click **New Item**
2. **Item name**: `fitverse-cd`
3. Select **Pipeline**
4. Click **OK**

### Step 2: Configure Pipeline

```groovy
@Library('shared-lib') _

pipeline {
    agent any
    
    parameters {
        choice(name: 'ENVIRONMENT', choices: ['dev', 'staging', 'prod'], description: 'Deploy environment')
        string(name: 'IMAGE_TAG', defaultValue: 'latest', description: 'Docker image tag to deploy')
    }
    
    environment {
        DOCKER_IMAGE = "your-dockerhub-username/fitverse-backend:${IMAGE_TAG}"
        HELM_CHART = "./helm"
        K8S_NAMESPACE = "fitverse-${ENVIRONMENT}"
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/main']],
                    userRemoteConfigs: [[
                        url: 'https://github.com/rupesh-hub/FitVerse.git',
                        credentialsId: 'git-ssh-cred'
                    ]]
                ])
            }
        }
        
        stage('Validate Helm Chart') {
            steps {
                sh '''
                    echo "Validating Helm chart..."
                    helm lint ${HELM_CHART}
                    helm template fitverse ${HELM_CHART} > /tmp/manifest.yaml
                    echo "✓ Helm chart valid"
                '''
            }
        }
        
        stage('Create Namespace') {
            steps {
                sh '''
                    kubectl create namespace ${K8S_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
                    echo "✓ Namespace ${K8S_NAMESPACE} ready"
                '''
            }
        }
        
        stage('Deploy with Helm') {
            steps {
                sh '''
                    echo "Deploying to ${ENVIRONMENT}..."
                    helm upgrade --install fitverse ${HELM_CHART} \
                        -n ${K8S_NAMESPACE} \
                        --values ${HELM_CHART}/values-${ENVIRONMENT}.yaml \
                        --set backend.image.tag=${IMAGE_TAG} \
                        --wait \
                        --timeout=10m
                '''
            }
        }
        
        stage('Verify Deployment') {
            steps {
                sh '''
                    echo "Waiting for rollout..."
                    kubectl rollout status deployment/fitverse-backend -n ${K8S_NAMESPACE} --timeout=5m
                    kubectl rollout status deployment/fitverse-frontend -n ${K8S_NAMESPACE} --timeout=5m
                    
                    echo "✓ Deployment successful!"
                    kubectl get pods -n ${K8S_NAMESPACE}
                '''
            }
        }
    }
    
    post {
        success {
            echo "✓ Deployed to ${ENVIRONMENT} successfully!"
        }
        failure {
            sh '''
                echo "✗ Deployment failed. Rolling back..."
                helm rollback fitverse -n ${K8S_NAMESPACE}
            '''
        }
    }
}
```

Click **Save**

---

# PART 5: KUBERNETES DEPLOYMENT

## Prerequisites

```bash
# Install Helm
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# Install kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

# Verify
helm version
kubectl version --client
```

## Quick Helm Deployment

```bash
# Create namespace
kubectl create namespace fitverse-prod

# Deploy
helm install fitverse ./helm \
    -n fitverse-prod \
    --values values-prod.yaml \
    --wait --timeout=10m

# Verify
helm status fitverse -n fitverse-prod
kubectl get all -n fitverse-prod
```

## Multi-Environment Deployments

### Development

```bash
kubectl create namespace fitverse-dev
helm install fitverse ./helm -n fitverse-dev \
    --values values-dev.yaml \
    --set backend.replicas=1 \
    --set frontend.replicas=1
```

### Staging

```bash
kubectl create namespace fitverse-staging
helm install fitverse ./helm -n fitverse-staging \
    --values values-staging.yaml \
    --set backend.replicas=2 \
    --set frontend.replicas=2
```

### Production

```bash
kubectl create namespace fitverse-prod
helm install fitverse ./helm -n fitverse-prod \
    --values values-prod.yaml \
    --set backend.replicas=3 \
    --set frontend.replicas=3
```

---

## Helm Commands Cheatsheet

```bash
# List releases
helm list -n fitverse-prod

# Get deployment status
helm status fitverse -n fitverse-prod

# Upgrade deployment
helm upgrade fitverse ./helm -n fitverse-prod -f values-prod.yaml

# Rollback to previous version
helm rollback fitverse -n fitverse-prod

# Delete deployment
helm uninstall fitverse -n fitverse-prod

# View generated manifests
helm template fitverse ./helm -f values-prod.yaml
```

---

# PART 6: TESTING & TROUBLESHOOTING

## Test CI Pipeline

```bash
# Trigger manually
# Jenkins UI → fitverse-ci → Build Now

# Check logs
# Click build number → Console Output

# Verify Docker image
docker images | grep fitverse-backend
```

## Test CD Pipeline

```bash
# Trigger with parameters
# Jenkins UI → fitverse-cd → Build with Parameters
# Select: ENVIRONMENT=dev, IMAGE_TAG=latest

# Verify Kubernetes deployment
kubectl get pods -n fitverse-dev
kubectl logs -f deployment/fitverse-backend -n fitverse-dev
```

## Test SonarQube Integration

```bash
# Manual analysis
cd /path/to/project
sonar-scanner \
    -Dsonar.projectKey=fitverse-backend \
    -Dsonar.sources=src \
    -Dsonar.host.url=http://localhost:9000 \
    -Dsonar.login=squ_your_token_here

# Check SonarQube dashboard
# Browser: http://localhost:9000/dashboard?id=fitverse-backend
```

## Common Issues & Solutions

| Issue | Solution |
|-------|----------|
| **Docker build fails** | Check Dockerfile path, ensure Docker daemon running (`sudo systemctl status docker`) |
| **Kubernetes deployment fails** | Check namespace exists, image pull errors, sufficient resources (`kubectl describe pod <pod-name> -n <namespace>`) |
| **SonarQube webhook not received** | Verify webhook URL in SonarQube, check Jenkins logs (`tail -f /var/log/jenkins/jenkins.log`) |
| **Quality gate blocks build** | Check SonarQube dashboard for issues, lower thresholds if needed |
| **Helm rollout stuck** | Check pod logs (`kubectl logs -f <pod-name> -n <namespace>`), increase timeout |
| **Permission denied errors** | Ensure Jenkins user has sudo privileges or docker group access |

---

# REFERENCE: COMMANDS & CHECKLISTS

## Quick Commands

```bash
# Jenkins status
sudo systemctl status jenkins
sudo systemctl restart jenkins

# SonarQube status (Docker)
docker ps | grep sonarqube
docker logs sonarqube

# Kubernetes deployments
kubectl get deployments -n fitverse-prod
kubectl describe deployment fitverse-backend -n fitverse-prod
kubectl logs -f deployment/fitverse-backend -n fitverse-prod

# Helm status
helm list
helm status fitverse -n fitverse-prod

# Rollback deployment
helm rollback fitverse 1 -n fitverse-prod
```

## Pre-Deployment Checklist

- [ ] Jenkins running and accessible
- [ ] All plugins installed and enabled
- [ ] Java17 and Maven3 tools configured
- [ ] Git SSH credentials added
- [ ] Docker credentials added
- [ ] Shared library configured
- [ ] SonarQube running
- [ ] SonarQube token created
- [ ] SonarQube webhook configured
- [ ] CI pipeline tested successfully
- [ ] Docker images pushed to registry
- [ ] Kubernetes cluster accessible
- [ ] Helm chart validated
- [ ] values-dev.yaml, values-staging.yaml, values-prod.yaml created
- [ ] CD pipeline tested in dev environment

## Production Safety Checklist

- [ ] All images scanned with Trivy
- [ ] SonarQube quality gate passing
- [ ] Code review completed
- [ ] Database backup before deployment
- [ ] Monitoring and alerting configured
- [ ] Rollback plan documented
- [ ] Team notification of deployment

---

## Troubleshooting Commands

```bash
# Check Jenkins connectivity to SonarQube
curl -u admin:password http://localhost:9000/api/system/status

# Check Jenkins connectivity to Docker Registry
docker pull your-registry/image:tag

# Check Jenkins connectivity to Kubernetes
kubectl auth can-i create deployments --as=system:serviceaccount:jenkins:default

# View Jenkins logs
tail -f /var/log/jenkins/jenkins.log
tail -f /var/log/syslog | grep jenkins

# View SonarQube logs (Docker)
docker logs -f sonarqube

# View Kubernetes deployment logs
kubectl logs -f deployment/fitverse-backend -n fitverse-prod
kubectl describe pod <pod-name> -n fitverse-prod
```

---

## Getting Help

- Jenkins logs: `/var/log/jenkins/jenkins.log`
- SonarQube logs: `docker logs sonarqube` or `/opt/sonarqube/logs/sonar.log`
- Kubernetes events: `kubectl describe node`, `kubectl get events -n fitverse-prod`
- GitHub Issues: Check repository issues for similar problems
- Documentation: jenkins.io, sonarqube.com, kubernetes.io/docs

---

**You're now ready for production CI/CD! 🚀**
