# FitVerse Jenkins Configuration Guide
## Complete Step-by-Step Setup for Beginners

---

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Jenkins Installation (Ubuntu/Linux)](#jenkins-installation)
3. [Initial Jenkins Setup](#initial-jenkins-setup)
4. [Installing Required Plugins](#plugins)
5. [Tool Configuration (Java17 & Maven3)](#tool-configuration)
6. [Global Credentials Setup](#global-credentials)
7. [Shared Library Configuration](#shared-library-setup)
8. [Creating CI Pipeline](#ci-pipeline)
9. [Creating CD Pipeline](#cd-pipeline)
10. [Testing & Troubleshooting](#testing)

---

## Prerequisites
- Ubuntu 20.04 LTS or later
- Minimum 4GB RAM, 20GB disk space
- Docker installed and running
- kubectl configured
- GitHub account with repository access
- DockerHub account

---

## Jenkins Installation (Ubuntu/Linux)

### Step 1: Update System
```bash
sudo apt-get update
sudo apt-get upgrade -y
```

### Step 2: Install Java 17 (Required for Jenkins)
Jenkins requires Java. Install JDK 17:
```bash
sudo apt-get install -y openjdk-17-jdk openjdk-17-jre
java -version  # Verify installation
```

### Step 3: Add Jenkins Repository
```bash
curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io.key | sudo tee /usr/share/keyrings/jenkins-keyring.asc > /dev/null
echo deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] https://pkg.jenkins.io/debian-stable binary/ | sudo tee /etc/apt/sources.list.d/jenkins.list > /dev/null
```

### Step 4: Install Jenkins
```bash
sudo apt-get update
sudo apt-get install -y jenkins
```

### Step 5: Start Jenkins Service
```bash
sudo systemctl start jenkins
sudo systemctl enable jenkins  # Auto-start on reboot
sudo systemctl status jenkins  # Verify it's running
```

### Step 6: Access Jenkins
1. Open browser: `http://localhost:8080`
2. Get admin password:
```bash
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```
3. Copy the password and paste in the browser

---

## Initial Jenkins Setup

### Step 1: Unlock Jenkins
1. Paste the admin password from above
2. Click **Continue**

### Step 2: Plugin Installation
1. Click **Install suggested plugins** (easier for beginners)
2. Wait for all plugins to install (~5-10 minutes)

### Step 3: Create First Admin User
1. Enter username: `admin`
2. Enter password: `<strong-password>`
3. Full name: `Jenkins Admin`
4. Email: `your-email@gmail.com`
5. Click **Save and Continue**

### Step 4: Jenkins URL Configuration
1. Jenkins URL: `http://<your-server-ip>:8080/`
2. Click **Save and Finish**

---

## Installing Required Plugins

Your CI/CD pipelines need specific plugins. Install them now:

### Step 1: Go to Plugin Manager
1. Click **Manage Jenkins** (top left)
2. Click **Manage Plugins**

### Step 2: Search and Install Each Plugin

Create each one in a new browser tab for reference:

| # | Plugin Name | Search Term | Purpose |
|---|---|---|---|
| 1 | Pipeline | `Pipeline` | For Declarative Pipelines |
| 2 | Pipeline: Shared Groovy Libraries | `pipeline-model-definition` | For @Library('Shared') |
| 3 | Docker | `docker-plugin` | Docker integration |
| 4 | Docker Pipeline | `docker-workflow` | Docker in pipelines |
| 5 | SonarQube Scanner | `sonar` | SonarQube scanning |
| 6 | Email Extension | `email-ext` | Email notifications |
| 7 | GitHub | `github` | GitHub integration |
| 8 | Kubernetes | `kubernetes` | Kubernetes support |
| 9 | Kubernetes CLI | `kubernetes-cli` | kubectl support |
| 10 | Helm | `helm` | Helm deployment |
| 11 | Timestamper | `timestamper` | Better log timestamps |

### Installation Steps for Each Plugin:
1. Click **Available plugins** tab
2. Search for plugin name (e.g., "docker")
3. Check the checkbox
4. Click **Install without restart**
5. Repeat for all plugins

**After all plugins are installed:**
- Click **Manage Jenkins** → **System Configuration**
- Restart Jenkins if prompted

---

## Tool Configuration (Java17 & Maven3)

Your pipeline uses:
```
tools {
   jdk 'Java17'
   maven 'maven-3'
}
```

Let's configure these:

### Step 1: Configure Java 17

1. Go to **Manage Jenkins**
2. Click **Tools and System Configuration**
3. Click **Tools**
4. Look for **JDK installations** section
5. Click **Add JDK**

Fill in:
- **Name**: `Java17` (MUST match your pipeline)
- **JAVA_HOME**: Leave empty, Jenkins will auto-detect
- **Check**: "Install automatically"
- In dropdown, select latest Java 17 (e.g., `jdk-17.0.9`)
- Click **Save**

### Step 2: Configure Maven 3

1. Still in **Tools** section
2. Look for **Maven installations** section
3. Click **Add Maven**

Fill in:
- **Name**: `maven-3` (MUST match your pipeline)
- **MAVEN_HOME**: Leave empty
- **Check**: "Install automatically"
- In dropdown, select latest Maven 3.9.x
- Click **Save**

**Result**: Jenkins will auto-download and cache these tools.

---

## Global Credentials Setup

Your pipeline needs 3 types of credentials. Let's set them up:

### Type 1: GitHub SSH Credentials

Used for: `checkoutRepository()` to pull your code

#### Step 1: Generate SSH Key (if you don't have one)
```bash
ssh-keygen -t ed25519 -C "jenkins@fitverse"
# Save as: /home/jenkins/.ssh/id_rsa
# Press Enter for no passphrase
cat ~/.ssh/id_rsa.pub  # Copy this
```

#### Step 2: Add SSH Key to GitHub
1. Go to GitHub → Settings → SSH and GPG keys
2. Click **New SSH key**
3. Title: `Jenkins FitVerse`
4. Paste the public key from above
5. Click **Add SSH key**

#### Step 3: Add to Jenkins
1. Go to **Manage Jenkins**
2. Click **Credentials**
3. Click on **(global)** domain
4. Click **Add Credentials** (top left)
5. Fill in:
   - **Kind**: SSH Username with private key
   - **Scope**: Global
   - **ID**: `git-ssh-cred` (MUST match CD pipeline)
   - **Username**: `git`
   - **Private Key**: Select "Enter directly"
   - Paste content of `~/.ssh/id_rsa`
   - **Passphrase**: Leave empty
6. Click **Create**

### Type 2: DockerHub Credentials

Used for: Pushing Docker images to DockerHub

#### Step 1: Generate DockerHub Token
1. Go to DockerHub.com → Account Settings → Security
2. Click **New Access Token**
3. Name: `Jenkins`
4. Copy the token

#### Step 2: Add to Jenkins
1. Go to **Manage Jenkins** → **Credentials**
2. Click **Add Credentials**
3. Fill in:
   - **Kind**: Username with password
   - **Scope**: Global
   - **ID**: `dockerhub` (Used in pipeline: `pushImages(pushList, "dockerhub")`)
   - **Username**: Your DockerHub username
   - **Password**: Paste the token from Step 1
4. Click **Create**

### Type 3: SonarQube Credentials

Used for: `trivyFsScan()` and SonarQube scanning

#### Step 1: Generate SonarQube Token
1. Go to SonarQube UI (usually `http://sonarqube:9000`)
2. Click your profile icon → My Account → Security
3. Click **Generate Tokens**
4. Name: `Jenkins`
5. Copy the token

#### Step 2: Add to Jenkins
1. Go to **Manage Jenkins** → **Credentials**
2. Click **Add Credentials**
3. Fill in:
   - **Kind**: Secret text
   - **Scope**: Global
   - **ID**: `SonarQube` (MUST match your pipeline env: `SONAR_API_ENV = "SonarQube"`)
   - **Secret**: Paste the SonarQube token
4. Click **Create**

### Type 4: Kubernetes Config

Used for: CD Pipeline to deploy to Kubernetes

#### Step 1: Get Kubernetes Config
```bash
# Copy your kubeconfig file
cat ~/.kube/config
```

#### Step 2: Add to Jenkins
1. Go to **Manage Jenkins** → **Credentials**
2. Click **Add Credentials**
3. Fill in:
   - **Kind**: Secret file
   - **Scope**: Global
   - **ID**: `k8s-config` (MUST match CD pipeline: `credentialsId: 'k8s-config'`)
   - **File**: Upload your kubeconfig file
4. Click **Create**

---

## Shared Library Configuration

Your pipelines use: `@Library('Shared') _`

This means Jenkins needs to load a shared library with common functions.

### Step 1: Create Shared Library Repository (GitHub)

1. Go to GitHub and create new repo: `jenkins-shared-library`
2. Clone it locally:
```bash
git clone https://github.com/YOUR_USERNAME/jenkins-shared-library.git
cd jenkins-shared-library
```

### Step 2: Create Library Structure
```bash
# Inside jenkins-shared-library directory
mkdir -p vars src/com/fitverse

# Create files
touch vars/checkoutRepository.groovy
touch vars/getVersion.groovy
touch vars/mavenExecute.groovy
touch vars/dockerBuild.groovy
touch vars/pushImages.groovy
touch vars/trivyFsScan.groovy
touch vars/trivyImageScan.groovy
touch vars/updateManifests.groovy
touch vars/notify.groovy
```

### Step 3: Add Library Functions

**vars/checkoutRepository.groovy**
```groovy
def call(String repoUrl, String branch = 'main', String credentialsId = 'git-ssh-cred') {
    checkout([
        $class: 'GitSCM',
        branches: [[name: "*/${branch}"]],
        userRemoteConfigs: [[
            url: repoUrl,
            credentialsId: credentialsId
        ]]
    ])
    echo "✓ Repository checked out: ${repoUrl}"
}
```

**vars/getVersion.groovy**
```groovy
def call(String projectPath = '.') {
    def pom = readFile("${projectPath}/pom.xml")
    def matcher = pom =~ /<version>(.*?)<\/version>/
    return matcher[0][1]
}
```

**vars/mavenExecute.groovy**
```groovy
def call(String goals, String projectPath = '.') {
    sh """
        cd ${projectPath}
        mvn ${goals} -DskipTests
    """
    echo "✓ Maven executed: ${goals}"
}
```

**vars/dockerBuild.groovy**
```groovy
def call(String repo, List<String> tags, Map buildParams = [:], String dockerfilePath = 'Dockerfile') {
    def buildArgs = buildParams.collect { key, value -> "--build-arg ${key}=${value}" }.join(' ')
    def tagArgs = tags.collect { tag -> "-t ${repo}:${tag}" }.join(' ')
    
    sh """
        docker build ${buildArgs} ${tagArgs} -f ${dockerfilePath} .
    """
    echo "✓ Docker image built: ${repo}:${tags.join(',')}"
}
```

**vars/pushImages.groovy**
```groovy
def call(List<String> images, String credentialsId = 'dockerhub') {
    withCredentials([usernamePassword(credentialsId: credentialsId, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
        sh """
            echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin
            ${images.collect { "docker push ${it}" }.join('\n')}
            docker logout
        """
    }
    echo "✓ Docker images pushed: ${images.join(', ')}"
}
```

**vars/trivyFsScan.groovy**
```groovy
def call(String path = '.') {
    sh """
        trivy fs --severity HIGH,CRITICAL ${path} || true
    """
    echo "✓ Trivy filesystem scan completed"
}
```

**vars/trivyImageScan.groovy**
```groovy
def call(List<String> images) {
    images.each { image ->
        sh """
            trivy image --severity HIGH,CRITICAL ${image} || true
        """
    }
    echo "✓ Trivy image scan completed"
}
```

**vars/updateManifests.groovy**
```groovy
def call(Map config) {
    def serviceName = config.serviceName ?: 'backend'
    def version = config.version
    def dockerUser = config.dockerUser
    def gitCredId = config.gitCredentialsId ?: 'git-ssh-cred'
    
    sh """
        # Update Helm values with new image tag
        sed -i "s|image\\.tag:.*|image\\.tag: ${version}|g" helm/values.yaml
        sed -i "s|${dockerUser}/.*:.*|${dockerUser}/fitverse-${serviceName}:${version}|g" helm/values.yaml
        
        # Commit and push changes
        git config user.name "Jenkins"
        git config user.email "jenkins@fitverse"
        git add helm/values.yaml
        git commit -m "Update ${serviceName} to version ${version}" || true
        git push origin main
    """
    echo "✓ Manifests updated with version: ${version}"
}
```

**vars/notify.groovy**
```groovy
def call(String status, String email) {
    def subject = "FitVerse Build ${status}: ${env.JOB_NAME} #${env.BUILD_NUMBER}"
    def body = """
    Build Status: ${status}
    Job: ${env.JOB_NAME}
    Build Number: ${env.BUILD_NUMBER}
    Build Log: ${env.BUILD_URL}console
    """
    
    emailext(
        subject: subject,
        body: body,
        to: email
    )
    echo "✓ Notification sent to ${email}"
}
```

### Step 4: Commit and Push to GitHub
```bash
cd jenkins-shared-library
git add .
git commit -m "Add shared library functions"
git push origin main
```

### Step 5: Configure Shared Library in Jenkins

1. Go to **Manage Jenkins** → **System Configuration**
2. Scroll down to **Global Pipeline Libraries**
3. Click **Add**
4. Fill in:
   - **Name**: `Shared` (MUST match `@Library('Shared')`)
   - **Default version**: `main`
   - **Modern SCM**: Check this
   - **GitHub**: Select GitHub
   - **Repository HTTPS URL**: `https://github.com/YOUR_USERNAME/jenkins-shared-library.git`
   - **Credentials**: Leave empty (public repo) or select GitHub SSH if private
5. Click **Save**

---

## Creating CI Pipeline

Now create the actual CI job in Jenkins.

### Step 1: Create New Job
1. Click **New Item** (top left)
2. Name: `FitVerse-CI`
3. Select: **Pipeline**
4. Click **OK**

### Step 2: Configure Job

**General Tab:**
- Check: "GitHub project"
- Project URL: `https://github.com/rupesh-hub/FitVerse-2026-01-01/`

**Build Triggers:**
- Check: "GitHub hook trigger for GITScm polling"
- (This allows GitHub webhooks to trigger the pipeline)

**Advanced Project Options:**
- Leave defaults

### Step 3: Add Pipeline Script

Scroll down to **Pipeline** section:
- **Definition**: Pipeline script from SCM
- **SCM**: Git
- **Repository URL**: `https://github.com/rupesh-hub/FitVerse-2026-01-01.git`
- **Credentials**: Select `git-ssh-cred`
- **Branches to build**: `*/main`
- **Script Path**: `Jenkinsfile-CI` (you'll create this next)

### Step 4: Save

Click **Save**

### Step 5: Create Jenkinsfile-CI in Your Repository

In your GitHub repo at root level, create `Jenkinsfile-CI`:

```groovy
@Library('Shared') _

pipeline {
    agent any

    tools {
       jdk 'Java17'
       maven 'maven-3'
    }

    environment {
        GIT_URL          = "https://github.com/rupesh-hub/FitVerse-2026-01-01.git"
        GIT_BRANCH       = "main"
        PROJECT_PATH     = "backend"
        DOCKERFILE_PATH  = "../docker/backend/Dockerfile"
        DOCKERHUB_REPO   = "rupesh1997/fitverse-backend"
        SONAR_API_ENV    = "SonarQube"
        ACTIVE_PROFILE   = "production"
    }

    stages {
        stage("Workspace Cleanup") {
            steps {
                cleanWs()
            }
        }

        stage('Checkout & Version Extraction') {
            steps {
                script {
                    checkoutRepository(env.GIT_URL, env.GIT_BRANCH)
                    env.APP_VERSION = getVersion(env.PROJECT_PATH)
                    echo "Extracted Version: ${env.APP_VERSION}"
                }
            }
        }

        stage('Java: Build & Package') {
            steps {
                script {
                    mavenExecute('clean package', env.PROJECT_PATH)
                }
            }
        }

        stage('Security Scanning') {
            steps {
                script {
                    trivyFsScan(env.PROJECT_PATH)
                }
            }
        }

        stage("Docker: Build Image") {
            steps {
                dir("${env.PROJECT_PATH}") {
                    script {
                        def tags = ["latest", env.APP_VERSION]
                        def buildParams = [
                            'PROJECT_VERSION': env.APP_VERSION,
                            'ACTIVE_PROFILE' : env.ACTIVE_PROFILE
                        ]
                        dockerBuild(env.DOCKERHUB_REPO, tags, buildParams, env.DOCKERFILE_PATH)
                    }
                }
            }
        }

        stage('Trivy: Image Scan') {
            steps {
                script {
                    trivyImageScan(["${env.DOCKERHUB_REPO}:${env.APP_VERSION}"])
                }
            }
        }

        stage("Docker: Push") {
            steps {
               script {
                  def pushList = ["${env.DOCKERHUB_REPO}:latest", "${env.DOCKERHUB_REPO}:${env.APP_VERSION}"]
                  pushImages(pushList, "dockerhub")
               }
            }
        }
    }

    post {
         success {
            script {
                archiveArtifacts artifacts: "${env.PROJECT_PATH}/target/*.jar", allowEmptyArchive: true
                notify('SUCCESS', 'dulalrupesh77@gmail.com')

                // Trigger CD Pipeline
                catchError(buildStepFailure: 'FAILURE', stageResult: 'FAILURE') {
                    build job: "FitVerse-CD", parameters: [
                        string(name: 'DOCKER_TAG', value: env.APP_VERSION),
                        string(name: 'SERVICE_NAME', value: 'backend')
                    ]
                }
            }
         }
         failure {
            script {
                notify('FAILURE', 'dulalrupesh77@gmail.com')
            }
         }
    }
}
```

### Step 6: Test CI Pipeline

1. Click **Build Now** in Jenkins
2. Watch the build progress in real-time
3. Check Console Output for logs

---

## Creating CD Pipeline

### Step 1: Create New Job
1. Click **New Item**
2. Name: `FitVerse-CD`
3. Select: **Pipeline**
4. Click **OK**

### Step 2: Configure Job

**General Tab:**
- Check: "This project is parameterized"
- Add Parameters:
   - **String Parameter 1**:
      - Name: `DOCKER_TAG`
      - Default value: `latest`
   - **String Parameter 2**:
      - Name: `SERVICE_NAME`
      - Default value: `backend`

### Step 3: Add Pipeline Script

Scroll down to **Pipeline** section:
- **Definition**: Pipeline script from SCM
- **SCM**: Git
- **Repository URL**: `https://github.com/rupesh-hub/FitVerse-2026-01-01.git`
- **Credentials**: Select `git-ssh-cred`
- **Branches to build**: `*/main`
- **Script Path**: `Jenkinsfile-CD`

### Step 4: Create Jenkinsfile-CD

In your GitHub repo root, create `Jenkinsfile-CD`:

```groovy
@Library('Shared') _

pipeline {
    agent any

    parameters {
        string(name: 'DOCKER_TAG', defaultValue: '', description: 'Tag passed from CI Job')
        string(name: 'SERVICE_NAME', defaultValue: 'backend', description: 'backend or frontend')
    }

    environment {
        GIT_URL          = "https://github.com/rupesh-hub/FitVerse-2026-01-01.git"
        GIT_BRANCH       = "main"
        DOCKERHUB_USER   = "rupesh1997"
        GIT_CRED_ID      = "git-ssh-cred"
        TARGET_NS        = "fitverse"
    }

    stages {
        stage('Checkout Manifests') {
            steps {
                checkoutRepository(env.GIT_URL, env.GIT_BRANCH, env.GIT_CRED_ID)
            }
        }

        stage('Update Manifests') {
            steps {
                script {
                    if (!params.DOCKER_TAG) {
                        error "Deployment aborted: DOCKER_TAG is missing!"
                    }
                    updateManifests(
                        serviceName: params.SERVICE_NAME,
                        version: params.DOCKER_TAG,
                        dockerUser: env.DOCKERHUB_USER,
                        gitCredentialsId: env.GIT_CRED_ID
                    )
                }
            }
        }

        stage('Pre-Deploy Check') {
            steps {
                script {
                    sh '''
                        # Check if namespace exists and is Helm-managed
                        if kubectl get namespace fitverse 2>/dev/null; then
                            if ! kubectl get namespace fitverse -o jsonpath='{.metadata.labels.managed-by}' | grep -q Helm; then
                                echo "Namespace exists but not Helm-managed. Deleting for clean setup..."
                                kubectl delete namespace fitverse --ignore-not-found=true
                                sleep 5
                            fi
                        fi
                    '''
                }
            }
        }

        stage('Deploy: Helm (Kubernetes)') {
            steps {
                withCredentials([file(credentialsId: 'k8s-config', variable: 'KUBECONFIG_FILE')]) {
                    script {
                        echo "Deploying ${params.SERVICE_NAME}:${params.DOCKER_TAG} to Namespace: ${env.TARGET_NS}..."

                        sh """
                            helm upgrade --install fitverse-${params.SERVICE_NAME} ./helm \
                                --namespace ${env.TARGET_NS} \
                                --create-namespace \
                                --set ${params.SERVICE_NAME}.image.tag=${params.DOCKER_TAG} \
                                --kubeconfig \$KUBECONFIG_FILE \
                                --rollback-on-failure \
                                --timeout 5m
                        """
                    }
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                withCredentials([file(credentialsId: 'k8s-config', variable: 'KUBECONFIG_FILE')]) {
                    sh """
                        echo "--- Current Status in ${env.TARGET_NS} ---"
                        kubectl --kubeconfig \$KUBECONFIG_FILE get pods,svc -n ${env.TARGET_NS} -l "app.kubernetes.io/instance=fitverse-${params.SERVICE_NAME}"
                    """
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        success {
            echo "✓ Deployment of ${params.SERVICE_NAME}:${params.DOCKER_TAG} successful!"
        }
        failure {
            echo "✗ Deployment failed. Check Helm logs and K8s events."
        }
    }
}
```

---

## Testing & Troubleshooting

### Test CI Pipeline
1. Make a change to your repo
2. Push to main branch
3. Jenkins should automatically trigger
4. Check Console Output for logs

### Common Issues & Fixes

| Problem | Solution |
|---------|----------|
| "Tool 'Java17' not found" | Go to Manage Jenkins → Tools → Add Java17 with "Install automatically" |
| "Maven not found" | Go to Manage Jenkins → Tools → Add maven-3 with "Install automatically" |
| "Docker command not found" | Run `sudo usermod -aG docker jenkins` and restart Jenkins |
| "Permission denied: ~/.ssh/id_rsa" | Run `sudo chown jenkins:jenkins ~/.ssh/id_rsa && sudo chmod 600 ~/.ssh/id_rsa` |
| "SonarQube token invalid" | Regenerate token in SonarQube → My Account → Security |
| "Kubeconfig not found" | Verify 'k8s-config' credential is uploaded in Manage Jenkins → Credentials |
| "Helm not found" | Run `curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 \| bash` |

### Useful Commands for Debugging
```bash
# Check Jenkins logs
sudo journalctl -u jenkins -f

# Check if Jenkins agent can access Docker
docker ps

# Test git SSH connection
ssh -T git@github.com

# Verify credentials
cat ~/.ssh/id_rsa.pub
```

---

## Next Steps

1. **Set up GitHub Webhooks** - Auto-trigger builds on push
2. **Configure SonarQube** - Quality analysis (see SONARQUBE_SETUP_GUIDE.md)
3. **Set up Monitoring** - Monitor pipeline health
4. **Create backup strategy** - Backup Jenkins configuration

---

## Quick Reference Checklist

- [ ] Jenkins installed and running
- [ ] All required plugins installed
- [ ] Java17 configured (Name: Java17)
- [ ] Maven3 configured (Name: maven-3)
- [ ] git-ssh-cred added (GitHub SSH)
- [ ] dockerhub credential added
- [ ] SonarQube credential added (ID: SonarQube)
- [ ] k8s-config credential added
- [ ] Shared library repository created
- [ ] Shared library functions added
- [ ] Shared library registered in Jenkins (Name: Shared)
- [ ] FitVerse-CI pipeline created
- [ ] FitVerse-CD pipeline created
- [ ] Both Jenkinsfiles in repository

---

## Troubleshooting & Production Checklist

### Common Issues & Fixes

#### Issue 1: Helm Invalid Release Name (fitverse-)

**Symptom**: Pipeline fails with `Error: release name is invalid: fitverse-`

**Root Cause**: Jenkins parameter `${SERVICE_NAME}` not being interpolated in shell script

**Solution**:
- Use triple **double quotes** in shell script: `sh """ ... """`
- Triple single quotes (`sh ''' ... '''`) prevent variable substitution
- Always use `sh """` when referencing Jenkins parameters like `${SERVICE_NAME}`

**Example Fix**:
```groovy
// ❌ WRONG - Single quotes prevent interpolation
sh '''
    helm install fitverse-${SERVICE_NAME} ./helm -n fitverse
'''

// ✅ CORRECT - Double quotes allow interpolation
sh """
    helm install fitverse-${SERVICE_NAME} ./helm -n fitverse
"""
```

#### Issue 2: Kubernetes Namespace Ownership Error

**Symptom**: Helm fails with "Ownership Error" or "cannot manage resources it doesn't own"

**Root Cause**: Namespace exists from previous deployment but not managed by Helm

**Solution**: Add namespace cleanup check before Helm deployment

```groovy
stage('Pre-Deploy Check') {
    steps {
        script {
            sh '''
                # Check if namespace exists and is Helm-managed
                if kubectl get namespace fitverse 2>/dev/null; then
                    if ! kubectl get namespace fitverse -o jsonpath='{.metadata.labels.managed-by}' | grep -q Helm; then
                        echo "Namespace exists but not Helm-managed. Deleting for clean setup..."
                        kubectl delete namespace fitverse --ignore-not-found=true
                        sleep 5
                    fi
                fi
            '''
        }
    }
}
```

#### Issue 3: GitHub Webhook Not Triggering (Local Network)

**Symptom**: Push to GitHub doesn't trigger Jenkins pipeline

**Root Cause**: Jenkins on local IP (e.g., 192.168.1.70) is unreachable from internet

**Solution**: Use webhook proxy (ngrok or Smee.io)

**Using ngrok**:
```bash
# Install ngrok
curl -sSLO https://bin.equinox.io/c/4VmDzA7iaHb/ngrok-stable-linux-amd64.zip
unzip ngrok-stable-linux-amd64.zip
sudo mv ngrok /usr/local/bin

# Start tunnel (in separate terminal)
ngrok http 8080

# You'll get: https://abc123def456.ngrok.io
# Use this URL in GitHub webhook: https://abc123def456.ngrok.io/github-webhook/
```

**Using Smee.io** (easier):
1. Go to https://smee.io
2. Click **Start a new channel**
3. Copy your channel URL: `https://smee.io/abc123def456`
4. Add webhook to GitHub with that URL
5. Run locally: `smee -u https://smee.io/abc123def456 -t http://localhost:8080/github-webhook/`

---

### Master Configuration Checklist

Use this checklist to verify your Jenkins setup is production-ready:

| Item | Requirement | Status |
|------|-------------|--------|
| **Jenkins Plugins** | GitHub Integration, Pipeline, Credentials, SonarQube Scanner | ☐ |
| **Credentials** | `git-ssh-cred` (SSH key for GitHub) | ☐ |
| **Credentials** | `dockerhub` (Docker Hub username/password) | ☐ |
| **Credentials** | `k8s-config` (Kubernetes config secret file) | ☐ |
| **Credentials** | `sonar-token` (SonarQube token) | ☐ |
| **Tool Config** | Java17 installed and configured | ☐ |
| **Tool Config** | Maven3 installed and configured | ☐ |
| **Tool Config** | SonarQube Scanner installed and configured | ☐ |
| **Shared Library** | Git repo URL configured correctly | ☐ |
| **Shared Library** | Branch set to `main` | ☐ |
| **Shared Library** | All functions are accessible in pipeline | ☐ |
| **Helm** | Chart.yaml exists in `/helm` directory | ☐ |
| **Webhook** | GitHub webhook URL configured | ☐ |
| **Webhook** | Webhook content type set to `application/json` | ☐ |
| **Networking** | Jenkins port 8080 is accessible | ☐ |
| **Networking** | Docker daemon is running | ☐ |
| **Kubernetes** | kubectl credentials configured | ☐ |
| **Kubernetes** | Can access cluster: `kubectl cluster-info` | ☐ |

---

### Verification Commands

Run these commands to verify your setup:

```bash
# Check Jenkins is running
curl -s http://localhost:8080 | head -20

# Check Docker is accessible
docker ps

# Check Kubernetes connection
kubectl cluster-info
kubectl get nodes

# Check SonarQube is running
curl -s http://localhost:9000/api/system/status

# Check Helm version
helm version

# Verify Git SSH key works
ssh -T git@github.com

# Test Maven
mvn -version

# Test Java
java -version
```

If any fails, review the corresponding setup section above.

---

## Key Takeaway: The Double Quote Rule

**This is critical for your pipeline success:**

```groovy
// ❌ Use single quotes for pure shell (no Jenkins variable substitution)
sh 'echo $PATH'  // Works - uses shell $PATH

// ❌ Don't use single quotes with Jenkins variables
sh 'helm install fitverse-${SERVICE_NAME} ./helm'  // FAILS - ${SERVICE_NAME} not replaced

// ✅ Use double quotes for Jenkins parameters
sh "helm install fitverse-${SERVICE_NAME} ./helm"  // WORKS - Jenkins replaces ${SERVICE_NAME}

// ✅ Use triple double quotes for multi-line with parameters
sh """
    helm install fitverse-${SERVICE_NAME} ./helm -n fitverse
    kubectl set image deployment/backend backend=myrepo/backend:${BUILD_TAG}
"""
```

---

Good luck with your CI/CD journey! 🚀
