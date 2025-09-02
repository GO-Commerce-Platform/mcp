# GO-Commerce MCP Server Database Architecture & Multi-tenancy Model

## Overview

The database architecture for the GO-Commerce MCP service implements a schema-per-tenant multi-tenancy model with PostgreSQL. This document outlines the database structure, tenant isolation mechanisms, migration strategies, and operational procedures for maintaining a secure and scalable multi-tenant environment.

## 1. Schema Design

### 1.1 Tenant Schema Structure

Each tenant's data is isolated in its own PostgreSQL schema following the naming convention:

```sql path=null start=null
-- Base structure for tenant schemas
CREATE SCHEMA "tenant_{{tenantId}}";

-- Core tables in each tenant schema
CREATE TABLE "tenant_{{tenantId}}".customers (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- tenant-specific fields
);

CREATE TABLE "tenant_{{tenantId}}".products (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- tenant-specific fields
);

-- Additional domain tables as needed
```

### 1.2 Shared Schema

Common, non-tenant-specific data resides in the public schema:

```sql path=null start=null
-- Public schema for shared resources
CREATE SCHEMA IF NOT EXISTS public;

-- Tenant registry
CREATE TABLE public.tenants (
    id UUID PRIMARY KEY,
    schema_name VARCHAR(63) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status tenant_status NOT NULL DEFAULT 'ACTIVE',
    config JSONB NOT NULL DEFAULT '{}'::jsonb
);

-- Tenant audit log
CREATE TABLE public.tenant_audit_log (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    action_type VARCHAR(50) NOT NULL,
    action_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details JSONB NOT NULL
);
```

## 2. Tenant Provisioning

### 2.1 Provisioning Process

```java path=null start=null
@ApplicationScoped
public class TenantProvisioningService {
    @Inject
    EntityManager em;
    
    @Inject
    FlywayMigrationService flyway;
    
    @Transactional
    public Tenant provisionNewTenant(TenantProvisionRequest request) {
        // 1. Create tenant record
        var tenant = Tenant.builder()
            .id(UUID.randomUUID())
            .schemaName("tenant_" + request.getTenantId())
            .status(TenantStatus.INITIALIZING)
            .build();
            
        em.persist(tenant);
        
        // 2. Create schema and base structure
        createTenantSchema(tenant);
        
        // 3. Run migrations
        flyway.migrateSchema(tenant.getSchemaName());
        
        // 4. Initialize tenant data
        initializeTenantData(tenant, request);
        
        // 5. Activate tenant
        tenant.setStatus(TenantStatus.ACTIVE);
        em.merge(tenant);
        
        return tenant;
    }
}
```

### 2.2 Tenant Lifecycle Management

```java path=null start=null
@ApplicationScoped
public class TenantLifecycleManager {
    @Inject
    TenantRepository tenantRepo;
    
    @Inject
    TenantBackupService backup;
    
    @Transactional
    public void deactivateTenant(UUID tenantId) {
        var tenant = tenantRepo.findById(tenantId);
        
        // 1. Create final backup
        backup.createBackup(tenant);
        
        // 2. Disable new connections
        tenant.setStatus(TenantStatus.DEACTIVATING);
        
        // 3. Wait for active connections to complete
        waitForConnectionDrain(tenant);
        
        // 4. Archive data
        archiveTenantData(tenant);
        
        // 5. Update status
        tenant.setStatus(TenantStatus.INACTIVE);
    }
}
```

## 3. Connection Pooling with PgBouncer

### 3.1 PgBouncer Configuration

```ini path=null start=null
[databases]
* = host=127.0.0.1 port=5432 dbname=gocommerce

[pgbouncer]
listen_port = 6432
listen_addr = *
auth_type = md5
auth_file = /etc/pgbouncer/userlist.txt
pool_mode = transaction
max_client_conn = 1000
default_pool_size = 20
min_pool_size = 10
reserve_pool_size = 5
reserve_pool_timeout = 5
max_db_connections = 50
max_user_connections = 50
```

### 3.2 Pool Management

```java path=null start=null
@ApplicationScoped
public class ConnectionPoolManager {
    @Inject
    PgBouncerClient pgbouncer;
    
    public void adjustPoolSize(String tenantId, PoolConfig config) {
        // Dynamic pool size adjustment based on tenant load
        pgbouncer.setPoolSize(
            tenantId,
            config.getMinSize(),
            config.getMaxSize()
        );
        
        // Update metrics
        updatePoolMetrics(tenantId, config);
    }
}
```

