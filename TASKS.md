# GO-Commerce MCP – Actionable Task Checklist

**🚨 CRITICAL: Git/GitHub Workflow Reminders**
- **NEVER close GitHub issues manually** - let PR merges auto-close them
- **Always check** `git status` before any `gh issue close` command  
- **Use feature branches**: `feature/MCP-{issue#}-{name}`
- **Link PRs properly**: Include "Closes #issue-number" in PR descriptions
- **Emergency recovery**: If issue closed prematurely, commit changes → create PR → reference closed issue
- **See WARP.md** for complete Git/GitHub workflow documentation

---

Each task is a single, minimal, independently implementable unit of work. Tasks are synced with GitHub issues for proper traceability. Check off items as you complete them.

**GitHub Issue Mapping:** Each major task group corresponds to GitHub issues. Issue numbers are referenced where available.

## 0) Git Sync and Housekeeping
- [x] Add PLAN.md and TASKS.md to Git (branch: chore/add-plan-tasks) → commit: "chore: add PLAN.md and TASKS.md"
- [x] Update README.md references to PLAN.md and TASKS.md (branch: docs/link-plan-tasks) → commit: "docs: link PLAN.md and TASKS.md in README"

## 1) Project Skeleton & Configuration **→ GitHub Issues: #1, #2 ✅ CLOSED**
- [x] Create Quarkus bootstrap class MCPServerApplication (branch: feat/bootstrap-app) → commit: "feat: add Quarkus bootstrap application"
- [x] Add application.properties base config (http port, app name) (branch: feat/base-config) → commit: "feat: add base application properties"
- [x] Configure Hibernate multitenancy SCHEMA mode (branch: feat/mt-config) → commit: "feat: enable SCHEMA-based multitenancy"
- [x] Add dev profile settings (live reload, swagger-ui) (branch: feat/dev-profile) → commit: "feat: add dev profile configuration"
- [x] Add Micrometer Prometheus metrics config (branch: feat/metrics-config) → commit: "feat: enable Prometheus metrics"

## 2) Tenant Context & Resolver **→ GitHub Issue: #3 🔄 OPEN**
- [x] Implement request-scoped TenantContext holder (branch: feat/tenant-context) → commit: "feat: add TenantContext holder"
- [x] Implement UnifiedTenantResolver (resolve storeId → schema) (branch: feat/tenant-resolver) → commit: "feat: implement UnifiedTenantResolver"
- [x] Map tenantId to schema naming convention (store_<slug>) (branch: feat/tenant-schema-map) → commit: "feat: map tenantId to schema"
- [x] Add unit/integration tests for tenant resolution (branch: test/tenant-resolution) → commit: "test: add tenant resolution tests"

## 3) Security & RBAC **→ GitHub Issue: #11 🔄 OPEN**
- [ ] Configure Keycloak OIDC resource server (branch: feat/keycloak-config) → commit: "feat: configure Keycloak OIDC"
- [ ] Implement JWT validation filter/interceptor (branch: feat/jwt-filter) → commit: "feat: add JWT validation"
- [ ] Define roles (mcp-client, store-admin, store-analyst) in configuration/docs (branch: docs/roles) → commit: "docs: define service roles"
- [ ] Apply @RolesAllowed to protected resources (branch: feat/rbac-annotations) → commit: "feat: apply role-based access control"
- [ ] Implement audit logging service (branch: feat/audit-log-service) → commit: "feat: add audit logging service"
- [ ] Add negative-path security tests (unauth/forbidden) (branch: test/security-negative) → commit: "test: add security negative tests"

## 4) Database & Migrations **→ GitHub Issue: #12 🔄 OPEN**
- [ ] Create control schema mcp (Flyway/Liquibase init) (branch: feat/db-migrations-init) → commit: "feat: init DB migrations with mcp schema"
- [ ] Create table mcp.mcp_contexts (branch: feat/db-mcp-contexts) → commit: "feat: add mcp_contexts table"
- [ ] Create table mcp.audit_logs (monthly partition optional) (branch: feat/db-audit-logs) → commit: "feat: add audit_logs table"
- [ ] Add recommended indexes (sku, order_number, email, etc.) (branch: feat/db-indexes) → commit: "feat: add domain indexes"
- [ ] Document optional PostgreSQL RLS/session vars approach (branch: docs/db-rls) → commit: "docs: outline optional RLS strategy"

