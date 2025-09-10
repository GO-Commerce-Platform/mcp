package dev.tiodati.saas.gocommerce.mcp.tenant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.time.Instant;
import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TenantContext class.
 * 
 * Tests comprehensive functionality including tenant setup, user management,
 * role-based access control, input validation, and state management.
 * 
 * @author GO-Commerce Development Team
 * @version 1.0.0
 * @since 1.0.0
 */
class TenantContextTest {

    private TenantContext tenantContext;

    @BeforeEach
    void setUp() {
        tenantContext = new TenantContext();
    }

    @Test
    @DisplayName("Should initialize with empty state")
    void shouldInitializeWithEmptyState() {
        assertNull(tenantContext.getTenantId());
        assertNull(tenantContext.getSchemaName());
        assertNull(tenantContext.getTenantSlug());
        assertNull(tenantContext.getUserId());
        assertTrue(tenantContext.getRoles().isEmpty());
        assertFalse(tenantContext.isInitialized());
        assertNotNull(tenantContext.getCreatedAt());
        assertTrue(tenantContext.getCreatedAt().isBefore(Instant.now().plusSeconds(1)));
    }

    @Test
    @DisplayName("Should set tenant with valid inputs")
    void shouldSetTenantWithValidInputs() {
        // Given
        String tenantId = "acme-corp";
        String schemaName = "store_acme_corp";
        String tenantSlug = "acme_corp";

        // When
        tenantContext.setTenant(tenantId, schemaName, tenantSlug);

        // Then
        assertEquals(tenantId, tenantContext.getTenantId());
        assertEquals(schemaName, tenantContext.getSchemaName());
        assertEquals(tenantSlug, tenantContext.getTenantSlug());
        assertTrue(tenantContext.isInitialized());
    }

    @Test
    @DisplayName("Should handle special characters in tenant data")
    void shouldHandleSpecialCharactersInTenantData() {
        // Given - tenant with special characters
        String tenantId = "Test Store 123!@#";
        String schemaName = "store_test_store_123";
        String tenantSlug = "test_store_123";

        // When
        tenantContext.setTenant(tenantId, schemaName, tenantSlug);

        // Then
        assertEquals(tenantId, tenantContext.getTenantId());
        assertEquals(schemaName, tenantContext.getSchemaName());
        assertEquals(tenantSlug, tenantContext.getTenantSlug());
    }

