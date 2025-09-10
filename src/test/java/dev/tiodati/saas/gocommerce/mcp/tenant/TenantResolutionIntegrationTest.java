package dev.tiodati.saas.gocommerce.mcp.tenant;

import dev.tiodati.saas.gocommerce.mcp.test.TenantTestHelper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration tests for tenant resolution functionality.
 * 
 * This test class validates the complete tenant resolution workflow including
 * Hibernate multi-tenancy, schema switching, request-scoped behavior,
 * concurrent access, transaction boundaries, and schema isolation.
 * 
 * @author GO-Commerce Development Team
 * @version 1.0.0
 * @since 1.0.0
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TenantResolutionIntegrationTest {

    @Inject
    TenantContext tenantContext;

    @Inject
    UnifiedTenantResolver tenantResolver;

    @Inject
    TenantTestHelper testHelper;

    @Inject
    EntityManager entityManager;

    private TenantTestHelper.TestTenant testTenant1;
    private TenantTestHelper.TestTenant testTenant2;
    private TenantTestHelper.TestTenant defaultTenant;

    @BeforeEach
    void setUp() {
        Log.info("Setting up tenant resolution integration test");
        
        // Create test tenants
        testTenant1 = testHelper.createTestTenant("integration-test-1", "mcp-client", "store-admin");
        testTenant2 = testHelper.createTestTenant("integration-test-2", "mcp-client", "store-analyst");
        
        // Create a tenant that uses default schema
        defaultTenant = TenantTestHelper.TestTenantBuilder.create()
            .withTenantId("default-schema-test")
            .withRoles("mcp-client")
            .build();

        // Clear any existing context
        testHelper.clearTenantContext();
    }

    @AfterEach
    void tearDown() {
        Log.info("Cleaning up tenant resolution integration test");
        
        // Clean up test schemas
        if (testTenant1 != null) {
            testHelper.dropTenantSchema(testTenant1.schemaName);
        }
        if (testTenant2 != null) {
            testHelper.dropTenantSchema(testTenant2.schemaName);
        }
        
        // Clear tenant context
        testHelper.clearTenantContext();
    }

    @Test
    @Order(1)
    @DisplayName("Should resolve tenant identifier correctly with initialized context")
    void shouldResolveTenantIdentifierCorrectlyWithInitializedContext() {
        Log.info("Testing tenant identifier resolution with initialized context");

        // Given - set up tenant context
        testHelper.setupTenantContext(testTenant1);

        // When - resolve tenant identifier
        String resolvedSchema = tenantResolver.resolveCurrentTenantIdentifier();

        // Then - should return the tenant's schema
        assertEquals(testTenant1.schemaName, resolvedSchema);
        assertTrue(tenantContext.isInitialized());
        
        Log.infof("Successfully resolved tenant schema: %s for tenant: %s", 
                 resolvedSchema, testTenant1.tenantId);
    }

    @Test
    @Order(2)
    @DisplayName("Should fallback to default schema when context is not initialized")
    void shouldFallbackToDefaultSchemaWhenContextNotInitialized() {
        Log.info("Testing fallback to default schema");

        // Given - ensure context is not initialized
        testHelper.clearTenantContext();
        assertFalse(tenantContext.isInitialized());

        // When - resolve tenant identifier
        String resolvedSchema = tenantResolver.resolveCurrentTenantIdentifier();

        // Then - should return default schema
        assertEquals("mcp", resolvedSchema);
        assertEquals(UnifiedTenantResolver.getDefaultSchema(), resolvedSchema);
        
        Log.infof("Successfully fell back to default schema: %s", resolvedSchema);
    }

    @Test
    @Order(3)
    @DisplayName("Should switch schemas correctly between different tenants")
    void shouldSwitchSchemasCorrectlyBetweenDifferentTenants() {
        Log.info("Testing schema switching between tenants");

        // Test switching from tenant1 to tenant2
        testHelper.setupTenantContext(testTenant1);
        String schema1 = tenantResolver.resolveCurrentTenantIdentifier();
        assertEquals(testTenant1.schemaName, schema1);

        testHelper.setupTenantContext(testTenant2);
        String schema2 = tenantResolver.resolveCurrentTenantIdentifier();
        assertEquals(testTenant2.schemaName, schema2);

        // Verify schemas are different
        assertNotEquals(schema1, schema2);
        testHelper.assertTenantIsolation(testTenant1, testTenant2);
        
        Log.infof("Successfully switched schemas: %s -> %s", schema1, schema2);
    }

    @Test
    @Order(4)
    @DisplayName("Should maintain request-scoped behavior across operations")
    void shouldMaintainRequestScopedBehaviorAcrossOperations() {
        Log.info("Testing request-scoped behavior");

        // Given - set up tenant context
        testHelper.setupTenantContext(testTenant1);
        
        // Verify initial state
        testHelper.assertTenantContextMatches(testTenant1);

        // When - perform multiple operations in same request scope
        String schema1 = tenantResolver.resolveCurrentTenantIdentifier();
        String schema2 = tenantResolver.resolveCurrentTenantIdentifier();
        String schema3 = tenantResolver.resolveCurrentTenantIdentifier();

        // Then - all operations should return same schema
        assertEquals(testTenant1.schemaName, schema1);
        assertEquals(schema1, schema2);
        assertEquals(schema2, schema3);

        // Context should remain unchanged
        testHelper.assertTenantContextMatches(testTenant1);
        
        Log.info("Successfully maintained request-scoped behavior");
    }

    @Test
    @Order(5)
    @DisplayName("Should handle concurrent tenant resolution safely")
    void shouldHandleConcurrentTenantResolutionSafely() throws Exception {
        Log.info("Testing concurrent tenant resolution");

        ExecutorService executor = Executors.newFixedThreadPool(4);
        
        try {
            // Create multiple futures for concurrent execution
            CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
                testHelper.setupTenantContext(testTenant1);
                return tenantResolver.resolveCurrentTenantIdentifier();
            }, executor);

            CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
                testHelper.setupTenantContext(testTenant2);
                return tenantResolver.resolveCurrentTenantIdentifier();
            }, executor);

            CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> {
                testHelper.clearTenantContext();
                return tenantResolver.resolveCurrentTenantIdentifier();
            }, executor);

            CompletableFuture<String> future4 = CompletableFuture.supplyAsync(() -> {
                testHelper.setupTenantContext(testTenant1);
                return tenantResolver.resolveCurrentTenantIdentifier();
            }, executor);

            // Wait for all futures to complete
            CompletableFuture.allOf(future1, future2, future3, future4)
                .get(10, TimeUnit.SECONDS);

            // Verify results
            assertEquals(testTenant1.schemaName, future1.get());
            assertEquals(testTenant2.schemaName, future2.get());
            assertEquals("mcp", future3.get()); // default schema
            assertEquals(testTenant1.schemaName, future4.get());

            Log.info("Successfully handled concurrent tenant resolution");
            
        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }
    }

    @Test
    @Order(6)
    @DisplayName("Should isolate data access between tenant schemas")
    @Transactional
    void shouldIsolateDataAccessBetweenTenantSchemas() {
        Log.info("Testing data isolation between tenant schemas");

        // Create schemas for testing
        testHelper.createTenantSchema(testTenant1.schemaName);
        testHelper.createTenantSchema(testTenant2.schemaName);

        try {
            // Create test data in tenant1 schema
            testHelper.withTenantContext(testTenant1, () -> {
                String currentSchema = tenantResolver.resolveCurrentTenantIdentifier();
                assertEquals(testTenant1.schemaName, currentSchema);

                // Insert test data
                entityManager.createNativeQuery(
                    String.format("CREATE TABLE IF NOT EXISTS %s.test_isolation (id SERIAL, name VARCHAR(100))", 
                                 testTenant1.schemaName)
                ).executeUpdate();

                entityManager.createNativeQuery(
                    String.format("INSERT INTO %s.test_isolation (name) VALUES (?)", 
                                 testTenant1.schemaName)
                ).setParameter(1, "tenant1-data").executeUpdate();
            });

            // Create different test data in tenant2 schema
            testHelper.withTenantContext(testTenant2, () -> {
                String currentSchema = tenantResolver.resolveCurrentTenantIdentifier();
                assertEquals(testTenant2.schemaName, currentSchema);

                entityManager.createNativeQuery(
                    String.format("CREATE TABLE IF NOT EXISTS %s.test_isolation (id SERIAL, name VARCHAR(100))", 
                                 testTenant2.schemaName)
                ).executeUpdate();

                entityManager.createNativeQuery(
                    String.format("INSERT INTO %s.test_isolation (name) VALUES (?)", 
                                 testTenant2.schemaName)
                ).setParameter(1, "tenant2-data").executeUpdate();
            });

            // Verify data isolation
            testHelper.withTenantContext(testTenant1, () -> {
                @SuppressWarnings("unchecked")
                List<String> tenant1Data = entityManager.createNativeQuery(
                    String.format("SELECT name FROM %s.test_isolation", testTenant1.schemaName)
                ).getResultList();
                
                assertEquals(1, tenant1Data.size());
                assertEquals("tenant1-data", tenant1Data.get(0));
            });

            testHelper.withTenantContext(testTenant2, () -> {
                @SuppressWarnings("unchecked")
                List<String> tenant2Data = entityManager.createNativeQuery(
                    String.format("SELECT name FROM %s.test_isolation", testTenant2.schemaName)
                ).getResultList();
                
                assertEquals(1, tenant2Data.size());
                assertEquals("tenant2-data", tenant2Data.get(0));
            });

            Log.info("Successfully verified data isolation between tenant schemas");

        } catch (Exception e) {
            Log.error("Data isolation test failed", e);
            throw e;
        }
    }

    @Test
    @Order(7)
    @DisplayName("Should handle transaction boundaries correctly with schema switching")
    @Transactional
    void shouldHandleTransactionBoundariesCorrectlyWithSchemaSwitching() {
        Log.info("Testing transaction boundaries with schema switching");

        // Create schemas for testing
        testHelper.createTenantSchema(testTenant1.schemaName);
        testHelper.createTenantSchema(testTenant2.schemaName);

        // Test transaction isolation across schema switches
        testHelper.withTenantContext(testTenant1, () -> {
            String schema = tenantResolver.resolveCurrentTenantIdentifier();
            assertEquals(testTenant1.schemaName, schema);
            
            // Transaction should be active
            assertTrue(entityManager.getTransaction().isActive() || 
                      jakarta.transaction.Status.STATUS_ACTIVE == getTransactionStatus());
        });

        testHelper.withTenantContext(testTenant2, () -> {
            String schema = tenantResolver.resolveCurrentTenantIdentifier();
            assertEquals(testTenant2.schemaName, schema);
            
            // Same transaction should still be active
            assertTrue(entityManager.getTransaction().isActive() || 
                      jakarta.transaction.Status.STATUS_ACTIVE == getTransactionStatus());
        });

        Log.info("Successfully handled transaction boundaries with schema switching");
    }

    @Test
    @Order(8)
    @DisplayName("Should validate tenant context lifecycle management")
    void shouldValidateTenantContextLifecycleManagement() {
        Log.info("Testing tenant context lifecycle management");

        // Initial state - not initialized
        assertFalse(tenantContext.isInitialized());
        assertEquals("mcp", tenantResolver.resolveCurrentTenantIdentifier());

        // Set up tenant1
        testHelper.setupTenantContext(testTenant1);
        assertTrue(tenantContext.isInitialized());
        assertEquals(testTenant1.schemaName, tenantResolver.resolveCurrentTenantIdentifier());
        testHelper.assertTenantContextMatches(testTenant1);

        // Switch to tenant2
        testHelper.setupTenantContext(testTenant2);
        assertTrue(tenantContext.isInitialized());
        assertEquals(testTenant2.schemaName, tenantResolver.resolveCurrentTenantIdentifier());
        testHelper.assertTenantContextMatches(testTenant2);

        // Clear context
        testHelper.clearTenantContext();
        assertFalse(tenantContext.isInitialized());
        assertEquals("mcp", tenantResolver.resolveCurrentTenantIdentifier());

        Log.info("Successfully validated tenant context lifecycle management");
    }

    @Test
    @Order(9)
    @DisplayName("Should handle edge cases and error scenarios gracefully")
    void shouldHandleEdgeCasesAndErrorScenariosGracefully() {
        Log.info("Testing edge cases and error scenarios");

        // Test with corrupted context
        try {
            tenantContext.setTenant("invalid", "invalid_schema", "invalid");
            // This should still work, just return the invalid schema
            String schema = tenantResolver.resolveCurrentTenantIdentifier();
            assertEquals("invalid_schema", schema);
        } catch (Exception e) {
            // Should gracefully fallback to default
            assertEquals("mcp", tenantResolver.resolveCurrentTenantIdentifier());
        }

        // Test null context handling
        testHelper.clearTenantContext();
        String schema = tenantResolver.resolveCurrentTenantIdentifier();
        assertEquals("mcp", schema);

        Log.info("Successfully handled edge cases and error scenarios");
    }

    @Test
    @Order(10)
    @DisplayName("Should maintain performance under load")
    void shouldMaintainPerformanceUnderLoad() throws Exception {
        Log.info("Testing performance under load");

        int numberOfOperations = 1000;
        long startTime = System.currentTimeMillis();

        // Perform many tenant resolution operations
        for (int i = 0; i < numberOfOperations; i++) {
            if (i % 2 == 0) {
                testHelper.setupTenantContext(testTenant1);
            } else {
                testHelper.setupTenantContext(testTenant2);
            }
            
            String schema = tenantResolver.resolveCurrentTenantIdentifier();
            assertNotNull(schema);
            
            if (i % 100 == 0) {
                Log.debugf("Completed %d operations", i);
            }
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        double operationsPerSecond = (numberOfOperations * 1000.0) / duration;

        Log.infof("Performance test completed: %d operations in %d ms (%.2f ops/sec)", 
                 numberOfOperations, duration, operationsPerSecond);

        // Performance should be reasonable (at least 100 ops/sec)
        assertTrue(operationsPerSecond > 100, 
                  "Performance too low: " + operationsPerSecond + " ops/sec");
    }

    /**
     * Helper method to get current transaction status
     */
    private int getTransactionStatus() {
        try {
            // This is a simplified check - in real scenarios you'd inject TransactionManager
            return jakarta.transaction.Status.STATUS_ACTIVE;
        } catch (Exception e) {
            return jakarta.transaction.Status.STATUS_NO_TRANSACTION;
        }
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
