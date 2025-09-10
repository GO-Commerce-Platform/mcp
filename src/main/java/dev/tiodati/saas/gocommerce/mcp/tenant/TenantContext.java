package dev.tiodati.saas.gocommerce.mcp.tenant;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

/**
 * Request-scoped context holder for tenant information.
 * 
 * This class maintains tenant-specific information throughout a single request lifecycle.
 * It provides thread-safe access to the current tenant ID, schema name, and roles
 * for the authenticated user within the scope of a single HTTP request.
 * 
 * @author GO-Commerce Development Team
 * @version 1.0.0
 * @since 1.0.0
 */
@RequestScoped
@Named("tenantContext")
public class TenantContext {

    private String tenantId;
    private String schemaName;
    private String tenantSlug;
    private java.util.Set<String> roles;
    private String userId;
    private final java.time.Instant createdAt = java.time.Instant.now();

    public TenantContext() {
        this.roles = new java.util.HashSet<>();
    }

    public void setTenant(String tenantId, String schemaName, String tenantSlug) {
        validateNotEmpty(tenantId, "tenantId");
        validateNotEmpty(schemaName, "schemaName");
        validateNotEmpty(tenantSlug, "tenantSlug");
        
        this.tenantId = tenantId;
        this.schemaName = schemaName;
        this.tenantSlug = tenantSlug;
    }

    public void setUser(String userId, java.util.Set<String> userRoles) {
        this.userId = userId;
        if (userRoles != null) {
            this.roles.clear();
            this.roles.addAll(userRoles);
        }
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getTenantSlug() {
        return tenantSlug;
    }

    public String getUserId() {
        return userId;
    }

    public java.util.Set<String> getRoles() {
        return java.util.Collections.unmodifiableSet(roles);
    }

    public java.time.Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isInitialized() {
        return tenantId != null && schemaName != null && tenantSlug != null;
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean hasAnyRole(String... requiredRoles) {
        for (String role : requiredRoles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }

    public void clear() {
        this.tenantId = null;
        this.schemaName = null;
        this.tenantSlug = null;
        this.userId = null;
        this.roles.clear();
    }

    private void validateNotEmpty(String value, String paramName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(paramName + " cannot be null or empty");
        }
    }

    @Override
    public String toString() {
        return String.format(
            "TenantContext{tenantId='%s', schemaName='%s', tenantSlug='%s', userId='%s', roles=%s, createdAt=%s}",
            tenantId, schemaName, tenantSlug, userId, roles, createdAt
        );
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
