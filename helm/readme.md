```shell
helm install fitverse-backend ./fitverse-backend-helm -n dev --create-namespace
helm lint ./fitverse-backend-helm
helm template fitverse-backend ./fitverse-backend-helm
helm install fitverse-backend ./fitverse-backend-helm
```

🟢 Basic Helm Commands
```shell
helm version
helm help

helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo list
helm repo update

helm search repo nginx
helm search hub wordpress

helm install my-nginx bitnami/nginx
helm uninstall my-nginx

helm list
helm list -A        # all namespaces

helm status my-nginx
helm get all my-nginx

helm install my-nginx bitnami/nginx -n web --create-namespace

helm upgrade my-nginx bitnami/nginx
helm rollback my-nginx 1

helm install my-nginx bitnami/nginx --dry-run --debug

helm install my-nginx bitnami/nginx -f values.yaml
helm upgrade my-nginx bitnami/nginx -f values.yaml

helm install my-nginx bitnami/nginx \
  --set service.type=NodePort

helm show values bitnami/nginx

helm history my-nginx

helm create mychart
helm lint mychart
helm template my-nginx bitnami/nginx
helm template mychart ./mychart
helm install mychart ./mychart --dry-run
helm dependency list
helm dependency update
helm dependency build
helm package mychart
helm push mychart-0.1.0.tgz myrepo
helm pull bitnami/nginx
helm pull bitnami/nginx --untar
helm upgrade my-nginx bitnami/nginx \
  --atomic --timeout 5m
helm upgrade my-nginx bitnami/nginx --force
helm secrets install myapp ./chart
```

```shell
# DEBUGGING FRONTEND (USEFUL)
kubectl exec -it fitverse-frontend-847bd6f566-75xmm -n fitverse -- sh
netstat -tlnp # check in which port frontend is listening

# Few useful command (HELM + KUBECTL)
helm template 3-tier-app ./3-tier-app-chart
kubectl port-forward pod/fitverse-frontend-847bd6f566-75xmm -n fitverse 4200:8080 --address=0.0.0.0 &
kubectl describe pod fitverse-frontend-847bd6f566-75xmm -n fitverse
kubectl logs fitverse-frontend-847bd6f566-75xmm -n fitverse
watch kubectl logs fitverse-frontend-847bd6f566-75xmm -n fitverse

kubectl get pods -n fitverse
kubectl get svc -n fitverse

helm install 3-tier-app ./3-tier-app-chart -n fitverse --create-namespace
helm uninstall 3-tier-app
helm uninstall 3-tier-app -n fitverse

kubectl exec -it fitverse-mysql-0 -n fitverse -- bash

```