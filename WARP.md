# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Overview

The GO-Commerce Model Context Protocol (MCP) service is a Quarkus-based microservice that provides a secure bridge enabling AI systems to access and interact with live, private, and tenant-specific information within the GO-Commerce ecosystem. The service implements schema-based multi-tenancy and exposes RESTful APIs for AI agent integration.

## Architecture

### Core Components

1. **Data Context Layer**: Centralizes multi-tenant data access across domains
2. **Event-Driven Updates**: Real-time data synchronization via Kafka
3. **Tenant Isolation**: Schema-per-store with Quarkus multi-tenancy
4. **Security**: JWT-based authentication with Keycloak

### Technology Stack

- **Framework**: Quarkus 3.23.4 with Java 21
- **Database**: PostgreSQL with schema-based multi-tenancy
- **Messaging**: Apache Kafka for event streaming
- **Caching**: Redis for distributed caching
- **Security**: Keycloak for OAuth2/OIDC
- **Monitoring**: OpenTelemetry + Prometheus metrics

## Quick Start

### Build and Run

```bash
# Development mode with hot reload (recommended)
mvn quarkus:dev
# With continuous testing
mvn quarkus:dev -Dquarkus.test.continuous-testing=enabled

# Alternative: Docker-based development
cd ../docker && docker-compose --env-file .env up -d mcp
cd ../docker && docker-compose --env-file .env logs -f mcp
```

### Testing

```bash
# Run all tests
./mvnw test

# Run specific test class
mvn test -Dtest=DataServiceTest

# Docker-based testing (recommended)
../docker/run-tests.sh mcp          # MCP tests only
../docker/run-tests.sh mcp-all      # MCP tests + lint
../docker/run-tests.sh mcp-coverage # Tests with coverage
```

### Code Style

```bash
# Run checkstyle
mvn checkstyle:check

# Apply store migrations (if needed)
../apply-store-migrations.sh
```

## Development Workflow

### Feature Development

1. **Branch Creation**
    ```bash
    git checkout -b feature/MCP-123-feature-name
    ```

2. **Development Mode**
    ```bash
    # Start dependencies only
    cd ../docker && docker-compose --env-file .env up -d postgres keycloak-db keycloak
    
    # Run MCP in dev mode
    mvn quarkus:dev
    ```

3. **Testing**
    ```bash
    # Run during development
    mvn quarkus:dev -Dquarkus.test.continuous-testing=enabled
    
    # Before commit
    ../docker/run-tests.sh mcp-all
    ```

### Code Organization

```plaintext
mcp/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── dev/tiodati/saas/gocommerce/mcp/
│   │   │       ├── core/           # Core MCP implementation
│   │   │       ├── tenant/         # Tenant management
│   │   │       ├── data/           # Data access layer
│   │   │       ├── security/       # Security components
│   │   │       ├── api/            # REST endpoints
│   │   │       └── config/         # Configuration
│   │   └── resources/
│   │       └── application.properties
│   └── test/
└── docs/
    └── architecture/
```

### Multi-Tenant Development

```java
// Set store context for operations
@RequiresStoreRole("store-admin")  // Auto-sets context
public ResponseEntity<?> storeEndpoint() {
    // Operations automatically use correct schema
}

// Manual context management
StoreContext.setCurrentStore(storeSchema);
try {
    // Perform operations
} finally {
    StoreContext.clear();
}
```

### Event-Driven Development

```java
// Event Producer
@Inject
@Channel("data-updates")
Emitter<JsonObject> dataEmitter;

// Event Consumer
@Incoming("data-updates")
public void handleDataUpdate(Message<JsonObject> message) {
    String storeId = message.getPayload().getString("storeId");
    StoreContext.setCurrentStore(storeId);
    // Process update
}
```

## Configuration

### Core Properties

```properties
# Application
quarkus.application.name=mcp-server
quarkus.http.port=8080

# Database
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/gocommerce
quarkus.datasource.username=${DB_USER}
quarkus.datasource.password=${DB_PASSWORD}

# Multi-tenancy
quarkus.hibernate-orm.multitenant=SCHEMA
quarkus.hibernate-orm.database.generation=none

# Metrics
quarkus.micrometer.export.prometheus.enabled=true
quarkus.micrometer.export.prometheus.path=/metrics
```

### Development Profile

```properties
# Development
%dev.quarkus.live-reload.enabled=true
%dev.quarkus.swagger-ui.enable=true

# Testing
%test.quarkus.datasource.db-kind=postgresql
%test.quarkus.datasource.username=test
%test.quarkus.datasource.password=test
```

## Monitoring

### Health Check

```bash
# Check service health
curl http://localhost:8080/q/health

# Metrics endpoint
curl http://localhost:8080/q/metrics
```

### Logging

- Application logs: `/var/log/mcp-server.log`
- Structured format: `%d{yyyy-MM-dd HH:mm:ss,SSS} %h %N[%i] %-5p [%c{1.}] (%t) %s%e%n`

## Troubleshooting

### Common Issues

1. **Schema Resolution Failures**
   - Verify JWT contains valid `storeId` claim
   - Check database schema exists
   - Ensure proper role assignments in Keycloak

2. **Event Processing Issues**
   - Check Kafka broker connectivity
   - Verify topic exists and permissions
   - Monitor consumer group lag

3. **Performance Issues**
   - Monitor connection pool usage
   - Check cache hit rates
   - Review query execution plans

### Diagnostics

```bash
# Check service status
docker ps | grep mcp

# View service logs
docker logs -f gocommerce_mcp_1

# Monitor Kafka lag
kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
    --describe --group mcp-consumer-group
```

## References

- [Core Architecture](./docs/architecture/core-architecture.md)
- [Technical Stack](./docs/architecture/technical-stack.md)
- [ADRs](./docs/architecture/adr/)
- [Runbooks](./docs/operations/runbooks.md)

// Copilot: This file may have been generated or refactored by GitHub Copilot.