## 5) Core Domain DTOs & Query Models
- [ ] Implement ProductDto record (branch: feat/dto-product) → commit: "feat: add ProductDto"
- [ ] Implement OrderDto record (branch: feat/dto-order) → commit: "feat: add OrderDto"
- [ ] Implement CustomerDto record (branch: feat/dto-customer) → commit: "feat: add CustomerDto"
- [ ] Implement QueryCriteria (filters, pagination, sort) (branch: feat/query-criteria) → commit: "feat: add QueryCriteria"
- [ ] Implement DataQuery and DataResponse records (branch: feat/data-query-response) → commit: "feat: add DataQuery/DataResponse"

## 6) Repositories & Data Context Layer
- [ ] Implement ProductRepository (Panache) with criteria support (branch: feat/repo-products) → commit: "feat: add ProductRepository"
- [ ] Implement OrderRepository (Panache) with criteria support (branch: feat/repo-orders) → commit: "feat: add OrderRepository"
- [ ] Implement CustomerRepository (Panache) with criteria support (branch: feat/repo-customers) → commit: "feat: add CustomerRepository"
- [ ] Implement DataContextService routing by domain (branch: feat/data-context-service) → commit: "feat: add DataContextService"
- [ ] Add repository integration tests with tenant switching (branch: test/repo-tenant) → commit: "test: repository tenant isolation tests"

## 7) Context Lifecycle API **→ GitHub Issue: #13 🔄 OPEN**
- [ ] Define ContextRequest/Response records (branch: feat/context-models) → commit: "feat: add ContextRequest/Response"
- [ ] Implement ContextService (create/get/refresh/delete) (branch: feat/context-service) → commit: "feat: implement ContextService"
- [ ] Implement REST: POST /api/v1/contexts (branch: feat/api-contexts-post) → commit: "feat(api): POST /contexts"
- [ ] Implement REST: GET /api/v1/contexts/{id} (branch: feat/api-contexts-get) → commit: "feat(api): GET /contexts/{id}"
- [ ] Implement REST: PUT /api/v1/contexts/{id}/refresh (branch: feat/api-contexts-refresh) → commit: "feat(api): PUT /contexts/{id}/refresh"
- [ ] Implement REST: DELETE /api/v1/contexts/{id} (branch: feat/api-contexts-delete) → commit: "feat(api): DELETE /contexts/{id}"
- [ ] Add context API integration tests (branch: test/api-contexts) → commit: "test(api): context endpoints"

## 8) Data Access APIs **→ GitHub Issues: #4, #5, #6 🔄 OPEN**
- [ ] Implement REST: GET /api/v1/data/products with filters (branch: feat/api-products) → commit: "feat(api): GET /data/products"
- [ ] Implement REST: GET /api/v1/data/orders with filters (branch: feat/api-orders) → commit: "feat(api): GET /data/orders"
- [ ] Implement REST: GET /api/v1/data/customers with filters (branch: feat/api-customers) → commit: "feat(api): GET /data/customers"
- [ ] Implement REST: POST /api/v1/data/search (branch: feat/api-search) → commit: "feat(api): POST /data/search"
- [ ] Add API contract tests (pagination, sorting, filtering) (branch: test/api-contract) → commit: "test(api): contract tests"

## 9) Cart (MVP Phase 2) **→ GitHub Issues: #7, #8, #9, #10 🔄 OPEN**
- [ ] Implement in-memory/Redis cart model (branch: feat/cart-model) → commit: "feat: add cart model"
- [ ] Implement REST: POST /api/v1/cart/items (branch: feat/api-cart-add) → commit: "feat(api): POST /cart/items"
- [ ] Implement REST: GET /api/v1/cart (branch: feat/api-cart-get) → commit: "feat(api): GET /cart"
- [ ] Implement REST: DELETE /api/v1/cart/items/{itemId} (branch: feat/api-cart-delete) → commit: "feat(api): DELETE /cart/items/{itemId}"
- [ ] Add cart API integration tests (branch: test/api-cart) → commit: "test(api): cart endpoints"

