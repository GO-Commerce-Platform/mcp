# GO-Commerce MCP Server DevOps & Deployment Architecture

## Overview

This document outlines the DevOps practices and deployment architecture for the GO-Commerce MCP service. It covers containerization, orchestration, continuous integration/deployment, environment management, and operational procedures.

## 1. Containerization Strategy

### 1.1 Multi-stage Dockerfile

```dockerfile path=null start=null
# Build stage
FROM registry.access.redhat.com/ubi8/openjdk-21:latest AS builder

WORKDIR /build
COPY pom.xml .
COPY src src

# Build with maven
RUN ./mvnw package -DskipTests

# Runtime stage
FROM registry.access.redhat.com/ubi8/openjdk-21-runtime:latest

# Set working directory
WORKDIR /deployment

# Copy build artifacts
COPY --from=builder /build/target/quarkus-app/lib/ /deployments/lib/
COPY --from=builder /build/target/quarkus-app/*.jar /deployments/
COPY --from=builder /build/target/quarkus-app/app/ /deployments/app/
COPY --from=builder /build/target/quarkus-app/quarkus/ /deployments/quarkus/

# Set environment variables
ENV JAVA_OPTS="-XX:+UseZGC -XX:+ZGenerational -XX:MaxRAMPercentage=75.0"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

# Non-root user for security
USER 185

# Expose application port
EXPOSE 8080

# Set health check
HEALTHCHECK --interval=30s --timeout=3s \
    CMD curl -f http://localhost:8080/q/health/ready || exit 1

# Start the application
CMD ["java", ${JAVA_OPTS}, "-jar", ${JAVA_APP_JAR}]
```

### 1.2 Container Resource Management

```yaml path=null start=null
resources:
  requests:
    memory: "512Mi"
    cpu: "500m"
  limits:
    memory: "1Gi"
    cpu: "1000m"
```

## 2. Kubernetes Deployment

### 2.1 Base Deployment Configuration

```yaml path=null start=null
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mcp-server
  namespace: gocommerce
spec:
  replicas: 3
  selector:
    matchLabels:
      app: mcp-server
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    metadata:
      labels:
        app: mcp-server
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/path: "/metrics"
        prometheus.io/port: "8080"
    spec:
      containers:
      - name: mcp-server
        image: gocommerce/mcp-server:${VERSION}
        ports:
        - containerPort: 8080
        env:
        - name: QUARKUS_PROFILE
          value: "kubernetes"
        - name: JAVA_OPTS
          value: "-XX:+UseZGC -XX:+ZGenerational -XX:MaxRAMPercentage=75.0"
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        readinessProbe:
          httpGet:
            path: /q/health/ready
            port: 8080
          initialDelaySeconds: 15
          periodSeconds: 5
        livenessProbe:
          httpGet:
            path: /q/health/live
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        startupProbe:
          httpGet:
            path: /q/health/started
            port: 8080
          failureThreshold: 30
          periodSeconds: 10
```

### 2.2 Service Configuration

```yaml path=null start=null
apiVersion: v1
kind: Service
metadata:
  name: mcp-server
  namespace: gocommerce
spec:
  type: ClusterIP
  ports:
  - port: 8080
    targetPort: 8080
    protocol: TCP
    name: http
  selector:
    app: mcp-server
```

### 2.3 HorizontalPodAutoscaler Configuration

```yaml path=null start=null
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: mcp-server-hpa
  namespace: gocommerce
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: mcp-server
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
      - type: Percent
        value: 100
        periodSeconds: 15
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 100
        periodSeconds: 15
```

## 3. CI/CD Pipeline

### 3.1 GitHub Actions Workflow

```yaml path=null start=null
name: MCP Server CI/CD
on:
  push:
    branches: [ main ]
    tags: [ 'v*' ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
        
    - name: Cache Maven packages
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
        
    - name: Build with Maven
      run: ./mvnw -B package
      
    - name: Run Tests
      run: ./mvnw test
      
    - name: Run Integration Tests
      run: ./mvnw verify -P integration-test
      
    - name: Security Scan
      uses: anchore/scan-action@v3
      with:
        image: "gocommerce/mcp-server:${{ github.sha }}"
        
    - name: Build and Push Docker Image
      if: github.event_name != 'pull_request'
      uses: docker/build-push-action@v4
      with:
        context: .
        push: true
        tags: |
          gocommerce/mcp-server:latest
          gocommerce/mcp-server:${{ github.sha }}
          
    - name: Deploy to Development
      if: github.ref == 'refs/heads/main'
      uses: azure/k8s-deploy@v1
      with:
        namespace: gocommerce-dev
        manifests: |
          k8s/deployment.yaml
          k8s/service.yaml
          k8s/hpa.yaml
        images: |
          gocommerce/mcp-server:${{ github.sha }}
```

