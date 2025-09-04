# ADR 0001: Multi-tenant Pattern Selection

## Status

Accepted

## Context

The GO-Commerce MCP service requires a multi-tenant architecture to serve multiple merchants (tenants) while ensuring complete data isolation, optimal resource utilization, and scalability. We need to choose between several common multi-tenancy patterns:

1. **Separate Databases**
   - Each tenant gets their own database
   - Complete isolation
   - Higher resource usage
   - More complex maintenance

2. **Shared Database, Separate Schemas**
   - Single database with schema per tenant
   - Good isolation
   - Efficient resource usage
   - Simpler maintenance

3. **Shared Schema**
   - Single database and schema
   - Tenant ID in each table
   - Most efficient resource usage
   - Highest risk of data leaks

## Decision

We have chosen to implement the **Shared Database, Separate Schemas** pattern for the following reasons:

1. **Data Isolation**
   - PostgreSQL schema-level isolation provides strong security boundaries
   - Additional Row-Level Security (RLS) as a second defense layer
   - Tenant context cannot accidentally leak across schemas

2. **Resource Optimization**
   - Single database instance reduces operational overhead
   - Connection pooling can be shared across tenants
   - Efficient resource utilization compared to separate databases

3. **Operational Simplicity**
   - Centralized backup and restore procedures
   - Simplified database administration
   - Easier monitoring and maintenance

4. **Scalability**
   - Horizontal scaling possible through database sharding
   - Schema operations can be parallelized
   - Independent tenant resource quotas

Implementation approach:
```sql
-- Create schema for new tenant
CREATE SCHEMA "tenant_{{tenantId}}";

-- Enable RLS on tenant tables
ALTER TABLE "tenant_{{tenantId}}".customers ENABLE ROW LEVEL SECURITY;

-- Create policy for tenant isolation
CREATE POLICY tenant_isolation ON "tenant_{{tenantId}}".customers
    USING (current_setting('app.current_tenant_id') = '{{tenantId}}');
```

## Consequences

### Positive

1. **Security**
   - Strong tenant isolation through PostgreSQL schemas
   - Additional security through RLS policies
   - Clear security boundaries

2. **Performance**
   - Efficient resource sharing
   - Optimized connection pooling
   - Better cache utilization

3. **Maintenance**
   - Centralized database management
   - Simplified backup procedures
   - Easier monitoring

4. **Cost**
   - Lower infrastructure costs
   - Efficient resource utilization
   - Reduced operational overhead

### Negative

1. **Complexity**
   - Need for tenant context management
   - More complex application logic
   - Schema migration coordination

2. **Operations**
   - Schema proliferation management
   - Need for schema quota management
   - More complex tenant cleanup

3. **Scalability**
   - Schema count limits per database
   - Need for eventual sharding strategy
   - Cross-tenant operation complexity

## Compliance

This decision impacts several compliance areas:

1. **Data Privacy**
   - Ensures tenant data isolation
   - Supports data residency requirements
   - Enables tenant-specific encryption

2. **Security**
   - Meets data isolation requirements
   - Supports audit requirements
   - Enables tenant-specific security policies

3. **Regulatory**
   - Supports GDPR requirements
   - Enables tenant data portability
   - Facilitates right to be forgotten

## Implementation

### 1. Tenant Context Management

```java
@ApplicationScoped
public class TenantContext {
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();
    
    public void setCurrentTenant(String tenantId) {
        currentTenant.set(tenantId);
    }
    
    public String getCurrentTenant() {
        return currentTenant.get();
    }
    
    public void clear() {
        currentTenant.remove();
    }
}
```

### 2. Schema Management

```java
@ApplicationScoped
public class SchemaManager {
    @Inject
    EntityManager em;
    
    @Transactional
    public void createTenantSchema(String tenantId) {
        // Create schema
        em.createNativeQuery("CREATE SCHEMA tenant_" + tenantId)
            .executeUpdate();
        
        // Apply migrations
        flywayMigrator.migrate(tenantId);
        
        // Configure RLS
        configureSchemaSecurity(tenantId);
    }
    
    private void configureSchemaSecurity(String tenantId) {
        // Enable RLS and create policies
        for (String table : getTenantTables()) {
            enableRLSForTable(tenantId, table);
        }
    }
}
```

### 3. Tenant Resolver

```java
@ApplicationScoped
public class TenantResolver implements QuarkusTenantResolver {
    @Inject
    TenantContext tenantContext;
    
    @Override
    public String resolveTenantId() {
        String tenantId = tenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new TenantNotResolvedException();
        }
        return tenantId;
    }
}
```

## Alternative Approaches Considered

### 1. Separate Databases Approach

**Pros:**
- Complete isolation
- Independent scaling
- Simpler tenant data management

**Cons:**
- Higher costs
- Complex operational management
- Resource inefficiency

### 2. Shared Schema Approach

**Pros:**
- Simplest implementation
- Most efficient resource usage
- Easiest maintenance

**Cons:**
- Highest risk of data leaks
- Complex queries
- Difficult to customize per tenant

## References

1. PostgreSQL Schema Documentation
   - https://www.postgresql.org/docs/current/ddl-schemas.html

2. Row Level Security Documentation
   - https://www.postgresql.org/docs/current/ddl-rowsecurity.html

3. Multi-tenant Data Architecture
   - NIST Database Security Guidelines
   - Cloud Security Alliance Multi-tenant Architecture Guidelines

## Related Decisions

- ADR 0002: Technology Stack Selection
- ADR 0003: Security Architecture
- ADR 0004: Data Partitioning Strategy

// Copilot: This file may have been generated or refactored by GitHub Copilot.
