# FitVerse: Complete Production Deployment Guide (2026)

## Table of Contents
1. [Quick Start](#quick-start)
2. [Prerequisites & Setup](#prerequisites--setup)
3. [Multi-Environment Deployment](#multi-environment-deployment)
4. [Helm Operations](#helm-operations)
5. [CI/CD Pipeline with Jenkins](#cicd-pipeline-with-jenkins)
6. [ArgoCD GitOps Workflow](#argocd-gitops-workflow)
7. [Scaling & Performance](#scaling--performance)
8. [Upgrades & Rollbacks](#upgrades--rollbacks)
9. [Monitoring & Debugging](#monitoring--debugging)
10. [Disaster Recovery](#disaster-recovery)
11. [Security Operations](#security-operations)
12. [Troubleshooting](#troubleshooting)
13. [Cheat Sheet](#cheat-sheet)

---

## Quick Start

**Deploy to Production in 5 minutes:**

```bash
# 1. Create namespace
kubectl create namespace fitverse-prod

# 2. Install prerequisites (one-time)
helm repo add jetstack https://charts.jetstack.io && helm repo update
helm install cert-manager jetstack/cert-manager -n cert-manager --create-namespace --set installCRDs=true

# 3. Deploy FitVerse with production values
helm install fitverse ./helm -n fitverse-prod \
  --values values-prod.yaml \
  --set global.environment=production \
  --wait --timeout=10m

# 4. Verify deployment
helm status fitverse -n fitverse-prod
kubectl get all -n fitverse-prod
kubectl get ingress -n fitverse-prod
```

---

## Prerequisites & Setup

### System Requirements
- Kubernetes Cluster v1.24+ (GKE, EKS, AKS, or on-prem)
- Helm 3.12+
- kubectl configured with cluster access
- Docker Registry access (DockerHub or private registry)
- Git repository for manifests

### Install Tools (Ubuntu/Linux)

```bash
# Install Helm
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
helm version

# Install kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

# Install yq (YAML manipulation)
sudo wget https://github.com/mikefarah/yq/releases/latest/download/yq_linux_amd64 -O /usr/bin/yq
sudo chmod +x /usr/bin/yq

# Install Trivy (vulnerability scanning)
wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | sudo apt-key add -
echo "deb https://aquasecurity.github.io/trivy-repo/deb $(lsb_release -sc) main" | sudo tee -a /etc/apt/sources.list.d/trivy.list
sudo apt-get update && sudo apt-get install trivy -y
```

### Create ClusterIssuer for Let's Encrypt TLS

```bash
kubectl apply -f - <<EOF
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: ops@fitverse.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: nginx
EOF
```

---

## Multi-Environment Deployment

### Development Environment
```bash
# Dev cluster typically runs on minimal resources
helm install fitverse ./helm -n fitverse-dev --create-namespace \
  --values values-dev.yaml \
  --set global.environment=development \
  --set backend.replicas=1 \
  --set frontend.replicas=1 \
  --set mysql.storage.size=5Gi

# Verify
helm status fitverse -n fitverse-dev
```

**values-dev.yaml:**
```yaml
global:
  environment: development
backend:
  replicas: 1
  autoscaling:
    enabled: false
  resources:
    requests:
      cpu: 100m
      memory: 256Mi
    limits:
      cpu: 500m
      memory: 512Mi
frontend:
  replicas: 1
  autoscaling:
    enabled: false
  resources:
    requests:
      cpu: 50m
      memory: 128Mi
    limits:
      cpu: 250m
      memory: 256Mi
mysql:
  replicas: 1
  storage:
    size: 5Gi
    storageClassName: "standard"
ingress:
  host: "fitverse-dev.example.com"
```

### Staging Environment
```bash
# Staging tests production configs with real load
helm install fitverse ./helm -n fitverse-staging --create-namespace \
  --values values-staging.yaml \
  --set global.environment=staging \
  --set backend.replicas=2 \
  --set mysql.storage.size=50Gi
```

**values-staging.yaml:**
```yaml
global:
  environment: staging
backend:
  replicas: 2
  autoscaling:
    enabled: true
    minReplicas: 2
    maxReplicas: 3
    targetCPUUtilizationPercentage: 70
frontend:
  replicas: 2
  autoscaling:
    enabled: true
    minReplicas: 2
    maxReplicas: 3
mysql:
  replicas: 1
  storage:
    size: 50Gi
    storageClassName: "fast-ssd"
ingress:
  host: "fitverse-staging.example.com"
```

### Production Environment
```bash
# Production uses high availability setup
helm install fitverse ./helm -n fitverse-prod --create-namespace \
  --values values-prod.yaml \
  --set global.environment=production \
  --set backend.replicas=3 \
  --set mysql.storage.size=100Gi \
  --wait --timeout=15m
```

**values-prod.yaml:**
```yaml
global:
  environment: production
backend:
  replicas: 3
  autoscaling:
    enabled: true
    minReplicas: 3
    maxReplicas: 10
    targetCPUUtilizationPercentage: 70
  resources:
    requests:
      cpu: 500m
      memory: 768Mi
    limits:
      cpu: 1000m
      memory: 1.5Gi
frontend:
  replicas: 3
  autoscaling:
    enabled: true
    minReplicas: 3
    maxReplicas: 5
mysql:
  replicas: 1
  storage:
    size: 100Gi
    storageClassName: "fast-ssd"
  backup:
    enabled: true
    schedule: "0 2 * * *"
ingress:
  host: "fitverse.example.com"
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
```

---

## Helm Operations

### Install Release
```bash
# Basic install
helm install fitverse ./helm -n fitverse-prod --create-namespace

# With custom values
helm install fitverse ./helm -n fitverse-prod \
  --values values-prod.yaml

# With overrides
helm install fitverse ./helm -n fitverse-prod \
  --set backend.image.tag=2.0.0 \
  --set mysql.storage.size=100Gi

# Dry-run (preview without deploying)
helm install fitverse ./helm -n fitverse-prod --dry-run --debug
```

### Verify Installation
```bash
# Check release status
helm status fitverse -n fitverse-prod

# List all releases
helm list -n fitverse-prod

# Get rendered manifests
helm get manifest fitverse -n fitverse-prod

# View values used
helm get values fitverse -n fitverse-prod
```

### Upgrade Release
```bash
# Upgrade with new values
helm upgrade fitverse ./helm -n fitverse-prod \
  --values values-prod.yaml

# Upgrade specific component
helm upgrade fitverse ./helm -n fitverse-prod \
  --reuse-values \
  --set backend.image.tag=2.0.0 \
  --wait --timeout=5m

# Upgrade with atomic (auto-rollback on failure)
helm upgrade fitverse ./helm -n fitverse-prod \
  --values values-prod.yaml \
  --atomic
```

### Rollback Release
```bash
# Rollback to previous revision
helm rollback fitverse -n fitverse-prod

# Rollback to specific revision
helm rollback fitverse 2 -n fitverse-prod

# View rollout history
helm history fitverse -n fitverse-prod
```

### Uninstall Release
```bash
# Delete release, keep history
helm uninstall fitverse -n fitverse-prod --keep-history

# Completely remove release
helm uninstall fitverse -n fitverse-prod
```

---

## CI/CD Pipeline with Jenkins

### 1. Install Jenkins & Tools

```bash
# On Jenkins Server (Ubuntu)
# Install Trivy
sudo apt-get install trivy -y

# Install yq
sudo wget https://github.com/mikefarah/yq/releases/latest/download/yq_linux_amd64 -O /usr/bin/yq
sudo chmod +x /usr/bin/yq

# Install Helm
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh && sudo sh get-docker.sh
sudo usermod -aG docker jenkins
```

### 2. Create Shared Library (Groovy)

**vars/imagePush.groovy:**
```groovy
def call(Map config) {
    sh '''
        docker build -t ${DOCKER_REGISTRY}/${DOCKER_USER}/${IMAGE_NAME}:${DOCKER_TAG} .
        docker login -u ${DOCKER_USER} -p ${DOCKER_PASSWORD} ${DOCKER_REGISTRY}
        docker push ${DOCKER_REGISTRY}/${DOCKER_USER}/${IMAGE_NAME}:${DOCKER_TAG}
    '''
}
```

**vars/sonarScan.groovy:**
```groovy
def call(Map config) {
    sh '''
        sonar-scanner \
          -Dsonar.projectKey=${PROJECT_KEY} \
          -Dsonar.sources=. \
          -Dsonar.host.url=${SONAR_HOST_URL} \
          -Dsonar.login=${SONAR_TOKEN}
    '''
}
```

**vars/trivyScan.groovy:**
```groovy
def call(Map config) {
    sh '''
        trivy image --severity HIGH,CRITICAL \
          ${DOCKER_REGISTRY}/${DOCKER_USER}/${IMAGE_NAME}:${DOCKER_TAG}
    '''
}
```

**vars/updateManifests.groovy:**
```groovy
def call(Map config) {
    sh '''
        git clone ${GIT_REPO} manifests
        cd manifests
        
        # Update image tag in Helm values
        yq eval ".${SERVICE_NAME}.image.tag = \"${DOCKER_TAG}\"" -i values.yaml
        
        # Commit and push
        git config user.email "jenkins@fitverse.com"
        git config user.name "Jenkins"
        git add .
        git commit -m "Update ${SERVICE_NAME} to ${DOCKER_TAG}"
        git push origin main
    '''
}
```

### 3. Jenkins CI Pipeline

**FitVerse-CI Jenkinsfile:**
```groovy
pipeline {
    agent any
    
    environment {
        DOCKER_REGISTRY = 'docker.io'
        DOCKER_USER = credentials('docker-username')
        DOCKER_PASSWORD = credentials('docker-password')
        SONAR_HOST_URL = 'http://sonarqube:9000'
        SONAR_TOKEN = credentials('sonar-token')
        PROJECT_KEY = 'fitverse-backend'
        DOCKER_TAG = "${BUILD_NUMBER}"
    }
    
    parameters {
        choice(name: 'SERVICE_NAME', choices: ['backend', 'frontend'], description: 'Service to build')
    }
    
    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/rupesh-hub/FitVerse.git'
            }
        }
        
        stage('Build') {
            steps {
                dir("${params.SERVICE_NAME}") {
                    sh 'npm install || mvn clean package'
                }
            }
        }
        
        stage('Code Quality') {
            steps {
                script {
                    imagePush(
                        dockerfile: "${params.SERVICE_NAME}/Dockerfile",
                        imageName: "fitverse-${params.SERVICE_NAME}"
                    )
                }
            }
        }
        
        stage('SonarQube Scan') {
            steps {
                script {
                    sonarScan(projectKey: PROJECT_KEY)
                }
            }
        }
        
        stage('Build Image') {
            steps {
                script {
                    imagePush(
                        dockerfile: "${params.SERVICE_NAME}/Dockerfile",
                        imageName: "fitverse-${params.SERVICE_NAME}"
                    )
                }
            }
        }
        
        stage('Security Scan') {
            steps {
                script {
                    trivyScan(
                        image: "docker.io/${DOCKER_USER}/fitverse-${params.SERVICE_NAME}:${DOCKER_TAG}"
                    )
                }
            }
        }
        
        stage('Push to Registry') {
            steps {
                sh '''
                    docker push docker.io/${DOCKER_USER}/fitverse-${params.SERVICE_NAME}:${DOCKER_TAG}
                    docker tag docker.io/${DOCKER_USER}/fitverse-${params.SERVICE_NAME}:${DOCKER_TAG} \
                              docker.io/${DOCKER_USER}/fitverse-${params.SERVICE_NAME}:latest
                    docker push docker.io/${DOCKER_USER}/fitverse-${params.SERVICE_NAME}:latest
                '''
            }
        }
        
        stage('Trigger CD') {
            steps {
                build job: 'FitVerse-CD', parameters: [
                    string(name: 'SERVICE_NAME', value: "${params.SERVICE_NAME}"),
                    string(name: 'DOCKER_TAG', value: "${DOCKER_TAG}")
                ]
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
        failure {
            echo "Build failed - check logs"
        }
    }
}
```

---

## ArgoCD GitOps Workflow

### 1. Install ArgoCD

```bash
# Create namespace
kubectl create namespace argocd

# Install ArgoCD
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Access ArgoCD (Change service type to LoadBalancer)
kubectl patch svc argocd-server -n argocd -p '{"spec": {"type": "LoadBalancer"}}'

# Get initial password
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d
```

### 2. Connect Git Repository

1. Open ArgoCD UI: `http://<argocd-loadbalancer>`
2. Login: `admin` / `<password-from-above>`
3. Go to **Settings → Repositories → Connect Repo**
4. Choose **SSH** and add your Git SSH key
5. Enter repo URL: `git@github.com:rupesh-hub/FitVerse-2026.git`

### 3. Create Backend Application (Helm)

```bash
kubectl apply -f - <<EOF
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: fitverse-backend
  namespace: argocd
spec:
  project: default
  source:
    repoURL: 'git@github.com:rupesh-hub/FitVerse-2026.git'
    targetRevision: main
    path: helm/                    # Path to Helm chart
    helm:
      valueFiles:
        - values-prod.yaml
      parameters:
        - name: backend.image.tag
          value: 'latest'
  destination:
    server: 'https://kubernetes.default.svc'
    namespace: fitverse-prod
  syncPolicy:
    automated:
      prune: true                # Delete removed resources
      selfHeal: true             # Revert manual changes
    syncOptions:
      - CreateNamespace=true
EOF
```

### 4. Create Frontend Application

```bash
kubectl apply -f - <<EOF
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: fitverse-frontend
  namespace: argocd
spec:
  project: default
  source:
    repoURL: 'git@github.com:rupesh-hub/FitVerse-2026.git'
    targetRevision: main
    path: kubernetes/              # Path to K8s manifests
  destination:
    server: 'https://kubernetes.default.svc'
    namespace: fitverse-prod
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
      - CreateNamespace=true
EOF
```

### 5. Modified CD Pipeline for ArgoCD

When using ArgoCD, the CD pipeline only needs to update Git:

**FitVerse-CD Jenkinsfile (ArgoCD version):**
```groovy
pipeline {
    agent any
    
    parameters {
        string(name: 'SERVICE_NAME', description: 'Service to deploy')
        string(name: 'DOCKER_TAG', description: 'Docker image tag')
    }
    
    stages {
        stage('Update GitOps Manifests') {
            steps {
                script {
                    sh '''
                        git clone git@github.com:rupesh-hub/FitVerse-2026.git manifests
                        cd manifests
                        
                        # Update Helm values for the service
                        yq eval ".${SERVICE_NAME}.image.tag = \"${DOCKER_TAG}\"" -i helm/values-prod.yaml
                        
                        # Commit and push
                        git config user.email "jenkins@fitverse.com"
                        git config user.name "Jenkins CI"
                        git add helm/values-prod.yaml
                        git commit -m "Update ${SERVICE_NAME} image to ${DOCKER_TAG}"
                        git push origin main
                        
                        # ArgoCD will detect the change within 3 minutes
                    '''
                }
            }
        }
    }
    
    post {
        success {
            echo "Git updated - ArgoCD will sync automatically"
        }
    }
}
```

### 6. ArgoCD CLI Commands

```bash
# Install ArgoCD CLI
curl -sSL -o argocd-linux-amd64 https://github.com/argoproj/argo-cd/releases/latest/download/argocd-linux-amd64
sudo install -m 555 argocd-linux-amd64 /usr/local/bin/argocd

# Login to ArgoCD
argocd login <argocd-server> --username admin --password <password>

# List applications
argocd app list

# Sync application manually
argocd app sync fitverse-backend

# Get app status
argocd app get fitverse-backend

# Rollback to previous sync
argocd app rollback fitverse-backend
```

---

## Scaling & Performance

### Horizontal Pod Autoscaling (HPA)

```bash
# View HPA status
kubectl get hpa -n fitverse-prod

# Describe HPA
kubectl describe hpa fitverse-backend-hpa -n fitverse-prod

# Edit HPA thresholds
kubectl edit hpa fitverse-backend-hpa -n fitverse-prod

# Update via Helm
helm upgrade fitverse ./helm -n fitverse-prod \
  --reuse-values \
  --set backend.autoscaling.maxReplicas=15 \
  --set backend.autoscaling.targetCPUUtilizationPercentage=60
```

### Manual Pod Scaling

```bash
# Scale backend to 5 replicas
kubectl scale statefulset fitverse-backend --replicas=5 -n fitverse-prod

# Scale frontend to 4 replicas
kubectl scale deployment fitverse-frontend --replicas=4 -n fitverse-prod

# Check resource usage
kubectl top pods -n fitverse-prod --sort-by=cpu
kubectl top pods -n fitverse-prod --sort-by=memory
```

### Node Scaling (Infrastructure)

```bash
# Scale cluster (GKE example)
gcloud container clusters resize fitverse-cluster --num-nodes=5

# Scale with cloud provider CLI or update Terraform:
# terraform apply
```

---

## Upgrades & Rollbacks

### Pre-Upgrade Checklist

```bash
# 1. Backup database
kubectl exec fitverse-mysql-0 -n fitverse-prod -- \
  mysqldump -uroot -p$MYSQL_ROOT_PASSWORD fitverse > backup-$(date +%s).sql

# 2. Check current status
helm status fitverse -n fitverse-prod

# 3. Get current values
helm get values fitverse -n fitverse-prod > current-values.yaml

# 4. View release history
helm history fitverse -n fitverse-prod
```

### Perform Upgrade

```bash
# Upgrade with new image
helm upgrade fitverse ./helm -n fitverse-prod \
  --reuse-values \
  --set backend.image.tag=2.0.0 \
  --wait --timeout=5m

# Upgrade entire chart
helm upgrade fitverse ./helm -n fitverse-prod \
  --values values-prod.yaml \
  --wait --timeout=10m \
  --atomic  # Auto-rollback on failure

# Monitor rolling update
kubectl rollout status statefulset fitverse-backend -n fitverse-prod -w
```

### Rollback

```bash
# Rollback to previous revision
helm rollback fitverse -n fitverse-prod

# Rollback to specific revision
helm rollback fitverse 2 -n fitverse-prod

# Verify rollback
helm status fitverse -n fitverse-prod
kubectl get pods -n fitverse-prod
```

---

## Monitoring & Debugging

### Status & Logs

```bash
# Check release status
helm status fitverse -n fitverse-prod

# List all resources
kubectl get all -n fitverse-prod

# View pod logs (last 100 lines, follow)
kubectl logs -n fitverse-prod -l app=fitverse,component=backend -f --tail=100

# Get logs from crashed pod
kubectl logs pod/fitverse-backend-0 -n fitverse-prod --previous

# Stream logs from all containers
kubectl logs pod/fitverse-backend-0 -n fitverse-prod --all-containers=true
```

### Debugging

```bash
# Describe pod for events
kubectl describe pod fitverse-backend-0 -n fitverse-prod

# Execute into pod
kubectl exec -it fitverse-backend-0 -n fitverse-prod -- /bin/sh

# Network diagnostics
kubectl run -it --rm debug --image=busybox --restart=Never -- \
  nslookup fitverse-mysql.fitverse-prod.svc.cluster.local

# Test connectivity to backend
kubectl run -it --rm debug --image=curlimages/curl --restart=Never -- \
  curl http://fitverse-backend:8181/health
```

### Helm Debugging

```bash
# Dry-run to see what would deploy
helm template fitverse ./helm -n fitverse-prod --values values-prod.yaml

# Install in debug mode
helm install fitverse ./helm -n fitverse-prod --debug --dry-run
```

---

## Disaster Recovery

### Database Backup & Restore

**Automated Backup:**
```bash
helm upgrade fitverse ./helm -n fitverse-prod \
  --reuse-values \
  --set mysql.backup.enabled=true \
  --set mysql.backup.schedule="0 2 * * *"
```

**Manual Backup:**
```bash
# Backup to local file
kubectl exec fitverse-mysql-0 -n fitverse-prod -- \
  mysqldump -uroot -p$MYSQL_ROOT_PASSWORD fitverse > backup-$(date +%Y%m%d).sql

# Backup to cloud storage (GCS)
kubectl exec fitverse-mysql-0 -n fitverse-prod -- \
  mysqldump -uroot -p$MYSQL_ROOT_PASSWORD fitverse | \
  gsutil cp - gs://fitverse-backups/backup-$(date +%s).sql
```

**Restore from Backup:**
```bash
# Restore from local backup
kubectl exec -i fitverse-mysql-0 -n fitverse-prod -- \
  mysql -uroot -p$MYSQL_ROOT_PASSWORD < backup-20260109.sql

# Restore from cloud storage
gsutil cat gs://fitverse-backups/backup-1234567890.sql | \
  kubectl exec -i fitverse-mysql-0 -n fitverse-prod -- \
  mysql -uroot -p$MYSQL_ROOT_PASSWORD
```

### Pod Recovery

```bash
# Restart a single pod
kubectl delete pod fitverse-backend-0 -n fitverse-prod

# Restart all backend pods
kubectl delete pods -l app=fitverse,component=backend -n fitverse-prod

# Check PersistentVolume status
kubectl get pv,pvc -n fitverse-prod

# Describe PVC for errors
kubectl describe pvc fitverse-mysql-pvc-0 -n fitverse-prod
```

---

## Security Operations

### Update Secrets

```bash
# Update database password (recommended: use Sealed Secrets)
kubectl create secret generic fitverse-secrets \
  --from-literal=MYSQL_ROOT_PASSWORD=new-secure-password \
  -n fitverse-prod --dry-run=client -o yaml | kubectl apply -f -

# Verify secrets
kubectl get secrets -n fitverse-prod
kubectl describe secret fitverse-mysql-secret -n fitverse-prod
```

### RBAC Management

```bash
# Check RBAC bindings
kubectl get rolebindings,clusterrolebindings -n fitverse-prod

# Create ServiceAccount with limited permissions
kubectl create serviceaccount fitverse-external -n fitverse-prod
kubectl create rolebinding fitverse-external-rb \
  --clusterrole=view \
  --serviceaccount=fitverse-prod:fitverse-external

# Get token for external access
kubectl get secret -n fitverse-prod \
  $(kubectl get secret -n fitverse-prod | grep fitverse-external-token | awk '{print $1}') \
  -o jsonpath='{.data.token}' | base64 -d
```

### Certificate Management

```bash
# Check certificate status
kubectl get certificate -n fitverse-prod

# View certificate details
kubectl get secret fitverse-tls -n fitverse-prod -o jsonpath='{.data.tls\.crt}' | \
  base64 -d | openssl x509 -text -noout

# Renew certificate
kubectl delete certificate fitverse -n fitverse-prod
# Cert-manager will auto-recreate
```

---

## Troubleshooting

### Pods in CrashLoopBackOff

```bash
# Check logs
kubectl logs fitverse-backend-0 -n fitverse-prod --previous

# Describe pod
kubectl describe pod fitverse-backend-0 -n fitverse-prod

# Fix image tag
helm upgrade fitverse ./helm -n fitverse-prod \
  --reuse-values \
  --set backend.image.tag=1.0.1
```

### Database Connection Failed

```bash
# Check MySQL pod
kubectl get pods -n fitverse-prod -l component=mysql

# Test connection
kubectl run -it --rm debug --image=mysql:8.0 --restart=Never -- \
  mysql -h fitverse-mysql -uroot -p$MYSQL_ROOT_PASSWORD -e "SELECT 1"

# Check volume
kubectl exec fitverse-mysql-0 -n fitverse-prod -- ls -la /var/lib/mysql
```

### Ingress Not Routing Traffic

```bash
# Verify ingress
kubectl get ingress -n fitverse-prod -o yaml

# Check backend service endpoints
kubectl get endpoints fitverse-backend -n fitverse-prod
kubectl get endpoints fitverse-frontend -n fitverse-prod

# Verify DNS
nslookup fitverse.example.com

# Check ingress controller logs
kubectl logs -n ingress-nginx -l app.kubernetes.io/name=ingress-nginx
```

### PVC Stuck in Pending

```bash
# Check PVC status
kubectl describe pvc fitverse-mysql-pvc-0 -n fitverse-prod

# Check available StorageClass
kubectl get storageclass

# Create PersistentVolume if needed
kubectl apply -f - <<EOF
apiVersion: v1
kind: PersistentVolume
metadata:
  name: fitverse-pv
spec:
  capacity:
    storage: 100Gi
  accessModes:
    - ReadWriteOnce
  storageClassName: fast-ssd
  hostPath:
    path: /mnt/data
EOF
```

---

## Cheat Sheet: Quick Commands

```bash
# DEPLOYMENT
helm install fitverse ./helm -n fitverse-prod --create-namespace --values values-prod.yaml
helm upgrade fitverse ./helm -n fitverse-prod --reuse-values --set backend.image.tag=2.0.0 --wait
helm rollback fitverse -n fitverse-prod
helm uninstall fitverse -n fitverse-prod

# STATUS & MONITORING
helm status fitverse -n fitverse-prod
kubectl get all -n fitverse-prod
kubectl logs -l app=fitverse -n fitverse-prod -f
kubectl top pods -n fitverse-prod

# SCALING
kubectl scale statefulset fitverse-backend --replicas=5 -n fitverse-prod
kubectl get hpa -n fitverse-prod

# DEBUGGING
kubectl describe pod fitverse-backend-0 -n fitverse-prod
kubectl exec -it fitverse-backend-0 -n fitverse-prod -- /bin/sh
kubectl port-forward svc/fitverse-backend 8181:8181 -n fitverse-prod

# BACKUP & RESTORE
kubectl exec fitverse-mysql-0 -n fitverse-prod -- mysqldump -uroot -p$MYSQL_ROOT_PASSWORD fitverse > backup.sql
kubectl exec -i fitverse-mysql-0 -n fitverse-prod -- mysql -uroot -p$MYSQL_ROOT_PASSWORD < backup.sql

# ARGOCD
argocd app list
argocd app sync fitverse-backend
argocd app rollback fitverse-backend
```

---

## Summary: Your Deployment Architecture

```
┌─────────────────────────────────────────────────┐
│        Git Repository (GitHub/GitLab)           │
│  (Contains helm/, values files, manifests)      │
└────────────────┬────────────────────────────────┘
                 │
       ┌─────────┴─────────┐
       │                   │
    ┌──▼──┐            ┌──▼──────┐
    │     │            │ ArgoCD  │
    │ CI  │            │ (Pulls) │
    │     │            └────┬────┘
    └──┬──┘                 │
       │ (Builds & Pushes)  │
       │ Docker Image       │
    ┌──▼──────────┐         │
    │ DockerHub   │         │
    │             │         │
    └──┬──────────┘    ┌────▼─────────┐
       │                │ Kubernetes   │
       │                │ (fitverse)   │
       └────────────────┘ Cluster      │
            (Pulls)       (Runs)       │
                         └─────────────┘
```

**Key Points:**
- Jenkins CI: Builds code → Tests → Scans → Pushes image to Docker Registry
- Jenkins CD OR ArgoCD: Updates manifests in Git
- ArgoCD pulls from Git every 3 minutes and deploys automatically
- Or use kubectl/helm to manually deploy from manifests

Choose GitOps (ArgoCD) for automatic sync, or traditional Jenkins CD for manual control!