## 10) Caching & Performance
- [ ] Integrate Redis client and connection config (branch: feat/redis-config) → commit: "feat: configure Redis client"
- [ ] Implement CacheManager abstraction (branch: feat/cache-manager) → commit: "feat: add CacheManager"
- [ ] Add L2 cache for product listings (branch: feat/cache-products) → commit: "feat: cache product listings"
- [ ] Add cache invalidation hooks via events (branch: feat/cache-invalidate) → commit: "feat: event-driven cache invalidation"
- [ ] Enable virtual threads where blocking IO occurs (branch: feat/virtual-threads) → commit: "feat: enable virtual threads usage"

## 11) Events & Streaming
- [ ] Configure Kafka topics in application properties (branch: feat/kafka-config) → commit: "feat: configure Kafka topics"
- [ ] Implement consumer for data-updates (branch: feat/kafka-consumer) → commit: "feat: add data-updates consumer"
- [ ] Implement producer for context-events (branch: feat/kafka-producer) → commit: "feat: add context-events producer"
- [ ] Add idempotency/retry + DLQ handling (branch: feat/kafka-reliability) → commit: "feat: improve Kafka reliability"
- [ ] Add integration tests for event flow (branch: test/events) → commit: "test: event flow integration"

## 12) WebSocket Events
- [ ] Implement WebSocket /ws/v1/events/{contextId} (branch: feat/ws-endpoint) → commit: "feat(ws): events endpoint"
- [ ] Wire WebSocket with Kafka to broadcast updates (branch: feat/ws-broadcast) → commit: "feat(ws): broadcast Kafka updates"
- [ ] Add WebSocket tests (happy path + disconnect) (branch: test/ws) → commit: "test(ws): endpoint tests"

## 13) Observability & Operations
- [ ] Implement liveness/readiness checks (branch: feat/health) → commit: "feat: add liveness/readiness checks"
- [ ] Add OpenTelemetry tracing config (branch: feat/otel-config) → commit: "feat: configure OpenTelemetry"
- [ ] Add structured JSON logging with correlationId (branch: feat/json-logging) → commit: "feat: structured logging with correlationId"
- [ ] Expose Prometheus metrics and verify (branch: feat/metrics-verify) → commit: "feat: verify Prometheus metrics"

## 14) OpenAPI & Docs
- [ ] Generate OpenAPI for all endpoints (branch: docs/openapi-gen) → commit: "docs(api): generate OpenAPI spec"
- [ ] Add endpoint examples to README/wiki (branch: docs/api-examples) → commit: "docs: add API examples"
- [ ] Document multi-tenancy and security in wiki (branch: docs/mt-security) → commit: "docs: document MT & security"

## 15) Packaging & Deployment
- [ ] Add multi-stage Dockerfile (branch: chore/dockerfile) → commit: "chore: add multi-stage Dockerfile"
- [ ] Add GitHub Actions CI (build, test) (branch: chore/ci) → commit: "chore(ci): add build and test workflow"
- [ ] Provide PgBouncer deployment notes/config (branch: docs/pgbouncer) → commit: "docs: PgBouncer guidance"
- [ ] Provide environment profile examples (dev/stage/prod) (branch: docs/env-profiles) → commit: "docs: env profiles"

## 16) Testing Strategy Execution
- [ ] Add tenant isolation integration tests suite (branch: test/tenant-isolation) → commit: "test: tenant isolation suite"
- [ ] Add security regression tests suite (branch: test/security-suite) → commit: "test: security regression suite"
- [ ] Add performance smoke tests for listings (branch: test/perf-smoke) → commit: "test: performance smoke tests"

## 17) Finalization Checklist
- [ ] Verify deliverables checklist from PLAN.md (branch: chore/verify-deliverables) → commit: "chore: verify deliverables checklist"
- [ ] Run full test matrix and fix regressions (branch: chore/test-matrix) → commit: "chore: run full test matrix"
- [ ] Prepare release notes and CHANGELOG (branch: docs/changelog) → commit: "docs: add CHANGELOG for initial release"


// Copilot: This file may have been generated or refactored by GitHub Copilot.

