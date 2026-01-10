### Used commands
```bash
  helm template fitverse ./helm -n fitverse
  helm install fitverse ./helm -n fitverse --dry-run --debug
  helm install fitverse ./helm -n fitverse --create-namespace
  helm list -n fitverse
  
  kubectl get pods -n fitverse
  kubectl get svc -n fitverse
  kubectl describe pod <pod> -n fitverse
  kubectl logs <pod> -n fitverse
  
  lsof -i :8181
  netstat -tulpn | grep 8181
  ss -tulpn | grep 8181
  kill <PID>
  
  kubectl port-forward svc/fitverse-backend -n fitverse 8181:8181 --address=0.0.0.0 &
  kubectl port-forward svc/fitverse-frontend -n fitverse 8181:8181 --address=0.0.0.0 &
  
  helm uninstall fitverse -n fitverse
```

```bash
# ARGO CD INSTALL

# Create the namespace
kubectl create namespace argocd

# Install the standard ArgoCD components
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```

```yaml
- name: Create .env file
  run: |
      echo "SPRING_DATASOURCE_DATABASE=${{ secrets.DB_NAME }}" >> .env
      echo "SPRING_DATASOURCE_PASSWORD=${{ secrets.DB_PASS }}" >> .env
      echo "JASYPT_ENCRYPTOR_PASSWORD=${{ secrets.JASYPT_KEY }}" >> .env
```

OR
--
```yaml
- name: Build and Push Docker Image
  env:
    SPRING_DATASOURCE_PASSWORD: ${{ secrets.SPRING_DATASOURCE_PASSWORD }}
  run: |
    docker compose build
```

```shell
# Forgot to pull before push
git pull --rebase origin main
git push -U origin main
```