package dev.tiodati.saas.gocommerce.mcp.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Unified tenant resolver for Hibernate multi-tenancy.
 * 
 * This class implements Hibernate's CurrentTenantIdentifierResolver interface
 * to provide schema-based multi-tenancy support. It resolves the current tenant
 * identifier from the TenantContext and maps it to the appropriate database schema.
 * 
 * @author GO-Commerce Development Team
 * @version 1.0.0
 * @since 1.0.0
 */
@ApplicationScoped
public class UnifiedTenantResolver implements CurrentTenantIdentifierResolver {

    private static final String DEFAULT_SCHEMA = "mcp";
    private static final String TENANT_SCHEMA_PREFIX = "store_";

    @Inject
    TenantContext tenantContext;

    public UnifiedTenantResolver() {
        Log.debug("UnifiedTenantResolver initialized");
    }

    @Override
    public String resolveCurrentTenantIdentifier() {
        try {
            if (tenantContext != null && tenantContext.isInitialized()) {
                String schemaName = tenantContext.getSchemaName();
                Log.debugf("Resolved tenant schema: %s for tenant: %s", 
                          schemaName, tenantContext.getTenantId());
                return schemaName;
            }
            
            Log.debug("No tenant context found, using default schema: " + DEFAULT_SCHEMA);
            return DEFAULT_SCHEMA;
            
        } catch (Exception e) {
            Log.warnf(e, "Error resolving tenant identifier, falling back to default schema: %s", 
                     DEFAULT_SCHEMA);
            return DEFAULT_SCHEMA;
        }
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    public static String mapTenantToSchema(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }
        
        String normalizedSlug = normalizeSchemaName(tenantId);
        String schemaName = TENANT_SCHEMA_PREFIX + normalizedSlug;
        
        Log.debugf("Mapped tenant ID '%s' to schema '%s'", tenantId, schemaName);
        return schemaName;
    }

    private static String normalizeSchemaName(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Input cannot be null or empty");
        }
        
        String normalized = input.trim().toLowerCase()
            .replaceAll("[^a-z0-9_]", "_")
            .replaceAll("_{2,}", "_")
            .replaceAll("^_+|_+$", "");
        
        if (!normalized.isEmpty() && Character.isDigit(normalized.charAt(0))) {
            normalized = "t_" + normalized;
        }
        
        if (normalized.isEmpty()) {
            normalized = "tenant";
        }
        
        int maxLength = 63 - TENANT_SCHEMA_PREFIX.length();
        if (normalized.length() > maxLength) {
            normalized = normalized.substring(0, maxLength);
            normalized = normalized.replaceAll("_+$", "");
        }
        
        return normalized;
    }

    public static String getDefaultSchema() {
        return DEFAULT_SCHEMA;
    }

    public static String getTenantSchemaPrefix() {
        return TENANT_SCHEMA_PREFIX;
    }

    public static boolean isTenantSchema(String schemaName) {
        return schemaName != null && 
               schemaName.startsWith(TENANT_SCHEMA_PREFIX) &&
               schemaName.length() > TENANT_SCHEMA_PREFIX.length();
    }

    public static String extractTenantSlug(String schemaName) {
        if (isTenantSchema(schemaName)) {
            return schemaName.substring(TENANT_SCHEMA_PREFIX.length());
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("UnifiedTenantResolver{defaultSchema='%s', tenantPrefix='%s'}",
                           DEFAULT_SCHEMA, TENANT_SCHEMA_PREFIX);
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
