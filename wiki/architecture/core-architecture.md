# GO-Commerce MCP Server Core Architecture Specification

## Overview

The GO-Commerce Model Context Protocol (MCP) service is designed as a secure bridge enabling AI systems to access and interact with live, private, and tenant-specific information within the GO-Commerce ecosystem. This document outlines the core architectural decisions and implementation patterns for the service.

## 1. Service Architecture

### 1.1 Core Components

The MCP service is structured around three primary components:

1. **MCP Host (External)**: LLM environment (e.g., Gemini) that initiates queries
2. **MCP Client (External)**: Component within LLM host that translates requests
3. **MCP Server (Internal)**: Custom Quarkus microservice handling tenant-aware data access

### 1.2 Technology Stack

- **Framework**: Quarkus 3.23.4+
- **Language**: Java 21
- **Database**: PostgreSQL with schema-based multi-tenancy
- **Security**: Keycloak for OAuth2/OIDC
- **Event Bus**: Apache Kafka for event streaming
- **Caching**: Redis for distributed caching

### 1.3 Core Features

- Tenant-aware data access and isolation
- Real-time data retrieval and context augmentation
- Event-driven updates via Kafka streams
- Role-based access control (RBAC)
- Audit logging and request tracing

## 2. Multi-Tenant Architecture

### 2.1 Tenant Isolation Strategy

The service implements the "Shared Database, Separate Schemas" pattern:

```java path=null start=null
@ApplicationScoped
@PersistenceUnitExtension
public class UnifiedTenantResolver implements TenantResolver {
    @Inject
    HttpServerRequest request;
    
    @Override
    public String resolveTenantId() {
        // Resolve tenant from JWT claims or request context
        return getCurrentTenant();
    }
}
```

Configuration in application.properties:
```properties path=null start=null
# Multi-tenant configuration
quarkus.hibernate-orm.multitenant=SCHEMA
quarkus.hibernate-orm.database.generation=none
```

### 2.2 Tenant Resolution Workflow

1. Incoming Request → JWT Validation
2. Tenant ID Extraction from JWT Claims
3. Schema Resolution via TenantResolver
4. Automatic Schema Switching by Quarkus
5. Data Access with Tenant Isolation

## 3. Data Context Layer

### 3.1 Unified Data Access

The Data Context Layer serves as a centralized point for accessing multi-tenant data across different domains:

```java path=null start=null
@ApplicationScoped
public class DataContextService {
    @Inject
    ProductRepository productRepo;
    
    @Inject
    OrderRepository orderRepo;
    
    @Inject
    CustomerRepository customerRepo;
    
    public <T> List<T> queryDomain(String domain, QueryCriteria criteria) {
        // Domain-specific query routing with tenant context
        return switch (domain) {
            case "products" -> productRepo.findByCriteria(criteria);
            case "orders" -> orderRepo.findByCriteria(criteria);
            case "customers" -> customerRepo.findByCriteria(criteria);
            default -> throw new UnsupportedDomainException(domain);
        };
    }
}
```

### 3.2 Repository Pattern Implementation

```java path=null start=null
@ApplicationScoped
public abstract class TenantAwareRepository<T extends PanacheEntity> {
    @Inject
    TenantContext tenantContext;
    
    protected String getCurrentSchema() {
        return tenantContext.getCurrentTenantId();
    }
    
    public List<T> findByCriteria(QueryCriteria criteria) {
        return find(criteria.toQuery(), criteria.getParameters())
            .page(Page.of(criteria.getOffset(), criteria.getLimit()))
            .list();
    }
}
```

## 4. Concurrency Model

### 4.1 Virtual Thread Usage

The service leverages Java 21's virtual threads for I/O-bound operations:

```java path=null start=null
@ApplicationScoped
public class AsyncDataService {
    public CompletableFuture<List<Data>> fetchDataAsync(String tenantId, String query) {
        return CompletableFuture.supplyAsync(() -> {
            // I/O-bound operation automatically uses virtual thread
            return fetchData(tenantId, query);
        });
    }
}
```

### 4.2 Reactive vs Blocking Operations

- **Reactive**: Event streaming, real-time updates, WebSocket connections
- **Blocking**: Complex database queries, external API calls (using virtual threads)
- **Hybrid**: Combining reactive event processing with blocking operations when needed