### 3.2 Quality Gates

```yaml path=null start=null
quality_gates:
  test_coverage:
    minimum: 80%
    critical: true
  
  code_quality:
    sonar:
      quality_gate: default
      critical: true
    
  security:
    dependency_check:
      maximum_critical: 0
      maximum_high: 0
      critical: true
    container_scan:
      maximum_critical: 0
      maximum_high: 0
      critical: true
    
  performance:
    load_test:
      p95_response_time: 500ms
      error_rate: 1%
      critical: false
```

## 4. Environment Configuration

### 4.1 Environment-Specific Properties

```properties path=null start=null
# Development Environment
%dev.quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/gocommerce
%dev.quarkus.hibernate-orm.database.generation=drop-and-create
%dev.quarkus.log.level=DEBUG

# Staging Environment
%staging.quarkus.datasource.jdbc.url=jdbc:postgresql://postgres-staging:5432/gocommerce
%staging.quarkus.hibernate-orm.database.generation=validate
%staging.quarkus.log.level=INFO

# Production Environment
%prod.quarkus.datasource.jdbc.url=jdbc:postgresql://postgres-prod:5432/gocommerce
%prod.quarkus.hibernate-orm.database.generation=validate
%prod.quarkus.log.level=INFO
%prod.quarkus.http.ssl.certificate.key-store-file=/etc/ssl/certs/mcp-server.jks
```

### 4.2 Environment Variables

```yaml path=null start=null
env:
  # Database Configuration
  - name: DB_USER
    valueFrom:
      secretKeyRef:
        name: db-credentials
        key: username
  - name: DB_PASSWORD
    valueFrom:
      secretKeyRef:
        name: db-credentials
        key: password
        
  # Redis Configuration
  - name: REDIS_HOST
    valueFrom:
      configMapKeyRef:
        name: redis-config
        key: host
  - name: REDIS_PORT
    valueFrom:
      configMapKeyRef:
        name: redis-config
        key: port
        
  # Keycloak Configuration
  - name: KEYCLOAK_URL
    valueFrom:
      configMapKeyRef:
        name: keycloak-config
        key: url
  - name: KEYCLOAK_REALM
    valueFrom:
      configMapKeyRef:
        name: keycloak-config
        key: realm
```

## 5. Infrastructure as Code

### 5.1 Terraform Configuration

```hcl path=null start=null
# Provider configuration
provider "kubernetes" {
  config_path = "~/.kube/config"
}

provider "helm" {
  kubernetes {
    config_path = "~/.kube/config"
  }
}

# Namespace
resource "kubernetes_namespace" "gocommerce" {
  metadata {
    name = "gocommerce"
    labels = {
      environment = var.environment
    }
  }
}

# PostgreSQL
resource "helm_release" "postgresql" {
  name       = "postgresql"
  repository = "https://charts.bitnami.com/bitnami"
  chart      = "postgresql"
  namespace  = kubernetes_namespace.gocommerce.metadata[0].name
  
  values = [
    file("${path.module}/values/postgresql.yaml")
  ]
  
  set {
    name  = "global.postgresql.auth.postgresPassword"
    value = var.postgres_password
  }
}

# Redis
resource "helm_release" "redis" {
  name       = "redis"
  repository = "https://charts.bitnami.com/bitnami"
  chart      = "redis"
  namespace  = kubernetes_namespace.gocommerce.metadata[0].name
  
  values = [
    file("${path.module}/values/redis.yaml")
  ]
}

# Monitoring stack
module "monitoring" {
  source = "./modules/monitoring"
  
  namespace = kubernetes_namespace.gocommerce.metadata[0].name
  environment = var.environment
}
```

### 5.2 Helm Chart Structure

```plaintext path=null start=null
mcp-server/
├── Chart.yaml
├── values.yaml
├── templates/
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── ingress.yaml
│   ├── configmap.yaml
│   ├── secret.yaml
│   ├── hpa.yaml
│   └── serviceaccount.yaml
└── charts/
    ├── postgresql/
    └── redis/
```

## 6. Blue-Green Deployment

### 6.1 Service Router Configuration

```yaml path=null start=null
apiVersion: v1
kind: Service
metadata:
  name: mcp-server-router
  namespace: gocommerce
spec:
  type: ClusterIP
  ports:
  - port: 8080
    targetPort: 8080
  selector:
    # Controlled by deployment strategy
    deployment: blue
```

### 6.2 Deployment Strategy

