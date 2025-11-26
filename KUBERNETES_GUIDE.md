# Kubernetes Deployment Guide for JHipster App

## Prerequisites

- Kubernetes cluster (minikube, kind, or cloud K8s)
- `kubectl` installed and configured
- Docker image built and available to Kubernetes

## Step 1: Build and Make Image Available to Kubernetes

### Option A: Using Minikube (Local Development)

```bash
# Start minikube
minikube start

# Point Docker to minikube's Docker daemon
eval $(minikube docker-env)

# Build the image (now it's in minikube's Docker)
docker build -t jhipster-app:k8s .

# Verify
docker images | grep jhipster-app
```

### Option B: Using Docker Registry (Production)

```bash
# Tag and push to registry
docker tag jhipster-app:latest your-registry/jhipster-app:latest
docker push your-registry/jhipster-app:latest
```

## Step 2: Create Kubernetes Manifests

### PostgreSQL Deployment & Service

Create `kubernetes/postgresql.yaml`:

```yaml
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgresql
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgresql
  template:
    metadata:
      labels:
        app: postgresql
    spec:
      containers:
        - name: postgresql
          image: postgres:15
          env:
            - name: POSTGRES_DB
              value: jhipsterSampleApplication
            - name: POSTGRES_USER
              value: jhipsterSampleApplication
            - name: POSTGRES_PASSWORD
              value: password
          ports:
            - containerPort: 5432
          readinessProbe:
            exec:
              command:
                - pg_isready
                - '-U'
                - jhipsterSampleApplication
            initialDelaySeconds: 5
            periodSeconds: 10
---
apiVersion: v1
kind: Service
metadata:
  name: postgresql
spec:
  type: ClusterIP
  selector:
    app: postgresql
  ports:
    - protocol: TCP
      port: 5432
      targetPort: 5432
```

### JHipster App Deployment & Service

Create `kubernetes/jhipster-app.yaml`:

```yaml
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: jhipster-app
spec:
  replicas: 2
  selector:
    matchLabels:
      app: jhipster-app
  template:
    metadata:
      labels:
        app: jhipster-app
    spec:
      containers:
        - name: jhipster-app
          image: jhipster-app:k8s # Use :latest for registry
          imagePullPolicy: Never # Use Always for registry
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: prod
            - name: SPRING_DATASOURCE_URL
              value: jdbc:postgresql://postgresql:5432/jhipsterSampleApplication
            - name: SPRING_DATASOURCE_USERNAME
              value: jhipsterSampleApplication
            - name: SPRING_DATASOURCE_PASSWORD
              value: password
          readinessProbe:
            httpGet:
              path: /management/health
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /management/health
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 15
      initContainers:
        - name: wait-for-postgresql
          image: busybox
          command:
            - sh
            - -c
            - |
              until nc -z postgresql 5432; do
                echo "Waiting for PostgreSQL..."
                sleep 3
              done
---
apiVersion: v1
kind: Service
metadata:
  name: jhipster-app
spec:
  type: LoadBalancer # Use NodePort for minikube, LoadBalancer for cloud
  selector:
    app: jhipster-app
  ports:
    - protocol: TCP
      port: 8080
      targetPort: 8080
```

## Step 3: Deploy to Kubernetes

```bash
# Deploy everything
kubectl apply -f kubernetes/

# Check deployment status
kubectl get deployments
kubectl get pods
kubectl get services

# Watch pods until they're ready
kubectl get pods -w
```

## Step 4: Access the Application

### Option A: Using Minikube

```bash
# Get the service URL
minikube service jhipster-app --url

# Or use minikube tunnel (for LoadBalancer)
minikube tunnel
# Then access via the external IP shown in: kubectl get svc jhipster-app
```

### Option B: Using NodePort

If using NodePort service type:

```bash
# Get the NodePort
kubectl get svc jhipster-app

# Access via: http://NODE_IP:NODE_PORT
# For minikube:
minikube ip  # Get the IP
# Then: http://MINIKUBE_IP:NODE_PORT
```

### Option C: Using Port Forward (Quick Testing)

```bash
# Forward local port 8080 to service
kubectl port-forward svc/jhipster-app 8080:8080

# Access at: http://localhost:8080
```

## Step 5: Verify Deployment

```bash
# Check pod logs
kubectl logs deployment/jhipster-app

# Check service endpoints
kubectl get endpoints jhipster-app

# Test health endpoint
kubectl exec -it deployment/jhipster-app -- curl http://localhost:8080/management/health
```

## Common Commands

```bash
# View all resources
kubectl get all

# Describe a resource
kubectl describe deployment jhipster-app
kubectl describe pod <pod-name>

# View logs
kubectl logs -f deployment/jhipster-app
kubectl logs -f deployment/postgresql

# Scale the app
kubectl scale deployment jhipster-app --replicas=3

# Update the deployment (after rebuilding image)
kubectl rollout restart deployment/jhipster-app

# Delete everything
kubectl delete -f kubernetes/
```

## Troubleshooting

**Pods not starting:**

```bash
kubectl describe pod <pod-name>
kubectl logs <pod-name>
```

**Image pull errors:**

- For minikube: Make sure you built image with `eval $(minikube docker-env)`
- For registry: Check image name and credentials

**Database connection issues:**

- Verify PostgreSQL pod is running: `kubectl get pods -l app=postgresql`
- Check service: `kubectl get svc postgresql`
- Test connection: `kubectl exec -it deployment/postgresql -- psql -U jhipsterSampleApplication -d jhipsterSampleApplication`

**Service not accessible:**

- Check service type and ports: `kubectl get svc jhipster-app`
- For LoadBalancer: Wait for external IP assignment
- For NodePort: Use correct port number
- Check firewall rules

## Production Considerations

1. **Use Secrets for passwords:**

```bash
kubectl create secret generic db-credentials \
  --from-literal=username=jhipsterSampleApplication \
  --from-literal=password=password
```

Then reference in deployment:

```yaml
env:
  - name: SPRING_DATASOURCE_USERNAME
    valueFrom:
      secretKeyRef:
        name: db-credentials
        key: username
```

2. **Use ConfigMaps for configuration**
3. **Set resource limits**
4. **Use Ingress instead of LoadBalancer**
5. **Enable horizontal pod autoscaling**
6. **Set up monitoring and logging**
