```shell
# During template rendering
helm template fitverse ./helm -n fitverse

# During actual deployment
helm install fitverse ./helm -n fitverse --create-namespace
```

# Production
```shell
# Step 1: Create namespace (optional with --create-namespace)
kubectl create namespace fitverse

# Step 2: Install with correct namespace
helm install fitverse ./helm -n fitverse

# Step 3: Verify
helm list -n fitverse
kubectl get all -n fitverse
```