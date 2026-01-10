# FitVerse Helm Chart - Production Operations Guide

## Table of Contents
1. [Installation](#installation)
2. [Multi-Environment Deployment](#multi-environment-deployment)
3. [Scaling Operations](#scaling-operations)
4. [Upgrades & Rollbacks](#upgrades--rollbacks)
5. [Monitoring & Debugging](#monitoring--debugging)
6. [Maintenance & Cleanup](#maintenance--cleanup)
7. [Disaster Recovery](#disaster-recovery)
8. [Security Operations](#security-operations)
9. [Troubleshooting](#troubleshooting)

---

## Installation

### Prerequisites
```bash
# 1. Install Helm (v3.12+)
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# 2. Verify installation
helm version
kubectl version

# 3. Install Ingress Controller (if not present)
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm install nginx-ingress ingress-nginx/ingress-nginx -n ingress-nginx --create-namespace

# 4. Install Cert-Manager for TLS (if not present)
helm repo add jetstack https://charts.jetstack.io
helm install cert-manager jetstack/cert-manager -n cert-manager --create-namespace \
  --set installCRDs=true
```

### Fresh Installation
```bash
# Step 1: Create namespace
kubectl create namespace fitverse

# Step 2: Add Helm chart repo (if using remote chart registry)
helm repo add fitverse https://your-registry.com
helm repo update

# Step 3: Install with default production values
helm install fitverse ./helm -n fitverse \
  --values values.yaml \
  --set global.environment=production

# Step 4: Verify installation
helm status fitverse -n fitverse
kubectl get all -n fitverse
kubectl get ingress -n fitverse
```

---

## Multi-Environment Deployment

### Environment Structure
```
helm/
├── values.yaml                 # Base/Production values
├── values-dev.yaml             # Development overrides
├── values-staging.yaml         # Staging overrides
└── values-prod.yaml            # Production overrides
```

### Development Environment
```bash
# Install with dev values (lower replicas, resources)
helm install fitverse ./helm -n fitverse-dev \
  --create-namespace \
  --values values-dev.yaml

# Command breakdown:
# - Replicas: 1 (no HA needed)
# - Resources: minimal (reduce costs)
# - Autoscaling: disabled
# - Ingress: basic HTTP only
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
  autoscaling:
    enabled: false
  resources:
    requests:
      cpu: 250m
      memory: 512Mi
    limits:
      cpu: 500m
      memory: 1Gi
  storage:
    size: 5Gi
    storageClassName: "standard"

ingress:
  host: "fitverse-dev.example.com"
```

### Staging Environment
```bash
# Install with staging values (balanced HA)
helm install fitverse ./helm -n fitverse-staging \
  --create-namespace \
  --values values-staging.yaml \
  --set mysql.storage.size=50Gi \
  --set backend.autoscaling.maxReplicas=3
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
    storageClassName: "fast-ssd"  # Use faster storage

ingress:
  host: "fitverse-staging.example.com"
```

### Production Environment
```bash
# Install with prod values (full HA, monitoring, backups)
helm install fitverse ./helm -n fitverse-prod \
  --create-namespace \
  --values values-prod.yaml \
  --set-string mysql.environment.MYSQL_ROOT_PASSWORD=$PROD_DB_PASSWORD \
  --set-string backend.environment.SPRING_DATASOURCE_PASSWORD=$PROD_DB_PASSWORD \
  --set ingress.host="fitverse.example.com"
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
      schedule: "0 2 * * *"  # Daily at 2 AM

ingress:
  host: "fitverse.example.com"
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
```

---

## Scaling Operations

### Manual Scaling
```bash
# Scale backend replicas
kubectl scale statefulset fitverse-backend --replicas=5 -n fitverse-prod

# Scale frontend replicas
kubectl scale deployment fitverse-frontend --replicas=4 -n fitverse-prod

# Scale database (NOT recommended for StatefulSet - use operator)
kubectl scale statefulset fitverse-mysql --replicas=3 -n fitverse-prod
```

### Horizontal Pod Autoscaling (HPA)
```bash
# Check HPA status
kubectl get hpa -n fitverse-prod

# View HPA details
kubectl describe hpa fitverse-backend-hpa -n fitverse-prod

# Edit HPA targets
kubectl edit hpa fitverse-backend-hpa -n fitverse-prod
```

### Update Autoscaling via Helm
```bash
# Increase max replicas for backend
helm upgrade fitverse ./helm -n fitverse-prod \
  --reuse-values \
  --set backend.autoscaling.maxReplicas=15 \
  --set backend.autoscaling.targetCPUUtilizationPercentage=60

# Check current scaling metrics
kubectl top nodes -n fitverse-prod
kubectl top pods -n fitverse-prod
```

### Cluster Node Scaling (Infrastructure)
```bash
# Scale your K8s cluster nodes (GKE example)
gcloud container clusters resize fitverse-cluster --num-nodes=5 --zone=us-central1-a

# Or with Terraform/IaC
# Update your infrastructure and apply changes
terraform apply
```

---

## Upgrades & Rollbacks

### Pre-Upgrade Checklist
```bash
# 1. Backup database
kubectl exec -it fitverse-mysql-0 -n fitverse-prod -- \
  mysqldump -uroot -p$MYSQL_ROOT_PASSWORD fitverse > backup-$(date +%s).sql

# 2. Check current release status
helm status fitverse -n fitverse-prod

# 3. Get current values
helm get values fitverse -n fitverse-prod > current-values.yaml

# 4. List deployment history
helm history fitverse -n fitverse-prod
```

### Upgrade with New Image Versions
```bash
# Upgrade backend image only
helm upgrade fitverse ./helm -n fitverse-prod \
  --reuse-values \
  --set backend.image.tag=2.0.0 \
  --wait \
  --timeout=5m

# Upgrade frontend image only
helm upgrade fitverse ./helm -n fitverse-prod \
  --reuse-values \
  --set frontend.image.tag=2.0.0 \
  --wait

# Upgrade all components with new values file
helm upgrade fitverse ./helm -n fitverse-prod \
  --values values-prod-v2.yaml \
  --wait \
  --timeout=10m
```

### Upgrade with Rolling Update Strategy
```bash
# Monitor rolling update in real-time
kubectl rollout status statefulset fitverse-backend -n fitverse-prod -w

# Check rollout history
kubectl rollout history statefulset fitverse-backend -n fitverse-prod

# Describe deployment to see update progress
kubectl describe deployment fitverse-frontend -n fitverse-prod | tail -20
```

### Rollback to Previous Release
```bash
# Rollback to previous release
helm rollback fitverse -n fitverse-prod

# Rollback to specific revision (e.g., revision 2)
helm rollback fitverse 2 -n fitverse-prod

# Verify rollback
helm status fitverse -n fitverse-prod
kubectl get pods -n fitverse-prod
```

### Zero-Downtime Upgrade Strategy
```bash
# 1. Ensure HPA is disabled during critical updates
helm upgrade fitverse ./helm -n fitverse-prod \
  --reuse-values \
  --set backend.autoscaling.enabled=false \
  --set backend.replicas=3 \
  --wait

# 2. Perform upgrade with maxSurge/maxUnavailable strategy
# (Already configured in templates for rolling updates)
helm upgrade fitverse ./helm -n fitverse-prod \
  --values values-prod.yaml \
  --wait \
  --timeout=15m \
  --atomic  # Rollback automatically if upgrade fails

# 3. Re-enable HPA
helm upgrade fitverse ./helm -n fitverse-prod \
  --reuse-values \
  --set backend.autoscaling.enabled=true
```

---

## Monitoring & Debugging

### Status Checks
```bash
# Check release status
helm status fitverse -n fitverse-prod

# List all resources
kubectl get all -n fitverse-prod

# Get detailed pod info
kubectl get pods -n fitverse-prod -o wide

# Watch pod status in real-time
kubectl get pods -n fitverse-prod -w
```

### View Logs
```bash
# Backend pod logs
kubectl logs -n fitverse-prod -l app=fitverse,component=backend -f --tail=100

# Frontend pod logs
kubectl logs -n fitverse-prod -l app=fitverse,component=frontend -f --tail=100

# MySQL pod logs
kubectl logs -n fitverse-prod -l app=fitverse,component=mysql -f

# Get logs from all containers in a pod
kubectl logs pod/fitverse-backend-0 -n fitverse-prod --all-containers=true

# Get logs from previous pod (if crashed)
kubectl logs pod/fitverse-backend-0 -n fitverse-prod --previous
```

### Debugging Commands
```bash
# Describe pod for events
kubectl describe pod fitverse-backend-0 -n fitverse-prod

# Check resource usage
kubectl top pods -n fitverse-prod
kubectl top nodes

# Execute into pod
kubectl exec -it fitverse-backend-0 -n fitverse-prod -- /bin/sh

# Check service endpoints
kubectl get endpoints -n fitverse-prod

# DNS resolution test
kubectl run -it --rm debug --image=busybox --restart=Never -- \
  nslookup fitverse-mysql.fitverse-prod.svc.cluster.local

# Network connectivity test
kubectl run -it --rm debug --image=curlimages/curl --restart=Never -- \
  curl http://fitverse-backend:8181/health
```

### Helm Debugging
```bash
# Template rendering (dry-run)
helm template fitverse ./helm -n fitverse-prod --values values-prod.yaml

# Install/upgrade with debug
helm install fitverse ./helm -n fitverse-prod --debug --dry-run

# Check manifest differences
helm diff upgrade fitverse ./helm -n fitverse-prod --values values-prod.yaml
```

### Prometheus Metrics
```bash
# Port-forward to Prometheus (if installed)
kubectl port-forward svc/prometheus 9090:9090 -n monitoring

# Query metrics in browser: localhost:9090
# Example queries:
# - cpu_usage_percent
# - memory_usage_bytes
# - http_requests_total
# - pod_restart_count
```

---

## Maintenance & Cleanup

### Regular Maintenance
```bash
# Lint chart for issues
helm lint ./helm

# Check for deprecated API versions
kubectl api-resources

# Verify TLS certificates
kubectl get certificate -n fitverse-prod
kubectl describe certificate fitverse-tls -n fitverse-prod

# Check resource usage trends
kubectl top pods --all-namespaces --sort-by=memory
```

### Cleanup Old Releases
```bash
# List all releases
helm list -n fitverse-prod

# Delete old release revision history (keep last 5)
helm history fitverse -n fitverse-prod --max=5

# Uninstall release but keep history
helm uninstall fitverse -n fitverse-prod --keep-history

# Fully uninstall with all history
helm uninstall fitverse -n fitverse-prod
```

### Clean Up Resources
```bash
# Delete pods in CrashLoopBackOff
kubectl delete pod -n fitverse-prod --field-selector=status.phase=Failed

# Remove stuck pods
kubectl delete pod <pod-name> -n fitverse-prod --grace-period=0 --force

# Clean PersistentVolumeClaims (caution: deletes data)
kubectl delete pvc -n fitverse-prod -l app=fitverse

# Clean all resources in namespace
kubectl delete all --all -n fitverse-prod
```

---

## Disaster Recovery

### Database Backup & Restore

**Automated Backup (Configure in StatefulSet):**
```bash
# Enable backups in values
helm upgrade fitverse ./helm -n fitverse-prod \
  --reuse-values \
  --set mysql.backup.enabled=true \
  --set mysql.backup.schedule="0 2 * * *"
```

**Manual Backup:**
```bash
# Backup MySQL
kubectl exec fitverse-mysql-0 -n fitverse-prod -- \
  mysqldump -uroot -p$MYSQL_ROOT_PASSWORD --all-databases > backup.sql

# Backup to cloud storage (GCS example)
kubectl exec fitverse-mysql-0 -n fitverse-prod -- \
  mysqldump -uroot -p$MYSQL_ROOT_PASSWORD fitverse | \
  gsutil cp - gs://fitverse-backups/backup-$(date +%s).sql
```

**Restore from Backup:**
```bash
# Restore MySQL database
kubectl exec -i fitverse-mysql-0 -n fitverse-prod -- \
  mysql -uroot -p$MYSQL_ROOT_PASSWORD < backup.sql

# Restore from cloud storage
gsutil cat gs://fitverse-backups/backup-1234567890.sql | \
  kubectl exec -i fitverse-mysql-0 -n fitverse-prod -- \
  mysql -uroot -p$MYSQL_ROOT_PASSWORD
```

### Pod/Volume Recovery
```bash
# Restart pod
kubectl delete pod fitverse-backend-0 -n fitverse-prod

# Restart all backend pods
kubectl delete pods -l app=fitverse,component=backend -n fitverse-prod

# Check PersistentVolume status
kubectl get pv,pvc -n fitverse-prod

# Describe PVC to check events
kubectl describe pvc fitverse-mysql-pvc-0 -n fitverse-prod
```

### Full Cluster Recovery
```bash
# Re-deploy entire stack from scratch
helm repo update
helm install fitverse ./helm -n fitverse-prod \
  --values values-prod.yaml \
  --wait \
  --timeout=10m

# Verify health
kubectl get all -n fitverse-prod
helm status fitverse -n fitverse-prod
```

---

## Security Operations

### Update Secrets
```bash
# Update database password (via Sealed Secrets)
kubectl create secret generic fitverse-secrets \
  --from-literal=MYSQL_ROOT_PASSWORD=new-secure-password \
  --dry-run=client -o yaml | kubeseal -o yaml | kubectl apply -f -

# Verify secrets
kubectl get secrets -n fitverse-prod
kubectl describe secret fitverse-mysql-secret -n fitverse-prod
```

### RBAC & Access Control
```bash
# Check RBAC bindings
kubectl get rolebindings,clusterrolebindings -n fitverse-prod

# Create ServiceAccount for external apps
kubectl create serviceaccount fitverse-external -n fitverse-prod
kubectl create rolebinding fitverse-external-rb \
  --clusterrole=view \
  --serviceaccount=fitverse-prod:fitverse-external

# Get ServiceAccount token
kubectl get secret -n fitverse-prod \
  $(kubectl get secret -n fitverse-prod | grep fitverse-external-token | awk '{print $1}') \
  -o jsonpath='{.data.token}' | base64 -d
```

### Network Policy Verification
```bash
# Check network policies
kubectl get networkpolicies -n fitverse-prod

# Test network connectivity
kubectl exec -it fitverse-backend-0 -n fitverse-prod -- \
  ping fitverse-mysql

# Verify ingress policies
kubectl get ingress -n fitverse-prod
kubectl describe ingress fitverse-ingress -n fitverse-prod
```

### Certificate Management
```bash
# Check certificate status
kubectl get certificate -n fitverse-prod

# Renew certificate manually
kubectl delete secret fitverse-tls -n fitverse-prod
kubectl delete certificate fitverse -n fitverse-prod
# Cert-manager will auto-recreate

# View certificate details
kubectl get secret fitverse-tls -n fitverse-prod -o jsonpath='{.data.tls\.crt}' | \
  base64 -d | openssl x509 -text -noout
```

---

## Troubleshooting

### Common Issues & Solutions

**1. Pods in CrashLoopBackOff**
```bash
# Check logs
kubectl logs fitverse-backend-0 -n fitverse-prod --previous

# Describe pod for events
kubectl describe pod fitverse-backend-0 -n fitverse-prod

# Solution: Fix image tag, environment variables, or resource limits
helm upgrade fitverse ./helm -n fitverse-prod \
  --reuse-values \
  --set backend.image.tag=1.0.1
```

**2. Database Connection Failed**
```bash
# Verify MySQL pod is running
kubectl get pods -n fitverse-prod -l component=mysql

# Test MySQL connectivity
kubectl run -it --rm debug --image=mysql:8.0 --restart=Never -- \
  mysql -h fitverse-mysql -uroot -p$MYSQL_ROOT_PASSWORD -e "SELECT 1"

# Check database volume mount
kubectl exec fitverse-mysql-0 -n fitverse-prod -- ls -la /var/lib/mysql
```

**3. Ingress not routing traffic**
```bash
# Verify ingress exists
kubectl get ingress -n fitverse-prod

# Check ingress controller logs
kubectl logs -n ingress-nginx -l app.kubernetes.io/name=ingress-nginx

# Verify backend service endpoints
kubectl get endpoints fitverse-backend -n fitverse-prod
kubectl get endpoints fitverse-frontend -n fitverse-prod

# Test DNS resolution
nslookup fitverse.example.com
```

**4. PVC Stuck in Pending**
```bash
# Check PVC status
kubectl describe pvc fitverse-mysql-pvc-0 -n fitverse-prod

# Check StorageClass
kubectl get storageclass

# If no PVs available, create manually:
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

**5. High Memory Usage**
```bash
# Check resource usage
kubectl top pods -n fitverse-prod --sort-by=memory

# Reduce memory limits
helm upgrade fitverse ./helm -n fitverse-prod \
  --reuse-values \
  --set backend.resources.limits.memory=1Gi

# Restart to apply limits
kubectl rollout restart statefulset fitverse-backend -n fitverse-prod
```

---

## Useful Helm Commands Reference

```bash
# Chart Management
helm repo add <name> <url>           # Add chart repository
helm repo update                     # Update chart repositories
helm search repo fitverse            # Search charts
helm pull <chart>                    # Download chart

# Release Management
helm install <release> <chart>       # Install release
helm upgrade <release> <chart>       # Upgrade release
helm list                            # List releases
helm uninstall <release>             # Uninstall release
helm status <release>                # Get release status
helm history <release>               # Release history
helm rollback <release> [REVISION]   # Rollback to revision

# Debugging
helm get values <release>            # Get release values
helm get manifest <release>          # Get release manifest
helm template <chart>                # Render templates locally
helm lint <chart>                    # Lint chart
helm diff upgrade <release> <chart>  # Diff before upgrade (requires plugin)
```

---

## Cheat Sheet: Quick Commands

```bash
# Development deployment
helm install fitverse ./helm -n fitverse-dev --create-namespace --values values-dev.yaml

# Production deployment
helm install fitverse ./helm -n fitverse-prod --create-namespace --values values-prod.yaml

# Upgrade with new image
helm upgrade fitverse ./helm -n fitverse-prod --set backend.image.tag=2.0.0 --wait

# Rollback last upgrade
helm rollback fitverse -n fitverse-prod

# Scale backend
kubectl scale statefulset fitverse-backend --replicas=5 -n fitverse-prod

# Check logs
kubectl logs -l app=fitverse,component=backend -n fitverse-prod -f

# Check resources
kubectl top pods -n fitverse-prod

# Port-forward to backend
kubectl port-forward svc/fitverse-backend 8181:8181 -n fitverse-prod

# Backup database
kubectl exec fitverse-mysql-0 -n fitverse-prod -- mysqldump -uroot -p$MYSQL_ROOT_PASSWORD fitverse > backup.sql
