package dev.tiodati.saas.gocommerce.mcp.config;

import dev.tiodati.saas.gocommerce.mcp.tenant.UnifiedTenantResolver;
import io.quarkus.arc.Unremovable;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * Configuration class for Hibernate multi-tenancy setup.
 * 
 * This configuration class provides the necessary beans and setup for
 * Hibernate's schema-based multi-tenancy functionality.
 * 
 * @author GO-Commerce Development Team
 * @version 1.0.0
 * @since 1.0.0
 */
@ApplicationScoped
public class MultiTenancyConfiguration {

    public MultiTenancyConfiguration() {
        Log.info("Multi-tenancy configuration initialized");
    }

    @Produces
    @ApplicationScoped
    @Unremovable
    public org.hibernate.context.spi.CurrentTenantIdentifierResolver tenantIdentifierResolver(
            UnifiedTenantResolver tenantResolver) {
        
        Log.info("Configuring Hibernate CurrentTenantIdentifierResolver");
        Log.debugf("Tenant resolver configuration: %s", tenantResolver.toString());
        
        return tenantResolver;
    }

    @jakarta.annotation.PostConstruct
    public void validateConfiguration() {
        Log.info("Validating multi-tenancy configuration...");
        
        try {
            validateSchemaNaming();
            logConfigurationSummary();
            Log.info("Multi-tenancy configuration validation completed successfully");
            
        } catch (Exception e) {
            Log.errorf(e, "Multi-tenancy configuration validation failed: %s", e.getMessage());
            throw new IllegalStateException("Invalid multi-tenancy configuration", e);
        }
    }

    private void validateSchemaNaming() {
        Log.debug("Validating schema naming conventions...");
        
        String[] testTenantIds = {
            "acme-corp",
            "Test Store 123",
            "special_chars_!@#",
            "123numeric",
            "a",
            "very_long_tenant_identifier_that_exceeds_normal_limits_and_should_be_truncated_properly"
        };
        
        for (String tenantId : testTenantIds) {
            try {
                String schemaName = UnifiedTenantResolver.mapTenantToSchema(tenantId);
                validateSchemaName(schemaName);
                Log.debugf("Schema naming validation passed: '%s' -> '%s'", tenantId, schemaName);
                
            } catch (Exception e) {
                Log.warnf("Schema naming validation failed for tenant ID '%s': %s", 
                         tenantId, e.getMessage());
                throw e;
            }
        }
    }

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || schemaName.trim().isEmpty()) {
            throw new IllegalArgumentException("Schema name cannot be null or empty");
        }
        
        if (schemaName.length() > 63) {
            throw new IllegalArgumentException(
                String.format("Schema name '%s' exceeds PostgreSQL maximum length of 63 characters", 
                            schemaName));
        }
        
        if (!schemaName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw new IllegalArgumentException(
                String.format("Schema name '%s' contains invalid characters", schemaName));
        }
        
        if (!schemaName.startsWith(UnifiedTenantResolver.getTenantSchemaPrefix()) && 
            !schemaName.equals(UnifiedTenantResolver.getDefaultSchema())) {
            throw new IllegalArgumentException(
                String.format("Schema name '%s' does not follow naming convention", schemaName));
        }
    }

    private void logConfigurationSummary() {
        Log.infof("Multi-tenancy configuration summary:");
        Log.infof("  - Default schema: %s", UnifiedTenantResolver.getDefaultSchema());
        Log.infof("  - Tenant schema prefix: %s", UnifiedTenantResolver.getTenantSchemaPrefix());
        Log.infof("  - Schema naming convention: %s<tenant_slug>", 
                 UnifiedTenantResolver.getTenantSchemaPrefix());
        Log.infof("  - Multi-tenancy mode: SCHEMA");
        Log.infof("  - Tenant resolution: Request-scoped via TenantContext");
    }

    @jakarta.annotation.PreDestroy
    public void cleanup() {
        Log.info("Cleaning up multi-tenancy configuration...");
        Log.info("Multi-tenancy configuration cleanup completed");
    }

    @Override
    public String toString() {
        return String.format(
            "MultiTenancyConfiguration{defaultSchema='%s', tenantPrefix='%s', mode='SCHEMA'}",
            UnifiedTenantResolver.getDefaultSchema(),
            UnifiedTenantResolver.getTenantSchemaPrefix()
        );
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
