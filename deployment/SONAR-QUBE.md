# SonarQube Setup & Jenkins Integration Guide

## Quick Start Overview
This guide covers: Docker/Local installation → Secret token → Webhooks → Jenkins integration → Testing

---

## Table of Contents
1. [Installation](#installation)
2. [Secret Token Generation](#secret-token-generation)
3. [Webhook Configuration](#webhook-configuration)
4. [Jenkins Integration](#jenkins-integration)
5. [Testing](#testing)

---

## Installation

### Option 1: Docker (Quickest)

**Step 1: Run SonarQube Container**

```bash
docker run -d \
  --name sonarqube \
  -p 9000:9000 \
  --restart unless-stopped \
  sonarqube:lts
```

**Step 2: Verify Installation**

```bash
# Wait 30 seconds for startup, then check:
curl -s http://localhost:9000/api/system/status

# Should return: {"status":"UP"}
```

**Step 3: Access SonarQube**

```
URL: http://localhost:9000 (or http://<your-server-ip>:9000)
Default Username: admin
Default Password: admin
```

**Step 4: Change Default Password**

- Click top-right **admin** → **My Account** → **Security** tab
- Change default password to something secure
- Save changes

---

### Option 2: Local Ubuntu Server Installation

**Step 1: Install Java 11+**

```bash
sudo apt-get update
sudo apt-get install -y openjdk-17-jdk
java -version  # Verify
```

**Step 2: Download & Install SonarQube**

```bash
# Create sonar user
sudo useradd -m -s /bin/bash sonar

# Download SonarQube LTS
cd /opt
sudo wget https://binaries.sonarsource.com/Distribution/sonarqube/sonarqube-10.3.0.82913.zip
sudo unzip sonarqube-10.3.0.82913.zip
sudo mv sonarqube-10.3.0.82913 sonarqube
sudo chown -R sonar:sonar /opt/sonarqube
```

**Step 3: Configure SonarQube**

```bash
# Edit configuration
sudo nano /opt/sonarqube/conf/sonar.properties

# Add/modify these lines:
sonar.host.url=http://localhost:9000
sonar.web.port=9000
sonar.web.javaAdditionalOpts=-server
```

**Step 4: Create Systemd Service**

```bash
sudo nano /etc/systemd/system/sonarqube.service
```

Paste:

```ini
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
```

**Step 5: Start SonarQube**

```bash
sudo systemctl daemon-reload
sudo systemctl enable sonarqube
sudo systemctl start sonarqube
sudo systemctl status sonarqube

# Check logs
tail -f /opt/sonarqube/logs/sonar.log
```

**Step 6: Access SonarQube**

Wait 1-2 minutes for startup:

```
URL: http://localhost:9000
Default Username: admin
Default Password: admin
```

---

## Secret Token Generation

### What is a Token?
A token is like a password that Jenkins uses to authenticate with SonarQube securely.

### Step 1: Login to SonarQube

```
URL: http://localhost:9000
Username: admin
Password: (your changed password)
```

### Step 2: Generate Token

1. Click top-right **admin** → **My Account**
2. Click **Security** tab
3. Under "Generate Tokens" section:
   - Token name: `jenkins-sonar-token`
   - Click **Generate**
4. Copy the token immediately (you won't see it again!)
5. Click **Done**

Example token: `squ_1a2b3c4d5e6f7g8h9i0j1k2l3m4n5o6p`

### Step 3: Save for Later

```
Save this token securely - you'll need it for Jenkins configuration
```

---

## Webhook Configuration

### What is a Webhook?
A webhook automatically notifies Jenkins when SonarQube code analysis completes.

### Step 1: Navigate to Webhooks

1. In SonarQube, click **Administration** (gear icon bottom-left)
2. Click **Webhooks** under **Configuration**
3. Click **Create**

### Step 2: Configure Webhook

Fill in these fields:

| Field | Value |
|-------|-------|
| Name | `jenkins-webhook` |
| URL | `http://<your-jenkins-ip>:8080/sonarqube-webhook/` |
| Secret | (leave empty for now) |

**Important**: Replace `<your-jenkins-ip>` with your actual Jenkins server IP

Example: `http://192.168.1.70:8080/sonarqube-webhook/`

### Step 3: Save

Click **Create** button.

### Step 4: Verify Webhook

1. Go back to **Webhooks**
2. Click the webhook you just created
3. Check the **Recent deliveries** section
4. You should see successful (green) webhook calls

---

## Jenkins Integration

### Step 1: Install SonarQube Plugin

1. In Jenkins, go to **Manage Jenkins** → **Plugins**
2. Click **Available plugins** tab
3. Search: `SonarQube Scanner`
4. Check the box for **SonarQube Scanner**
5. Click **Install without restart**
6. Wait for installation to complete

### Step 2: Add SonarQube Token as Credential

1. Go to **Manage Jenkins** → **Credentials**
2. Click **System** (on the left)
3. Click **Global credentials (unrestricted)**
4. Click **+ Add Credentials**

Fill in:

| Field | Value |
|-------|-------|
| Kind | `Secret text` |
| Secret | `squ_1a2b3c4d5e6f7g8h9i0j1k2l3m4n5o6p` (your token) |
| ID | `sonar-token` |
| Description | `SonarQube Jenkins Token` |

Click **Create**

### Step 3: Configure SonarQube in Jenkins

1. Go to **Manage Jenkins** → **System**
2. Scroll down to **SonarQube servers**
3. Check **Enable injection of SonarQube server configuration as build environment variables**
4. Click **Add SonarQube**

Fill in:

| Field | Value |
|-------|-------|
| Name | `SonarQube` |
| Server URL | `http://localhost:9000` (or your server IP) |
| Server authentication token | Select `sonar-token` from dropdown |

Click **Save**

### Step 4: Add SonarQube Scanner Tool

1. Go to **Manage Jenkins** → **Tools**
2. Scroll to **SonarQube Scanner**
3. Click **Add SonarQube Scanner**

Fill in:

| Field | Value |
|-------|-------|
| Name | `sonar-scanner` |
| Install automatically | Check this box |
| Version | `Latest` |

Click **Save**

---

## Testing

### Test 1: Manual SonarQube Analysis

```bash
# In your project root directory
cd /path/to/your/java/project

# Run SonarQube scan (Docker container)
docker run --rm \
  -e SONAR_HOST_URL=http://localhost:9000 \
  -e SONAR_LOGIN=squ_1a2b3c4d5e6f7g8h9i0j1k2l3m4n5o6p \
  -v $(pwd):/source \
  sonarsource/sonar-scanner-cli:latest \
  -Dsonar.projectKey=fitverse \
  -Dsonar.projectName=FitVerse \
  -Dsonar.sources=src
```

Check SonarQube dashboard - you should see analysis results.

### Test 2: Jenkins Pipeline Integration

In your Jenkinsfile (CI pipeline), add:

```groovy
stage('Code Quality - SonarQube') {
    steps {
        script {
            withSonarQubeEnv('SonarQube') {
                sh '''
                    ${SONAR_SCANNER_HOME}/bin/sonar-scanner \
                    -Dsonar.projectKey=fitverse \
                    -Dsonar.projectName=FitVerse \
                    -Dsonar.sources=src
                '''
            }
        }
    }
}
```

Run the Jenkins job and verify:
- Jenkins console shows "SonarQube analysis completed"
- SonarQube dashboard updates with new analysis
- Webhook delivery shows green checkmark

### Test 3: Quality Gate Check

Add to your Jenkinsfile:

```groovy
stage('Quality Gate') {
    steps {
        script {
            timeout(time: 5, unit: 'MINUTES') {
                waitForQualityGate abortPipeline: true
            }
        }
    }
}
```

If code quality fails, pipeline will fail too (good for enforcement!)

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| SonarQube won't start | Check logs: `docker logs sonarqube` or `tail -f /opt/sonarqube/logs/sonar.log` |
| Jenkins can't reach SonarQube | Verify URL: `curl http://localhost:9000/api/system/status` |
| Webhook not triggering | Check SonarQube **Administration** → **Webhooks** → recent deliveries for errors |
| Token appears invalid in Jenkins | Regenerate token and update Jenkins credential |
| Analysis shows 0% coverage | Check `sonar.sources` path in Jenkinsfile matches your project structure |
```

Now let me add the troubleshooting section to the Jenkins guide:
