# GO-Commerce MCP Service – Technical Implementation Plan

This plan translates the high-level specification in WARP.md into a concrete, implementation-ready blueprint. It aligns with Quarkus 3.x + Java 21, PostgreSQL (schema-per-tenant), Kafka, Redis, and Keycloak.

---

## 1) Executive Summary

The MCP service is a tenant-aware bridge for AI agents to securely access GO-Commerce data. It enforces strict tenant isolation, surfaces RESTful APIs for product/customer/order data, streams updates via Kafka/WebSockets, and integrates with Keycloak for JWT-based RBAC.

Key priorities:
- Schema-per-tenant isolation with a hardened TenantResolver
- Defense-in-depth security: JWT + RBAC + schema isolation (+ optional RLS)
- Unified Data Context Layer to abstract domain access patterns
- Observability-by-default: health, metrics, tracing, structured logs

---

## 2) System Architecture Overview

Layers:
- Edge/API
  - REST (JAX-RS) under /api/v1
  - WebSocket endpoints for eventing
  - OpenAPI docs enabled (dev profile)
- Security
  - Keycloak bearer token validation
  - Role-based authorization annotations
  - Tenant resolution (from JWT claims)
- Business/Core
  - MCP Context lifecycle (create, get, refresh, delete)
  - Data Context Layer routing to domain repositories
  - Validation and error handling
- Data Access
  - Panache repositories per domain (products, orders, customers)
  - QueryCriteria builders and pagination/sorting
  - Redis L2 cache for hot datasets
- Integration/Eventing
  - Kafka consumers (data-updates) and producers (context-events)
- Observability/Runtime
  - Micrometer Prometheus metrics, Quarkus health checks
  - OpenTelemetry tracing, correlation IDs in logs

---

## 3) Core Components and Services

- MCPServerApplication: Quarkus bootstrap
- SecurityConfiguration: JWT verification, Keycloak adapter
- TenantResolver (UnifiedTenantResolver): derives tenantId/schema from JWT
- StoreContext/TenantContext: request-scoped holder for tenant + roles
- ContextManager/ContextService: manages MCP contexts (TTL, status)
- DataContextService: routes domain queries (products/orders/customers)
- Domain Repositories: ProductRepository, OrderRepository, CustomerRepository
- EventProcessor: Kafka consumers/producers
- CacheManager/RedisCache: Redis-backed L2 cache
- ErrorHandler/ValidationInterceptor: consistent error/validation responses

---

## 4) Data Models and Entities

4.1 MCP Core
- MCPContext
  - id: UUID (contextId)
  - tenantId: String
  - status: enum { ACTIVE, EXPIRED, REVOKED }
  - createdAt: Instant
  - expiresAt: Instant
  - metadata: JSON (optional)
- AuditLog
  - id: UUID
  - contextId: UUID
  - tenantId: String
  - action: String
  - userId: String
  - timestamp: Instant
  - details: JSON

4.2 DTOs (Java records)
- ProductDto(id: UUID, name: String, sku: String, price: BigDecimal, stock: Integer, currency: String, category: String)
- OrderDto(id: UUID, orderNumber: String, status: String, total: BigDecimal, currency: String, createdAt: Instant)
- CustomerDto(id: UUID, email: String, name: String, type: String, createdAt: Instant)

4.3 Query & Pagination
- QueryCriteria(filters: Map<String,Object>, pagination: {page,size}, sort: {field,order})
- DataQuery(domain: enum, criteria: QueryCriteria)
- DataResponse(items: List<Any>, total: long, page: int, size: int)

4.4 Context API Models
- ContextRequest(scope: [products|orders|customers], ttlSeconds: int=3600, metadata: Map)
- ContextResponse(id: UUID, tenantId: String, status: String, createdAt: Instant, expiresAt: Instant)

Note: All DTOs are immutable Java records; entities use Panache. Keep request/response models as records to reduce boilerplate.

---

## 5) Multi-Tenancy and Security

5.1 Tenant Resolution
- Resolve tenantId from JWT claim storeId
- Map tenantId -> database schema (e.g., store_<slug>)
- Configure quarkus.hibernate-orm.multitenant=SCHEMA
- Optionally set PostgreSQL session variable for RLS compatibility

