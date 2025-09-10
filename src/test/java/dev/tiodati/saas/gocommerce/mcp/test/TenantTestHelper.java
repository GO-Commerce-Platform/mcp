package dev.tiodati.saas.gocommerce.mcp.test;

import dev.tiodati.saas.gocommerce.mcp.tenant.TenantContext;
import dev.tiodati.saas.gocommerce.mcp.tenant.UnifiedTenantResolver;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Utility class providing helper methods for tenant resolution testing.
 * 
 * This class provides comprehensive utilities for multi-tenant testing including
 * tenant creation, JWT token simulation, data builders, schema management,
 * and assertion helpers for verifying tenant isolation.
 * 
 * @author GO-Commerce Development Team
 * @version 1.0.0
 * @since 1.0.0
 */
@ApplicationScoped
public class TenantTestHelper {

    @Inject
    EntityManager entityManager;

    @Inject
    TenantContext tenantContext;

    // Test tenant configurations
    public static final String[] COMMON_TENANT_IDS = {
        "acme-corp",
        "test-store",
        "simple-tenant",
        "special-chars-!@#",
        "unicode-café",
        "123-numeric-start"
    };

    public static final String[] COMMON_USER_ROLES = {
        "mcp-client",
        "store-admin", 
        "store-analyst",
        "store-viewer"
    };

    /**
     * Test tenant data container
     */
    public static class TestTenant {
        public final String tenantId;
        public final String schemaName;
        public final String tenantSlug;
        public final String userId;
        public final Set<String> roles;

        public TestTenant(String tenantId, String schemaName, String tenantSlug, 
                         String userId, Set<String> roles) {
            this.tenantId = tenantId;
            this.schemaName = schemaName;
            this.tenantSlug = tenantSlug;
            this.userId = userId;
            this.roles = new HashSet<>(roles);
        }

        @Override
        public String toString() {
            return String.format("TestTenant{id='%s', schema='%s', slug='%s', user='%s', roles=%s}",
                tenantId, schemaName, tenantSlug, userId, roles);
        }
    }

    /**
     * Creates a test tenant with randomized data
     */
    public TestTenant createRandomTestTenant() {
        String tenantId = generateRandomTenantId();
        String schemaName = UnifiedTenantResolver.mapTenantToSchema(tenantId);
        String tenantSlug = extractSlugFromSchema(schemaName);
        String userId = generateRandomUserId();
        Set<String> roles = generateRandomRoles();

        return new TestTenant(tenantId, schemaName, tenantSlug, userId, roles);
    }

    /**
     * Creates a specific test tenant with given parameters
     */
    public TestTenant createTestTenant(String tenantId, String... roles) {
        String schemaName = UnifiedTenantResolver.mapTenantToSchema(tenantId);
        String tenantSlug = extractSlugFromSchema(schemaName);
        String userId = "test-user-" + tenantId.hashCode();
        Set<String> roleSet = roles.length > 0 ? Set.of(roles) : Set.of("mcp-client");

        return new TestTenant(tenantId, schemaName, tenantSlug, userId, roleSet);
    }

    /**
     * Sets up tenant context with the given test tenant
     */
    public void setupTenantContext(TestTenant tenant) {
        tenantContext.clear();
        tenantContext.setTenant(tenant.tenantId, tenant.schemaName, tenant.tenantSlug);
        tenantContext.setUser(tenant.userId, tenant.roles);
    }

    /**
     * Clears the current tenant context
     */
    public void clearTenantContext() {
        tenantContext.clear();
    }

    /**
     * Executes a block of code with a specific tenant context
     */
    public <T> T withTenantContext(TestTenant tenant, Supplier<T> operation) {
        TestTenant originalTenant = getCurrentTenant();
        try {
            setupTenantContext(tenant);
            return operation.get();
        } finally {
            if (originalTenant != null) {
                setupTenantContext(originalTenant);
            } else {
                clearTenantContext();
            }
        }
    }

    /**
     * Executes a block of code with a specific tenant context (void version)
     */
    public void withTenantContext(TestTenant tenant, Runnable operation) {
        withTenantContext(tenant, () -> {
            operation.run();
            return null;
        });
    }

    /**
     * Gets the current tenant from context as TestTenant
     */
    public TestTenant getCurrentTenant() {
        if (!tenantContext.isInitialized()) {
            return null;
        }
        return new TestTenant(
            tenantContext.getTenantId(),
            tenantContext.getSchemaName(),
            tenantContext.getTenantSlug(),
            tenantContext.getUserId(),
            tenantContext.getRoles()
        );
    }

    /**
     * Creates database schema for the given tenant
     */
    @Transactional
    public void createTenantSchema(String schemaName) {
        try {
            // Create schema if it doesn't exist
            entityManager.createNativeQuery(
                String.format("CREATE SCHEMA IF NOT EXISTS %s", schemaName)
            ).executeUpdate();
            
            // Create basic tables in the tenant schema if needed
            // This would typically be done by Flyway or Liquibase in real scenarios
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create tenant schema: " + schemaName, e);
        }
    }

    /**
     * Drops database schema for the given tenant
     */
    @Transactional
    public void dropTenantSchema(String schemaName) {
        try {
            // Only drop tenant schemas, not system schemas
            if (UnifiedTenantResolver.isTenantSchema(schemaName)) {
                entityManager.createNativeQuery(
                    String.format("DROP SCHEMA IF EXISTS %s CASCADE", schemaName)
                ).executeUpdate();
            }
        } catch (Exception e) {
            // Log warning but don't fail the test
            System.err.println("Warning: Failed to drop tenant schema: " + schemaName + " - " + e.getMessage());
        }
    }