```yaml path=null start=null
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: mcp-server
spec:
  replicas: 3
  selector:
    matchLabels:
      app: mcp-server
  template:
    metadata:
      labels:
        app: mcp-server
    spec:
      containers:
      - name: mcp-server
        image: gocommerce/mcp-server:latest
  strategy:
    blueGreen:
      activeService: mcp-server-active
      previewService: mcp-server-preview
      autoPromotionEnabled: false
      prePromotionAnalysis:
        templates:
        - templateName: smoke-tests
      postPromotionAnalysis:
        templates:
        - templateName: performance-tests
      scaleDownDelaySeconds: 300
```

## 7. Monitoring & Alerting

### 7.1 Prometheus Service Monitor

```yaml path=null start=null
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: mcp-server
  namespace: gocommerce
spec:
  selector:
    matchLabels:
      app: mcp-server
  endpoints:
  - port: http
    path: /metrics
    interval: 15s
    scrapeTimeout: 14s
```

### 7.2 Grafana Dashboard Configuration

```yaml path=null start=null
apiVersion: v1
kind: ConfigMap
metadata:
  name: mcp-server-dashboard
  namespace: monitoring
  labels:
    grafana_dashboard: "true"
data:
  mcp-server-dashboard.json: |
    {
      "dashboard": {
        "title": "MCP Server Dashboard",
        "panels": [
          {
            "title": "Request Rate",
            "type": "graph",
            "datasource": "Prometheus",
            "targets": [
              {
                "expr": "rate(http_server_requests_seconds_count[5m])",
                "legendFormat": "{{method}} {{status}}"
              }
            ]
          }
        ]
      }
    }
```

## 8. Implementation Guidelines

### 8.1 Deployment Checklist

1. **Pre-deployment**
   - Run full test suite
   - Perform security scan
   - Update documentation
   - Review resource requests/limits

2. **Deployment Process**
   - Update deployment manifests
   - Apply database migrations
   - Deploy blue environment
   - Run smoke tests
   - Switch traffic
   - Monitor metrics

3. **Post-deployment**
   - Verify health checks
   - Check logging
   - Monitor performance
   - Scale if needed

### 8.2 Operation Guidelines

1. **Resource Management**
   - Monitor resource usage
   - Scale proactively
   - Cleanup unused resources
   - Optimize costs

2. **Security**
   - Regular updates
   - Security scanning
   - Access control review
   - Audit logging

3. **Backup Strategy**
   - Regular backups
   - Verification
   - Retention policy
   - Recovery testing

4. **Monitoring**
   - Alert configuration
   - Dashboard maintenance
   - Log analysis
   - Performance tracking

## 9. Operational Procedures

### 9.1 Deployment Procedures

```bash path=null start=null
#!/bin/bash

# Deploy to new environment
deploy_new_version() {
    VERSION=$1
    ENV=$2
    
    # Update version
    kubectl set image deployment/mcp-server \
        mcp-server=gocommerce/mcp-server:${VERSION} \
        -n gocommerce-${ENV}
        
    # Wait for rollout
    kubectl rollout status deployment/mcp-server \
        -n gocommerce-${ENV} \
        --timeout=10m
        
    # Verify deployment
    verify_deployment ${ENV}
}

# Rollback procedure
rollback_deployment() {
    ENV=$1
    
    # Rollback to previous version
    kubectl rollout undo deployment/mcp-server \
        -n gocommerce-${ENV}
        
    # Wait for rollback
    kubectl rollout status deployment/mcp-server \
        -n gocommerce-${ENV} \
        --timeout=5m
        
    # Verify rollback
    verify_deployment ${ENV}
}
```

### 9.2 Scaling Procedures

```bash path=null start=null
#!/bin/bash

# Scale deployment
scale_deployment() {
    REPLICAS=$1
    ENV=$2
    
    # Update replicas
    kubectl scale deployment/mcp-server \
        --replicas=${REPLICAS} \
        -n gocommerce-${ENV}
        
    # Wait for scaling
    kubectl rollout status deployment/mcp-server \
        -n gocommerce-${ENV} \
        --timeout=5m
        
    # Verify scaling
    verify_scaling ${ENV} ${REPLICAS}
}

# Auto-scaling configuration
configure_autoscaling() {
    MIN=$1
    MAX=$2
    ENV=$3
    
    # Update HPA
    kubectl patch hpa mcp-server-hpa \
        -n gocommerce-${ENV} \
        --patch "{\"spec\":{\"minReplicas\":${MIN},\"maxReplicas\":${MAX}}}"
}
```

// Copilot: This file may have been generated or refactored by GitHub Copilot.