5.2 Authentication & Authorization
- JWT bearer via Keycloak
- Roles: mcp-client, store-admin, store-analyst, etc.
- Endpoint-level @RolesAllowed annotations

5.3 Defense-in-Depth
- Application: JWT + RBAC + TenantResolver
- Database: schema isolation; optional RLS with session variables
- Network: TLS, gateway rate limiting, WAF/DDOS where applicable

---

## 6) Performance & Concurrency
- Java 21 virtual threads for blocking DB/IO paths
- Reactive streams for Kafka/eventing
- PgBouncer recommended in front of Postgres
- Redis cache for read-heavy endpoints (products/catalog hot paths)
- Consistent pagination and selective projections to limit payloads

---

## 7) Observability & Operations
- Health: /q/health, /q/health/ready, /q/health/live
- Metrics: /q/metrics (Micrometer + Prometheus)
- Tracing: OpenTelemetry with context propagation
- Logging: JSON structured logs with correlationId and tenantId

---

## 8) API Specifications (REST)

Base path: /api/v1
Security: Bearer JWT (Keycloak), roles as specified per resource
Content-Type: application/json

8.1 Context Management
- POST /contexts
  - Auth: mcp-client
  - Body: ContextRequest
  - 201: ContextResponse
- GET /contexts/{contextId}
  - Auth: mcp-client
  - 200: ContextResponse
- PUT /contexts/{contextId}/refresh
  - Auth: mcp-client
  - 200: ContextResponse
- DELETE /contexts/{contextId}
  - Auth: mcp-client
  - 204

8.2 Data Access (Domain-specific)
- GET /data/products
  - Auth: mcp-client or store-analyst
  - Query params: search, sku, category, priceMin, priceMax, page, size, sort
  - 200: DataResponse<ProductDto>
- GET /data/orders
  - Auth: mcp-client or store-analyst
  - Query params: orderNumber, status, dateFrom, dateTo, page, size, sort
  - 200: DataResponse<OrderDto>
- GET /data/customers
  - Auth: mcp-client or store-analyst
  - Query params: email, name, type, page, size, sort
  - 200: DataResponse<CustomerDto>
- POST /data/search
  - Auth: mcp-client
  - Body: DataQuery (domain + criteria)
  - 200: DataResponse<Record>

8.3 Cart (MVP-Phase 2)
- POST /cart/items
  - Auth: mcp-client
  - Body: { productId: UUID, quantity: int }
  - 201: { itemId: UUID }
- GET /cart
  - Auth: mcp-client
  - 200: { items: [{itemId, productId, name, price, qty, subtotal}], total }
- DELETE /cart/items/{itemId}
  - Auth: mcp-client
  - 204

8.4 Events & Streaming
- WebSocket /ws/v1/events/{contextId}
  - Auth: mcp-client
  - Server-to-client messages: { type, payload, ts }
- POST /events/publish
  - Auth: store-admin
  - Body: { topic: String, key?: String, payload: JSON }
  - 202

8.5 Health & Ops
- GET /q/health (Quarkus)
- GET /q/metrics (Quarkus)

8.6 Error Model
- Problem Details (RFC 7807 style): { type, title, status, detail, traceId }

---

## 9) JSON Schemas (Illustrative)

ContextRequest
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "required": ["scope"],
  "properties": {
    "scope": { "type": "array", "items": {"type": "string", "enum": ["products", "orders", "customers"]}},
    "ttlSeconds": { "type": "integer", "minimum": 60, "default": 3600 },
    "metadata": { "type": "object", "additionalProperties": true }
  }
}

DataQuery
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "required": ["domain", "criteria"],
  "properties": {
    "domain": {"type": "string", "enum": ["products", "orders", "customers"]},
    "criteria": {
      "type": "object",
      "properties": {
        "filters": {"type": "object", "additionalProperties": true},
        "pagination": {
          "type": "object",
          "properties": {"page": {"type": "integer", "minimum": 0}, "size": {"type": "integer", "minimum": 1, "maximum": 200}},
          "required": ["page", "size"]
        },
        "sort": {"type": "object", "properties": {"field": {"type": "string"}, "order": {"type": "string", "enum": ["ASC", "DESC"]}}}
      },
      "required": ["pagination"]
    }
  }
}