    /**
     * Verifies that tenant contexts are properly isolated
     */
    public void assertTenantIsolation(TestTenant tenant1, TestTenant tenant2) {
        assertNotNull(tenant1, "Tenant 1 should not be null");
        assertNotNull(tenant2, "Tenant 2 should not be null");
        
        assertNotEquals(tenant1.tenantId, tenant2.tenantId, 
            "Tenant IDs should be different for isolation test");
        assertNotEquals(tenant1.schemaName, tenant2.schemaName, 
            "Schema names should be different for isolation test");
    }

    /**
     * Asserts that the current tenant context matches the expected tenant
     */
    public void assertTenantContextMatches(TestTenant expectedTenant) {
        if (expectedTenant == null) {
            assertFalse(tenantContext.isInitialized(), "Tenant context should not be initialized");
            return;
        }

        assertTrue(tenantContext.isInitialized(), "Tenant context should be initialized");
        assertEquals(expectedTenant.tenantId, tenantContext.getTenantId(), "Tenant ID mismatch");
        assertEquals(expectedTenant.schemaName, tenantContext.getSchemaName(), "Schema name mismatch");
        assertEquals(expectedTenant.tenantSlug, tenantContext.getTenantSlug(), "Tenant slug mismatch");
        assertEquals(expectedTenant.userId, tenantContext.getUserId(), "User ID mismatch");
        assertEquals(expectedTenant.roles, tenantContext.getRoles(), "Roles mismatch");
    }

    /**
     * Generates a JWT-like token payload for testing (simplified)
     */
    public String generateTestJWTPayload(TestTenant tenant) {
        return String.format("""
            {
                "sub": "%s",
                "storeId": "%s",
                "roles": [%s],
                "iat": %d,
                "exp": %d
            }
            """,
            tenant.userId,
            tenant.tenantId,
            String.join(",", tenant.roles.stream().map(r -> "\"" + r + "\"").toList()),
            Instant.now().getEpochSecond(),
            Instant.now().plusSeconds(3600).getEpochSecond()
        );
    }

    /**
     * Executes code within a transaction boundary
     */
    @Transactional
    public void inTransaction(Runnable operation) {
        operation.run();
    }

    /**
     * Executes code within a transaction boundary with return value
     */
    @Transactional
    public <T> T inTransaction(Supplier<T> operation) {
        return operation.get();
    }

    /**
     * Executes code within a specific tenant context and transaction
     */
    @Transactional
    public void inTransactionWithTenant(TestTenant tenant, Runnable operation) {
        withTenantContext(tenant, operation);
    }

    /**
     * Simulates concurrent tenant access for testing race conditions
     */
    public void simulateConcurrentTenantAccess(TestTenant tenant1, TestTenant tenant2, 
                                             Consumer<TestTenant> operation) throws InterruptedException {
        Thread thread1 = new Thread(() -> {
            try {
                withTenantContext(tenant1, () -> operation.accept(tenant1));
            } catch (Exception e) {
                throw new RuntimeException("Thread 1 failed", e);
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                withTenantContext(tenant2, () -> operation.accept(tenant2));
            } catch (Exception e) {
                throw new RuntimeException("Thread 2 failed", e);
            }
        });

        thread1.start();
        thread2.start();

        thread1.join(5000); // 5 second timeout
        thread2.join(5000);

        if (thread1.isAlive() || thread2.isAlive()) {
            thread1.interrupt();
            thread2.interrupt();
            fail("Concurrent tenant access test timed out");
        }
    }

    // Private helper methods

    private String generateRandomTenantId() {
        String[] prefixes = {"tenant", "store", "shop", "company", "org"};
        String prefix = prefixes[ThreadLocalRandom.current().nextInt(prefixes.length)];
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return prefix + "-" + suffix;
    }

    private String generateRandomUserId() {
        return "user-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private Set<String> generateRandomRoles() {
        Set<String> roles = new HashSet<>();
        roles.add("mcp-client"); // Always include base role
        
        // Randomly add additional roles
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextBoolean()) roles.add("store-admin");
        if (random.nextBoolean()) roles.add("store-analyst");
        if (random.nextBoolean()) roles.add("store-viewer");
        
        return roles;
    }

    private String extractSlugFromSchema(String schemaName) {
        return UnifiedTenantResolver.extractTenantSlug(schemaName);
    }

    /**
     * Builder for creating test tenants with fluent interface
     */
    public static class TestTenantBuilder {
        private String tenantId;
        private String userId;
        private Set<String> roles = new HashSet<>();

        public static TestTenantBuilder create() {
            return new TestTenantBuilder();
        }

        public TestTenantBuilder withTenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public TestTenantBuilder withUserId(String userId) {
            this.userId = userId;
            return this;
        }

        public TestTenantBuilder withRole(String role) {
            this.roles.add(role);
            return this;
        }

        public TestTenantBuilder withRoles(String... roles) {
            this.roles.addAll(Set.of(roles));
            return this;
        }

        public TestTenant build() {
            if (tenantId == null) {
                tenantId = "test-tenant-" + System.currentTimeMillis();
            }
            if (userId == null) {
                userId = "test-user-" + tenantId.hashCode();
            }
            if (roles.isEmpty()) {
                roles.add("mcp-client");
            }

            String schemaName = UnifiedTenantResolver.mapTenantToSchema(tenantId);
            String tenantSlug = UnifiedTenantResolver.extractTenantSlug(schemaName);

            return new TestTenant(tenantId, schemaName, tenantSlug, userId, roles);
        }
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