    @Test
    @DisplayName("Should handle very long tenant names")
    void shouldHandleVeryLongTenantNames() {
        // Given - very long tenant name
        String tenantId = "a".repeat(100);
        String schemaName = "store_" + "a".repeat(50);
        String tenantSlug = "a".repeat(50);

        // When
        tenantContext.setTenant(tenantId, schemaName, tenantSlug);

        // Then
        assertEquals(tenantId, tenantContext.getTenantId());
        assertEquals(schemaName, tenantContext.getSchemaName());
        assertEquals(tenantSlug, tenantContext.getTenantSlug());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"", "   "})
    @DisplayName("Should reject null or empty tenant ID")
    void shouldRejectNullOrEmptyTenantId(String invalidTenantId) {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> tenantContext.setTenant(invalidTenantId, "store_test", "test")
        );
        assertEquals("tenantId cannot be null or empty", exception.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"", "   "})
    @DisplayName("Should reject null or empty schema name")
    void shouldRejectNullOrEmptySchemaName(String invalidSchemaName) {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> tenantContext.setTenant("test", invalidSchemaName, "test")
        );
        assertEquals("schemaName cannot be null or empty", exception.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"", "   "})
    @DisplayName("Should reject null or empty tenant slug")
    void shouldRejectNullOrEmptyTenantSlug(String invalidTenantSlug) {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> tenantContext.setTenant("test", "store_test", invalidTenantSlug)
        );
        assertEquals("tenantSlug cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Should set user with valid data and roles")
    void shouldSetUserWithValidDataAndRoles() {
        // Given
        String userId = "user123";
        Set<String> roles = Set.of("admin", "user", "analyst");

        // When
        tenantContext.setUser(userId, roles);

        // Then
        assertEquals(userId, tenantContext.getUserId());
        assertEquals(roles, tenantContext.getRoles());
    }

    @Test
    @DisplayName("Should handle null user ID")
    void shouldHandleNullUserId() {
        // Given
        Set<String> roles = Set.of("admin", "user");

        // When
        tenantContext.setUser(null, roles);

        // Then
        assertNull(tenantContext.getUserId());
        assertEquals(roles, tenantContext.getRoles());
    }

    @Test
    @DisplayName("Should handle null roles")
    void shouldHandleNullRoles() {
        // Given
        String userId = "user123";

        // When
        tenantContext.setUser(userId, null);

        // Then
        assertEquals(userId, tenantContext.getUserId());
        assertTrue(tenantContext.getRoles().isEmpty());
    }

    @Test
    @DisplayName("Should replace existing roles when setting new roles")
    void shouldReplaceExistingRolesWhenSettingNewRoles() {
        // Given - initial roles
        Set<String> initialRoles = new HashSet<>(Set.of("admin", "user"));
        tenantContext.setUser("user123", initialRoles);
        
        // When - update with new roles
        Set<String> newRoles = Set.of("analyst", "viewer");
        tenantContext.setUser("user123", newRoles);

        // Then
        assertEquals(newRoles, tenantContext.getRoles());
        assertFalse(tenantContext.hasRole("admin"));
        assertFalse(tenantContext.hasRole("user"));
        assertTrue(tenantContext.hasRole("analyst"));
        assertTrue(tenantContext.hasRole("viewer"));
    }

    @Test
    @DisplayName("Should check roles correctly with hasRole method")
    void shouldCheckRolesCorrectlyWithHasRole() {
        // Given
        Set<String> roles = Set.of("admin", "user", "analyst");
        tenantContext.setUser("user123", roles);

        // When & Then
        assertTrue(tenantContext.hasRole("admin"));
        assertTrue(tenantContext.hasRole("user"));
        assertTrue(tenantContext.hasRole("analyst"));
        assertFalse(tenantContext.hasRole("manager"));
        assertFalse(tenantContext.hasRole(""));
        assertFalse(tenantContext.hasRole(null));
    }

    @Test
    @DisplayName("Should check multiple roles correctly with hasAnyRole method")
    void shouldCheckMultipleRolesCorrectlyWithHasAnyRole() {
        // Given
        Set<String> roles = Set.of("admin", "user", "analyst");
        tenantContext.setUser("user123", roles);

        // When & Then
        assertTrue(tenantContext.hasAnyRole("admin"));
        assertTrue(tenantContext.hasAnyRole("admin", "manager"));
        assertTrue(tenantContext.hasAnyRole("manager", "user"));
        assertTrue(tenantContext.hasAnyRole("analyst", "editor", "viewer"));
        assertFalse(tenantContext.hasAnyRole("manager"));
        assertFalse(tenantContext.hasAnyRole("manager", "editor"));
        assertFalse(tenantContext.hasAnyRole("manager", "editor", "viewer"));
        assertFalse(tenantContext.hasAnyRole());
    }

    @Test
    @DisplayName("Should handle hasAnyRole with null elements")
    void shouldHandleHasAnyRoleWithNullElements() {
        // Given
        Set<String> roles = Set.of("admin", "user");
        tenantContext.setUser("user123", roles);

        // When & Then
        assertTrue(tenantContext.hasAnyRole("admin", null));
        assertFalse(tenantContext.hasAnyRole("manager", null));
        assertFalse(tenantContext.hasAnyRole((String) null));
    }

    @Test
    @DisplayName("Should return immutable roles set")
    void shouldReturnImmutableRolesSet() {
        // Given
        Set<String> roles = new HashSet<>(Set.of("admin", "user"));
        tenantContext.setUser("user123", roles);

        // When
        Set<String> returnedRoles = tenantContext.getRoles();

        // Then
        assertThrows(UnsupportedOperationException.class, 
            () -> returnedRoles.add("hacker"));
    }

    @Test
    @DisplayName("Should check initialization state correctly")
    void shouldCheckInitializationStateCorrectly() {
        // Initially not initialized
        assertFalse(tenantContext.isInitialized());

        // Set only user - still not initialized
        tenantContext.setUser("user123", Set.of("admin"));
        assertFalse(tenantContext.isInitialized());

        // Set tenant - now initialized
        tenantContext.setTenant("test", "store_test", "test");
        assertTrue(tenantContext.isInitialized());

        // Clear tenant data - not initialized again
        tenantContext.clear();
        assertFalse(tenantContext.isInitialized());
    }

    @Test
    @DisplayName("Should clear all state correctly")
    void shouldClearAllStateCorrectly() {
        // Given - fully populated context
        tenantContext.setTenant("test", "store_test", "test");
        tenantContext.setUser("user123", Set.of("admin", "user"));

        // When
        tenantContext.clear();

        // Then
        assertNull(tenantContext.getTenantId());
        assertNull(tenantContext.getSchemaName());
        assertNull(tenantContext.getTenantSlug());
        assertNull(tenantContext.getUserId());
        assertTrue(tenantContext.getRoles().isEmpty());
        assertFalse(tenantContext.isInitialized());
        // createdAt should remain unchanged
        assertNotNull(tenantContext.getCreatedAt());
    }

    @Test
    @DisplayName("Should maintain creation time across operations")
    void shouldMaintainCreationTimeAcrossOperations() {
        // Given
        Instant initialCreatedAt = tenantContext.getCreatedAt();

        // When
        tenantContext.setTenant("test", "store_test", "test");
        tenantContext.setUser("user123", Set.of("admin"));
        tenantContext.clear();

        // Then
        assertEquals(initialCreatedAt, tenantContext.getCreatedAt());
    }

    @Test
    @DisplayName("Should produce meaningful toString representation")
    void shouldProduceMeaningfulToStringRepresentation() {
        // Given
        tenantContext.setTenant("acme-corp", "store_acme_corp", "acme_corp");
        tenantContext.setUser("user123", Set.of("admin", "user"));

        // When
        String toString = tenantContext.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("TenantContext"));
        assertTrue(toString.contains("acme-corp"));
        assertTrue(toString.contains("store_acme_corp"));
        assertTrue(toString.contains("acme_corp"));
        assertTrue(toString.contains("user123"));
        assertTrue(toString.contains("admin"));
        assertTrue(toString.contains("user"));
    }

    @Test
    @DisplayName("Should handle toString with partial data")
    void shouldHandleToStringWithPartialData() {
        // Given - only tenant set
        tenantContext.setTenant("test", "store_test", "test");

        // When
        String toString = tenantContext.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("TenantContext"));
        assertTrue(toString.contains("test"));
        assertTrue(toString.contains("store_test"));
        // Just verify that null userId and empty roles are handled properly
        // (the exact format may vary but should contain the basic structure)
        assertTrue(toString.contains("null") || toString.contains("user"));
        assertTrue(toString.contains("roles") || toString.contains("[]"));
    }

    @Test
    @DisplayName("Should handle concurrent modifications safely")
    void shouldHandleConcurrentModificationsSafely() {
        // Given
        Set<String> mutableRoles = new HashSet<>(Set.of("admin", "user"));
        tenantContext.setUser("user123", mutableRoles);

        // When - modify original roles after setting
        mutableRoles.add("hacker");

        // Then - tenant context should be unaffected
        assertEquals(2, tenantContext.getRoles().size());
        assertTrue(tenantContext.hasRole("admin"));
        assertTrue(tenantContext.hasRole("user"));
        assertFalse(tenantContext.hasRole("hacker"));
    }

    @Test
    @DisplayName("Should validate edge cases for role checking")
    void shouldValidateEdgeCasesForRoleChecking() {
        // Given - empty roles
        tenantContext.setUser("user123", Set.of());

        // When & Then
        assertFalse(tenantContext.hasRole("admin"));
        assertFalse(tenantContext.hasRole(""));
        assertFalse(tenantContext.hasRole(null));
        assertFalse(tenantContext.hasAnyRole("admin", "user"));
        assertFalse(tenantContext.hasAnyRole());
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