## 4. Schema Migration Strategy

### 4.1 Flyway Configuration

```properties path=null start=null
# Flyway base configuration
quarkus.flyway.migrate-at-start=false
quarkus.flyway.baseline-on-migrate=true
quarkus.flyway.baseline-version=1.0.0
quarkus.flyway.table=schema_version

# Tenant-specific configuration is applied dynamically
```

### 4.2 Migration Management

```java path=null start=null
@ApplicationScoped
public class FlywayMigrationService {
    @Inject
    FlywayFactory flyway;
    
    @Inject
    TenantRegistry registry;
    
    public void migrateAllTenants() {
        registry.getActiveTenants().forEach(tenant -> {
            try {
                migrateTenant(tenant);
            } catch (Exception e) {
                handleMigrationFailure(tenant, e);
            }
        });
    }
    
    @Transactional
    public void migrateTenant(Tenant tenant) {
        // Configure Flyway for tenant schema
        Flyway fw = flyway.createForTenant(tenant);
        
        // Execute migration
        var result = fw.migrate();
        
        // Log results
        logMigrationResult(tenant, result);
    }
}
```

## 5. Data Isolation

### 5.1 Row-Level Security

```sql path=null start=null
-- Enable RLS on all tenant tables
ALTER TABLE "tenant_{{tenantId}}".customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE "tenant_{{tenantId}}".products ENABLE ROW LEVEL SECURITY;

-- Create tenant isolation policies
CREATE POLICY tenant_isolation_customers ON "tenant_{{tenantId}}".customers
    USING (current_setting('app.current_tenant_id') = '{{tenantId}}');
    
CREATE POLICY tenant_isolation_products ON "tenant_{{tenantId}}".products
    USING (current_setting('app.current_tenant_id') = '{{tenantId}}');
```

### 5.2 Query Isolation

```java path=null start=null
@ApplicationScoped
public class TenantAwareQueryInterceptor {
    @Inject
    TenantContext context;
    
    @AroundInvoke
    public Object interceptQuery(InvocationContext ic) throws Exception {
        // Set tenant context in thread local
        TenantContextHolder.set(context.getCurrentTenant());
        
        try {
            // Execute the query
            return ic.proceed();
        } finally {
            // Clear tenant context
            TenantContextHolder.clear();
        }
    }
}
```

## 6. Backup and Recovery

### 6.1 Backup Strategy

```java path=null start=null
@ApplicationScoped
public class TenantBackupService {
    @Inject
    BackupClient backupClient;
    
    @Scheduled(cron = "0 0 2 * * ?") // 2 AM daily
    void performBackups() {
        tenantRegistry.getActiveTenants().forEach(tenant -> {
            try {
                // Create backup
                var backup = BackupJob.builder()
                    .tenantId(tenant.getId())
                    .timestamp(Instant.now())
                    .type(BackupType.FULL)
                    .build();
                    
                // Execute backup
                backupClient.executeBackup(backup);
                
                // Log success
                logBackupSuccess(tenant, backup);
            } catch (Exception e) {
                // Handle failure
                handleBackupFailure(tenant, e);
            }
        });
    }
}
```

### 6.2 Recovery Procedures

```java path=null start=null
@ApplicationScoped
public class TenantRecoveryService {
    @Inject
    BackupClient backupClient;
    
    @Transactional
    public void restoreTenant(UUID tenantId, Instant pointInTime) {
        // 1. Locate appropriate backup
        var backup = backupClient.findNearestBackup(tenantId, pointInTime);
        
        // 2. Create recovery schema
        var recoverySchema = createRecoverySchema(tenantId);
        
        // 3. Restore data
        backupClient.restoreToSchema(backup, recoverySchema);
        
        // 4. Validate restored data
        validateRestoredData(recoverySchema);
        
        // 5. Switch to restored schema
        switchToRestoredSchema(tenantId, recoverySchema);
    }
}
```

## 7. Performance Optimization

### 7.1 Index Strategy

```sql path=null start=null
-- Create indexes for common queries
CREATE INDEX idx_customers_email 
    ON "tenant_{{tenantId}}".customers(email);
    
CREATE INDEX idx_products_category 
    ON "tenant_{{tenantId}}".products(category_id);
    
-- Create partial indexes for active records
CREATE INDEX idx_active_customers 
    ON "tenant_{{tenantId}}".customers(id) 
    WHERE status = 'ACTIVE';
```

### 7.2 Query Optimization

