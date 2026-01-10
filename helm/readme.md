# Fitverse Helm Chart - Deployment Guide

## Prerequisites

1. **Kubernetes Cluster** (1.20+)
2. **Helm 3.x** installed
3. **Cert-Manager** (for HTTPS)
4. **Ingress Controller** (NGINX recommended)
5. **Persistent Storage** (if not using local volumes)

## Deployment Steps

### 1. Create Namespace (DevOps)
```bash
kubectl create namespace fitverse
```

### 2. Install Cert-Manager (if not present)
```bash
helm repo add jetstack https://charts.jetstack.io
helm repo update
helm install cert-manager jetstack/cert-manager \
  --namespace cert-manager --create-namespace \
  --set installCRDs=true
```

### 3. Create ClusterIssuer for Let's Encrypt
```bash
kubectl apply -f - <<EOF
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: your-email@example.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: nginx
EOF
```

### 4. Create External Secrets (Using Sealed Secrets or Vault)
```bash
# Option A: Using kubectl (NOT production)
kubectl create secret generic fitverse-secrets \
  -n fitverse \
  --from-literal=MYSQL_ROOT_PASSWORD=your-secure-password \
  --from-literal=JASYPT_ENCRYPTOR_PASSWORD=your-encryption-key \
  --dry-run=client -o yaml | kubectl apply -f -

# Option B: Use Sealed Secrets (RECOMMENDED for production)
# Install: helm repo add sealed-secrets https://bitnami-labs.github.io/sealed-secrets
# Then seal your secrets
```

### 5. Deploy Chart
```bash
# Create values override file
cat > custom-values.yaml <<EOF
global:
  environment: production
mysql:
  storage:
    storageClassName: "ebs"  # Use your storage class
backend:
  image:
    tag: "1.0.1"
  replicas: 3
frontend:
  image:
    tag: "1.0.0"
  replicas: 3
ingress:
  host: "fitverse.example.com"  # YOUR DOMAIN
EOF

# Install
helm install fitverse . -n fitverse -f custom-values.yaml

# Or upgrade
helm upgrade fitverse . -n fitverse -f custom-values.yaml
```

### 6. Verify Deployment
```bash
kubectl get all -n fitverse
kubectl logs -n fitverse -l app=fitverse-backend
kubectl describe ingress -n fitverse
```

## Helm Variables Reference

| Variable | Type | Location | Notes |
|----------|------|----------|-------|
| `.Release.Namespace` | Built-in | Auto-populated | Kubernetes namespace of release |
| `.Release.Name` | Built-in | Auto-populated | Name given to helm release |
| `.Chart.Name` | Built-in | Auto-populated | Chart name from Chart.yaml |
| `.Chart.AppVersion` | Built-in | Auto-populated | App version from Chart.yaml |
| `.Values.*` | User-defined | values.yaml | User-configurable values |

## Security Checklist

- [ ] Override default passwords in secrets
- [ ] Configure HTTPS/TLS with valid certificate
- [ ] Set proper resource limits (prevents DoS)
- [ ] Enable RBAC and NetworkPolicies
- [ ] Use private container registries
- [ ] Implement Pod Security Standards
- [ ] Configure liveness/readiness probes
- [ ] Enable audit logging

## Troubleshooting

**Pod not starting?**
```bash
kubectl describe pod <pod-name> -n fitverse
kubectl logs <pod-name> -n fitverse
```

**Database connection issues?**
```bash
kubectl exec -it <backend-pod> -n fitverse -- sh
# Inside pod: nc -zv fitverse-mysql 3306
```

**Ingress not working?**
```bash
kubectl get ingress -n fitverse -o yaml
# Verify cert status: kubectl get certificate -n fitverse
