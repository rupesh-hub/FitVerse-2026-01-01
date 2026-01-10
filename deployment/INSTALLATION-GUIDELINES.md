# FitVerse: Complete SonarQube Installation & Integration Guide (2026)

## Table of Contents
1. [SonarQube Installation](#sonarqube-installation)
2. [SonarQube Configuration](#sonarqube-configuration)
3. [SonarQube Project Setup](#sonarqube-project-setup)
4. [Token Generation](#token-generation)
5. [Webhook Setup](#webhook-setup)
6. [Jenkins Integration](#jenkins-integration)
7. [Quality Gates](#quality-gates)
8. [Troubleshooting](#troubleshooting)

---

## SonarQube Installation

### Option 1: Docker Installation (Recommended)

**Step 1: Create persistent volume for SonarQube data**

```bash
# Create directories for SonarQube data
mkdir -p ~/sonarqube/data
mkdir -p ~/sonarqube/logs
mkdir -p ~/sonarqube/extensions

# Set proper permissions
sudo chown -R 999:999 ~/sonarqube/
```

**Step 2: Run SonarQube Docker container**

```bash
docker run -d \
  --name sonarqube \
  -p 9000:9000 \
  -p 9092:9092 \
  -e SONAR_JDBC_URL=jdbc:postgresql://postgres:5432/sonar \
  -e SONAR_JDBC_USERNAME=sonar \
  -e SONAR_JDBC_PASSWORD=sonarpassword123 \
  -v ~/sonarqube/data:/opt/sonarqube/data \
  -v ~/sonarqube/logs:/opt/sonarqube/logs \
  -v ~/sonarqube/extensions:/opt/sonarqube/extensions \
  --restart unless-stopped \
  sonarqube:lts
```

**Step 3: Wait for startup**

```bash
# Check logs
docker logs -f sonarqube

# Wait for "SonarQube is ready" message (2-3 minutes)

# Verify SonarQube is running
curl -s http://localhost:9000/api/system/status
```

**Step 4: Access SonarQube**

```
URL: http://localhost:9000 (or http://<your-server-ip>:9000)
Default Username: admin
Default Password: admin
```

### Option 2: Docker Compose Installation

**Create docker-compose.yml:**

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:14-alpine
    container_name: sonar-postgres
    environment:
      POSTGRES_DB: sonar
      POSTGRES_USER: sonar
      POSTGRES_PASSWORD: sonarpassword123
    volumes:
      - postgres-data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U sonar -d sonar"]
      interval: 10s
      timeout: 5s
      retries: 5

  sonarqube:
    image: sonarqube:lts
    container_name: sonarqube
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SONAR_JDBC_URL: jdbc:postgresql://postgres:5432/sonar
      SONAR_JDBC_USERNAME: sonar
      SONAR_JDBC_PASSWORD: sonarpassword123
    volumes:
      - sonarqube-data:/opt/sonarqube/data
      - sonarqube-logs:/opt/sonarqube/logs
      - sonarqube-extensions:/opt/sonarqube/extensions
    ports:
      - "9000:9000"
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/api/system/status"]
      interval: 30s
      timeout: 10s
      retries: 5

volumes:
  postgres-data:
  sonarqube-data:
  sonarqube-logs:
  sonarqube-extensions:

networks:
  default:
    name: sonarqube-network
```

**Deploy with Docker Compose:**

```bash
# Start services
docker-compose up -d

# View logs
docker-compose logs -f sonarqube

# Check status
docker-compose ps
```

### Option 3: Kubernetes (Helm) Installation

**Add SonarQube Helm repository:**

```bash
# Add repository
helm repo add sonarqube https://sonarqube-charts.storage.googleapis.com
helm repo update

# Install SonarQube
helm install sonarqube sonarqube/sonarqube \
  --namespace sonarqube \
  --create-namespace \
  --set sonarqube.image.tag=lts \
  --set postgresql.enabled=true \
  --set postgresql.auth.password=sonarpassword123 \
  --set persistence.enabled=true \
  --set persistence.size=20Gi \
  --set ingress.enabled=true \
  --set ingress.hosts[0].name=sonarqube.example.com \
  --wait
```

**Verify installation:**

```bash
# Check pod status
kubectl get pods -n sonarqube

# Check service
kubectl get svc -n sonarqube

# Get initial admin password (default: admin/admin)
kubectl logs -n sonarqube -l app=sonarqube | grep "password"
```

**Access via port-forward:**

```bash
kubectl port-forward svc/sonarqube 9000:9000 -n sonarqube
# Access at http://localhost:9000
```

---

## SonarQube Configuration

### Step 1: First Login and Password Change

1. Open browser: `http://localhost:9000`
2. Login with default credentials:
    - **Username:** admin
    - **Password:** admin
3. You'll be prompted to change the password
4. Set a strong new password (e.g., `SonarQube@2026!Secure`)

### Step 2: Configure Server URL

**For remote access, set server URL:**

1. Go to **Administration** → **Configuration** → **Settings**
2. Search for `sonar.core.serverBaseURL`
3. Set value to: `http://sonarqube.example.com:9000` (or your public IP)
4. Click **Save**

### Step 3: Create Organization (Optional, for multi-project setup)

```bash
# Via API
curl -u admin:SonarQube@2026!Secure \
  -X POST "http://localhost:9000/api/organizations/create" \
  -d "key=fitverse&name=FitVerse%20Organization"
```

### Step 4: Configure System Settings

1. Go to **Administration** → **Configuration** → **Security**
2. **Force user authentication:**
    - Check "Force user authentication"
    - Prevents anonymous access
3. **Disable guest users:**
    - Uncheck "Allow users to signup themselves"

### Step 5: Set Up Email Notifications (Optional)

1. Go to **Administration** → **Configuration** → **Email**
2. Fill in:
    - **SMTP host:** smtp.gmail.com
    - **SMTP port:** 587
    - **From address:** sonarqube@fitverse.com
    - **From name:** FitVerse SonarQube
    - **Username:** your-email@gmail.com
    - **Password:** app-specific-password (for Gmail)
3. Click **Test** to verify
4. Click **Save**

---

## SonarQube Project Setup

### Step 1: Create New Project

1. Click **"Create project"** button (top right)
2. Choose project display name: `fitverse-backend`
3. Choose project key: `fitverse-backend` (auto-filled)
4. Click **Setup** (Do not skip)

### Step 2: Create Quality Profile

1. Go to **Quality Profiles** (under Administration menu)
2. Click **Create** button
3. **Profile name:** FitVerse Backend Standards
4. **Language:** Java (or your backend language)
5. Click **Create**

### Step 3: Add Quality Rules

1. In your new quality profile, click **Activate Rules**
2. Search for specific rules:
    - Code Smells
    - Bug Detection
    - Vulnerability Detection
    - Security Hotspots
3. Activate rules by clicking the rule and selecting "Activate"
4. Set severity levels (Blocker, Critical, Major, Minor, Info)

### Step 4: Set as Default Profile

1. Go to **Quality Profiles**
2. Click **Set as default** on your FitVerse profile

---

## Token Generation

### Step 1: Generate Global Analysis Token (Recommended)

**Via UI:**

1. Click your **profile icon** (top right) → **My Account**
2. Go to **Security** tab
3. Click **Generate** under "Generate Tokens"
4. **Token name:** jenkins-ci-token
5. **Expires in:** 90 days (or Never)
6. Click **Generate**

**Copy the token immediately** (it won't be shown again):
```
squ_abc123def456ghi789jkl012mno345pqr
```

### Step 2: Store Token Securely

**On Jenkins server:**

```bash
# Store in Jenkins credentials (via UI or CLI)
# OR store in environment file
echo "SONAR_TOKEN=squ_abc123def456ghi789jkl012mno345pqr" | sudo tee /etc/jenkins/sonar.env

# Restrict permissions
sudo chmod 600 /etc/jenkins/sonar.env
```

### Step 3: Generate User Analysis Token (Optional)

If you need per-user tokens:

1. Go to **Administration** → **Security** → **Users**
2. Click on a user
3. Go to **Tokens** tab
4. Click **Generate**
5. Follow same steps as above

---

## Webhook Setup

### Step 1: Create Webhook in SonarQube

**Via UI:**

1. Go to **Administration** → **Configuration** → **Webhooks**
2. Click **Create**
3. **Name:** jenkins-webhook
4. **URL:** `http://jenkins.example.com:8080/sonarqube-webhook/` (or your Jenkins URL)
5. **Secret:** Leave empty (unless Jenkins requires it)
6. **Events:** Check all events:
    - ✓ Project Analysis Done
7. Click **Create**

**Verify webhook:**

```bash
# Test webhook via cURL
curl -X POST \
  -H "Content-Type: application/json" \
  http://jenkins.example.com:8080/sonarqube-webhook/ \
  -d '{
    "project": {"key": "fitverse-backend"},
    "status": "SUCCESS"
  }'
```

### Step 2: Configure Jenkins to Receive Webhook

**In Jenkins:**

1. Go to **Manage Jenkins** → **System**
2. Scroll to **SonarQube Servers**
3. Check **Enable SonarQube Webhook**
4. (Optional) Set webhook URL: `http://jenkins.example.com:8080/sonarqube-webhook/`
5. Click **Save**

### Step 3: Add Webhook Event Handler in Pipeline

**Add to Jenkinsfile:**

```groovy
pipeline {
    agent any
    
    triggers {
        // Receive webhook from SonarQube
        webhook(
            requestHeader: 'X-Sonar-Webhook-HMAC',
            token: 'sonarqube-webhook-secret'
        )
    }
    
    stages {
        stage('Webhook Received') {
            steps {
                echo "SonarQube webhook received!"
                echo "Quality gate status: ${env.SONAR_QUBE_GATE_STATUS}"
            }
        }
    }
}
```

### Step 4: Test Webhook

**Run a SonarQube analysis and check:**

1. Jenkins should receive webhook event
2. Check Jenkins system logs:
   ```bash
   tail -f /var/log/jenkins/jenkins.log | grep sonarqube
   ```
3. Check SonarQube webhook logs:
    - Go to **Administration** → **Configuration** → **Webhooks**
    - Click on webhook name
    - View **Recent deliveries**

---

## Jenkins Integration

### Step 1: Install SonarQube Scanner Plugin

1. Go to **Manage Jenkins** → **Manage Plugins**
2. Search for **SonarQube Scanner**
3. Install **SonarQube Scanner** plugin
4. Restart Jenkins

### Step 2: Configure Global SonarQube Settings

1. Go to **Manage Jenkins** → **System**
2. Scroll to **SonarQube Servers**
3. Click **Add SonarQube**
4. Fill in:
    - **Name:** SonarQube
    - **Server URL:** `http://sonarqube.example.com:9000` (or `http://localhost:9000`)
    - **Server authentication token:** Click "Add" → Jenkins Credentials
        - **Kind:** Secret text
        - **Secret:** Paste your SonarQube token (squ_abc123...)
        - **ID:** sonar-token
        - **Description:** SonarQube Analysis Token
5. Click **Test Connection** (should show "Connection successful")
6. Click **Save**

### Step 3: Install SonarScanner on Jenkins

1. Go to **Manage Jenkins** → **Tools**
2. Scroll to **SonarQube Scanner**
3. Click **Add SonarQube Scanner**
4. Fill in:
    - **Name:** sonar-scanner
    - **Install automatically:** Check
    - **Install from Maven Central:** sonar-scanner-cli-5.0.1
5. Click **Save**

### Step 4: Add SonarQube Configuration to Jenkinsfile

**Basic Jenkinsfile with SonarQube:**

```groovy
pipeline {
    agent any
    
    environment {
        SONAR_HOME = tool 'sonar-scanner'
        SONAR_PROJECT_KEY = 'fitverse-backend'
        SONAR_SOURCES = 'src'
    }
    
    stages {
        stage('Checkout') {
            steps {
                git 'https://github.com/rupesh-hub/FitVerse.git'
            }
        }
        
        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }
        
        stage('SonarQube Analysis') {
            steps {
                script {
                    withSonarQubeEnv('SonarQube') {
                        sh '''
                            ${SONAR_HOME}/bin/sonar-scanner \
                              -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                              -Dsonar.sources=${SONAR_SOURCES} \
                              -Dsonar.java.binaries=target/classes \
                              -Dsonar.exclusions=**/test/** \
                              -Dsonar.coverage.exclusions=**/test/**
                        '''
                    }
                }
            }
        }
        
        stage('Quality Gate') {
            steps {
                script {
                    timeout(time: 1, unit: 'HOURS') {
                        def qualityGate = waitForQualityGate()
                        if (qualityGate.status != 'OK') {
                            error "Quality gate failed: ${qualityGate.status}"
                        }
                    }
                }
            }
        }
    }
    
    post {
        success {
            echo "✓ Build and quality checks passed!"
        }
        failure {
            echo "✗ Build or quality checks failed"
        }
    }
}
```

### Step 5: Advanced Configuration for Different Languages

**For JavaScript/Frontend:**

```groovy
stage('SonarQube Analysis') {
    steps {
        script {
            withSonarQubeEnv('SonarQube') {
                sh '''
                    ${SONAR_HOME}/bin/sonar-scanner \
                      -Dsonar.projectKey=fitverse-frontend \
                      -Dsonar.sources=src \
                      -Dsonar.exclusions=node_modules/**,dist/**,coverage/** \
                      -Dsonar.javascript.lcov.reportPaths=coverage/lcov.info
                '''
            }
        }
    }
}
```

**For Python:**

```groovy
stage('SonarQube Analysis') {
    steps {
        script {
            withSonarQubeEnv('SonarQube') {
                sh '''
                    ${SONAR_HOME}/bin/sonar-scanner \
                      -Dsonar.projectKey=fitverse-python \
                      -Dsonar.sources=. \
                      -Dsonar.exclusions=tests/**,venv/**,.venv/** \
                      -Dsonar.python.coverage.reportPaths=coverage.xml
                '''
            }
        }
    }
}
```

---

## Quality Gates

### Step 1: Create Custom Quality Gate

1. Go to **Quality Gates** (under Quality section)
2. Click **Create** button
3. **Name:** FitVerse Standard
4. Click **Create**

### Step 2: Add Conditions

1. Click **Add Condition**
2. Add conditions:

| Metric | Operator | Value | Notes |
|--------|----------|-------|-------|
| Coverage | is less than | 80% | Min code coverage |
| Duplicated Lines (%) | is greater than | 3% | Max duplication |
| Blocker Issues | is greater than | 0 | Block if any critical bugs |
| Critical Issues | is greater than | 5 | Max critical issues |
| Code Smell Rating | is worse than | A | A=good, E=worst |
| Security Rating | is worse than | A | Security vulnerabilities |

3. Click **Save**

### Step 3: Set as Default Quality Gate

1. Click the 3-dot menu on your quality gate
2. Select **Set as Default**

### Step 4: Bind Quality Gate to Project

1. Go to your **fitverse-backend** project
2. Click **Project Settings** → **Quality Gates**
3. Select **FitVerse Standard**
4. Click **Save**

---

## Troubleshooting

### SonarQube Won't Start

**Check logs:**

```bash
# Docker
docker logs sonarqube

# Docker Compose
docker-compose logs sonarqube

# Kubernetes
kubectl logs -n sonarqube -l app=sonarqube
```

**Common issues:**

| Issue | Solution |
|-------|----------|
| Port 9000 in use | `lsof -i :9000` and kill process |
| Postgres connection fails | Check Postgres is running and credentials |
| Out of memory | Increase memory: `-e SONAR_CE_JAVAADDITIONALOPTS=-Xmx2g` |
| Database already initialized | Delete volume and restart |

### Quality Gate Not Blocking

1. Verify project has quality gate assigned
2. Check conditions are configured correctly
3. Check webhook is delivering events
4. Manually run analysis and check SonarQube dashboard

### Webhook Not Received

1. Check Jenkins webhook handler is running:
   ```bash
   curl http://jenkins.example.com:8080/sonarqube-webhook/
   # Should get 405 (method not allowed) or similar
   ```
2. Check Jenkins firewall allows inbound from SonarQube
3. Check SonarQube webhook delivery logs
4. Check Jenkins system logs:
   ```bash
   tail -f /var/log/jenkins/jenkins.log | grep webhook
   ```

### Analysis Timeout

**Increase timeout in Jenkinsfile:**

```groovy
stage('Quality Gate') {
    steps {
        script {
            timeout(time: 2, unit: 'HOURS') {
                def qualityGate = waitForQualityGate()
            }
        }
    }
}
```

### Token Issues

**Regenerate token:**

```bash
# Delete old token
curl -u admin:password \
  -X DELETE "http://localhost:9000/api/user_tokens/revoke?name=jenkins-ci-token"

# Generate new token via UI or API
curl -u admin:password \
  -X POST "http://localhost:9000/api/user_tokens/generate?name=jenkins-ci-token"
```

---

## Complete Integration Checklist

- [ ] SonarQube installed and running
- [ ] Admin password changed from default
- [ ] SonarQube accessible from Jenkins
- [ ] Project created in SonarQube
- [ ] Quality profile configured
- [ ] Quality gate created and assigned
- [ ] Global analysis token generated
- [ ] Token stored in Jenkins credentials
- [ ] SonarQube plugin installed in Jenkins
- [ ] SonarQube server configured in Jenkins
- [ ] SonarScanner tool installed in Jenkins
- [ ] Jenkinsfile configured with SonarQube stage
- [ ] Webhook created in SonarQube
- [ ] Jenkins webhook receiver enabled
- [ ] Analysis run successfully
- [ ] Quality gate evaluation working
- [ ] Webhook event received by Jenkins

---

## Quick Command Reference

```bash
# SonarQube Status
docker ps | grep sonarqube
docker logs sonarqube

# Access SonarQube
# Browser: http://localhost:9000

# Test SonarQube API
curl -u admin:password http://localhost:9000/api/system/status

# Run Analysis from Command Line
sonar-scanner \
  -Dsonar.projectKey=fitverse-backend \
  -Dsonar.sources=src \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=squ_abc123def456

# View Analysis Results
# Browser: http://localhost:9000/dashboard?id=fitverse-backend

# Check Jenkins SonarQube Connection
curl http://jenkins:8080/api/json | jq '.sonarServer'
```

---

## Next Steps

1. **Integrate with CI/CD:** Add SonarQube scanning to your Jenkins pipeline
2. **Set Up Notifications:** Configure email alerts for quality gate failures
3. **Monitor Trends:** Review SonarQube dashboard regularly for code quality trends
4. **Enforce Standards:** Use quality gates to enforce coding standards
5. **Educate Team:** Teach developers to fix SonarQube issues early

You're now ready to implement enterprise-grade code quality checks!