## 5. MCP Protocol Implementation

### 5.1 Request Processing Pipeline

1. Authentication & Authorization
2. Tenant Resolution
3. Request Validation
4. Context Loading
5. Data Access
6. Response Formation

### 5.2 Context Loading Pattern

```java path=null start=null
@ApplicationScoped
public class ContextLoader {
    @Inject
    DataContextService dataContext;
    
    public MCPContext loadContext(MCPRequest request) {
        return MCPContext.builder()
            .withTenantData(dataContext.queryDomain(request.getDomain()))
            .withUserContext(request.getUserContext())
            .withTimeScope(request.getTimeScope())
            .build();
    }
}
```

## 6. Integration Patterns

### 6.1 Event-Driven Updates

```java path=null start=null
@ApplicationScoped
public class KafkaEventHandler {
    @Incoming("data-updates")
    public void handleDataUpdate(DataUpdateEvent event) {
        // Process updates and notify relevant components
    }
}
```

### 6.2 External Service Integration

- REST API endpoints for synchronous operations
- Kafka topics for asynchronous event handling
- WebSocket connections for real-time updates

## 7. Scalability Considerations

### 7.1 Horizontal Scaling

- Stateless service design
- Distributed caching with Redis
- Connection pooling with PgBouncer
- Kafka for event distribution

### 7.2 Performance Optimization

- Query optimization through prepared statements
- Efficient tenant context switching
- Connection pool management
- Cache strategy implementation

## 8. Error Handling

### 8.1 Exception Hierarchy

```java path=null start=null
public sealed class MCPException extends RuntimeException
    permits TenantResolutionException, DataAccessException, SecurityException {
    // Common exception handling logic
}
```

### 8.2 Error Response Format

```json path=null start=null
{
    "error": {
        "code": "TENANT_NOT_FOUND",
        "message": "Unable to resolve tenant context",
        "details": {
            "requestId": "123e4567-e89b-12d3-a456-426614174000",
            "timestamp": "2025-09-02T01:38:49Z"
        }
    }
}
```

## 9. Monitoring and Observability

### 9.1 Metrics Collection

- Request latency per tenant
- Database connection pool usage
- Cache hit/miss rates
- Event processing throughput

### 9.2 Distributed Tracing

- OpenTelemetry integration
- Correlation ID propagation
- Tenant-aware trace sampling

## 10. Development Guidelines

### 10.1 Code Organization

```
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

### 10.2 Testing Strategy

- Integration tests with @QuarkusTest
- Multi-tenant aware test infrastructure
- Performance testing scenarios
- Security compliance tests

## 11. Security Measures

### 11.1 Authentication Flow

1. JWT validation
2. Tenant resolution
3. Role verification
4. Permission checking

### 11.2 Data Access Security

- Schema isolation
- Row-level security
- Audit logging
- Access control enforcement

## 12. Deployment Strategy

### 12.1 Container Configuration

```dockerfile path=null start=null
FROM registry.access.redhat.com/ubi8/openjdk-21:latest
COPY --chown=185 target/quarkus-app/lib/ /deployments/lib/
COPY --chown=185 target/quarkus-app/*.jar /deployments/
COPY --chown=185 target/quarkus-app/app/ /deployments/app/
COPY --chown=185 target/quarkus-app/quarkus/ /deployments/quarkus/
EXPOSE 8080
USER 185
ENV JAVA_OPTS="-XX:+UseZGC -XX:+ZGenerational"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"
```

### 12.2 Resource Requirements

- CPU: 2 cores (minimum)
- Memory: 4GB (minimum)
- Storage: 20GB (minimum)
- Network: 1Gbps

## 13. Future Considerations

- GraphQL API support
- Enhanced analytics capabilities
- Machine learning model integration
- Extended multi-tenant features

---

## Implementation Notes

This architecture specification should be implemented following these guidelines:

1. Start with core tenant isolation
2. Add security layer
3. Implement data access patterns
4. Add observability
5. Optimize performance
6. Extend capabilities

The implementation should strictly follow the GO-Commerce platform's existing patterns and practices while introducing the new capabilities required for the MCP service.

// Copilot: This file may have been generated or refactored by GitHub Copilot.