DataResponse
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "required": ["items", "total", "page", "size"],
  "properties": {
    "items": {"type": "array", "items": {"type": "object"}},
    "total": {"type": "integer", "minimum": 0},
    "page": {"type": "integer", "minimum": 0},
    "size": {"type": "integer", "minimum": 1}
  }
}

---

## 10) Implementation Phases

Phase 1: Core Infrastructure & MT Setup
- Quarkus skeleton, POM, Dockerfile, dev services
- TenantResolver + StoreContext
- Health/metrics, logging, tracing
- Product query endpoint (GET /data/products) with pagination
- Integration tests for tenant isolation

Phase 2: Security & RBAC
- Keycloak integration, JWT filter
- @RolesAllowed across endpoints
- Audit logging for data access
- Negative security tests (unauthorized/forbidden)

Phase 3: Data Layer & Caching
- Panache repositories, QueryCriteria builder
- Redis L2 cache, cache invalidation hooks (Kafka events)
- Orders/customers endpoints
- DB migrations and indexes (SKU, orderNumber, email)

Phase 4: MCP Protocol & Events
- Context lifecycle endpoints
- WebSocket events + Kafka bridging
- POST /data/search with flexible criteria
- OpenAPI spec and examples

Phase 5 (optional): Analytics Bridge
- Aggregation service or warehouse integration (pgvector/OLAP)
- /analytics endpoints guarded by privileged roles

---

## 11) Database Design & Migrations

- Shared DB with per-tenant schemas: store_<slug>
- MCP metadata tables in a control schema (e.g., mcp):
  - mcp.mcp_contexts
  - mcp.audit_logs (partition by month)
- Recommended indexes:
  - products(sku), products(category, price)
  - orders(order_number), orders(created_at), orders(status)
  - customers(email), customers(created_at)

---

## 12) Kafka Topics
- data-updates: domain update events (keyed by tenantId)
- context-events: lifecycle events for MCP contexts
- cache-invalidate: signals for Redis cache invalidation

Consumer groups per service instance; idempotent processors with retry + DLQ if needed.

---

## 13) Configuration (application.properties excerpt)

quarkus.application.name=mcp-server
quarkus.http.port=8080

# Database
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/gocommerce
quarkus.datasource.username=${DB_USER}
quarkus.datasource.password=${DB_PASSWORD}
quarkus.hibernate-orm.multitenant=SCHEMA
quarkus.hibernate-orm.database.generation=none

# Metrics
quarkus.micrometer.export.prometheus.enabled=true
quarkus.micrometer.export.prometheus.path=/metrics

# Dev
%dev.quarkus.live-reload.enabled=true
%dev.quarkus.swagger-ui.enable=true

# Redis
quarkus.redis.hosts=redis://localhost:6379

# Kafka (example)
mp.messaging.incoming.data-updates.connector=smallrye-kafka
mp.messaging.outgoing.context-events.connector=smallrye-kafka

---

## 14) Testing Strategy
- Integration-first with @QuarkusTest
- Tenant isolation tests per JWT (storeId claim)
- Security tests (unauth, wrong roles, cross-tenant attempts)
- Contract tests for REST schemas
- Performance smoke tests (pagination, large result sets)

---

## 15) Deployment & Ops
- Containerized (multi-stage Dockerfile)
- Run behind API gateway/ingress with TLS
- PgBouncer in front of Postgres
- Environment-specific configs via profiles
- Dashboards (Grafana), alerts (Prometheus rules)

---

## 16) Risks & Mitigations
- Risk: Tenant leakage via incorrect schema selection
  - Mitigation: Mandatory TenantResolver + tests; optional RLS
- Risk: Connection exhaustion
  - Mitigation: PgBouncer + pool limits; backpressure on reactive paths
- Risk: Cache staleness
  - Mitigation: Event-driven invalidation; short TTLs for critical keys

---

## 17) Deliverables Checklist
- [ ] Context endpoints implemented and documented
- [ ] Products/Orders/Customers endpoints with pagination and filtering
- [ ] TenantResolver wired with Keycloak JWTs
- [ ] Kafka and Redis integrations
- [ ] OpenAPI published; examples verified
- [ ] Health/metrics/tracing/logging in place
- [ ] Integration/security tests passing


// Copilot: This file may have been generated or refactored by GitHub Copilot.