```java path=null start=null
@ApplicationScoped
public class QueryOptimizationService {
    @Inject
    EntityManager em;
    
    public <T> List<T> optimizedQuery(QuerySpec spec) {
        // Create query with hints
        var query = em.createQuery(spec.getQuery())
            .setHint(QueryHints.FETCH_SIZE, 100)
            .setHint(QueryHints.READ_ONLY, true);
            
        // Apply tenant context
        query.setParameter("tenantId", getTenantId());
        
        // Execute with pagination
        return query.setFirstResult(spec.getOffset())
            .setMaxResults(spec.getLimit())
            .getResultList();
    }
}
```

## 8. Monitoring and Maintenance

### 8.1 Schema Statistics

```java path=null start=null
@ApplicationScoped
public class SchemaStatisticsCollector {
    @Scheduled(every = "1h")
    void collectStatistics() {
        tenantRegistry.getActiveTenants().forEach(tenant -> {
            var stats = SchemaStatistics.builder()
                .tenantId(tenant.getId())
                .tableCount(countTables(tenant))
                .totalSize(calculateSize(tenant))
                .rowCounts(getTableRowCounts(tenant))
                .build();
                
            metricsService.recordSchemaStats(stats);
        });
    }
}
```

### 8.2 Health Checks

```java path=null start=null
@ApplicationScoped
public class DatabaseHealthCheck {
    @Inject
    EntityManager em;
    
    @Scheduled(every = "1m")
    void checkDatabaseHealth() {
        try {
            // Check connection
            em.createNativeQuery("SELECT 1").getSingleResult();
            
            // Check replication lag
            checkReplicationLag();
            
            // Check connection pools
            checkConnectionPools();
            
            // Record metrics
            recordHealthMetrics();
        } catch (Exception e) {
            handleHealthCheckFailure(e);
        }
    }
}
```

## 9. Implementation Guidelines

### 9.1 Schema Creation Pattern

```java path=null start=null
public record SchemaCreationSpec(
    String schemaName,
    List<String> extensions,
    List<String> tables,
    List<String> indices
) {
    public static SchemaCreationSpec forTenant(String tenantId) {
        return new SchemaCreationSpec(
            "tenant_" + tenantId,
            List.of("uuid-ossp", "pg_stat_statements"),
            getDefaultTables(),
            getDefaultIndices()
        );
    }
}
```

### 9.2 Migration Patterns

```java path=null start=null
@Path("/V{version}__{description}")
public class TenantAwareMigration {
    @Inject
    TenantContext tenantContext;
    
    public void migrate(MigrationContext context) {
        var tenantId = context.getTenantId();
        tenantContext.setCurrentTenant(tenantId);
        
        try {
            // Execute migration logic
            executeMigration(context);
        } finally {
            tenantContext.clear();
        }
    }
}
```

## 10. Operational Procedures

### 10.1 Tenant Onboarding

1. Validate tenant information
2. Create tenant record in registry
3. Provision tenant schema
4. Execute baseline migrations
5. Initialize tenant data
6. Verify tenant access
7. Enable monitoring

### 10.2 Tenant Offboarding

1. Backup tenant data
2. Archive tenant schema
3. Update tenant status
4. Clean up resources
5. Archive audit logs
6. Generate offboarding report

### 10.3 Maintenance Windows

1. Schedule during low-usage periods
2. Notify affected tenants
3. Execute maintenance tasks
4. Verify system health
5. Resume normal operations

## Appendix A: Database Configuration

```properties path=null start=null
# PostgreSQL Configuration
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/gocommerce
quarkus.datasource.username=${DB_USER}
quarkus.datasource.password=${DB_PASSWORD}

# Connection Pool Configuration
quarkus.datasource.jdbc.min-size=10
quarkus.datasource.jdbc.max-size=20
quarkus.datasource.jdbc.initial-size=10

# PgBouncer Configuration
quarkus.datasource.jdbc.additional-jdbc-properties.targetServerType=master
quarkus.datasource.jdbc.additional-jdbc-properties.loadBalanceHosts=true
```

## Appendix B: Backup Configuration

```yaml path=null start=null
backup:
  schedule:
    full: "0 0 2 * * ?"  # Daily at 2 AM
    incremental: "0 0 */4 * * ?"  # Every 4 hours
  retention:
    full: 30d  # Keep full backups for 30 days
    incremental: 7d  # Keep incremental backups for 7 days
  compression: true
  encryption: true
  validation: true
```

// Copilot: This file may have been generated or refactored by GitHub Copilot.
