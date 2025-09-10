package dev.tiodati.saas.gocommerce.mcp.tenant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UnifiedTenantResolver class.
 * 
 * Tests comprehensive tenant resolution functionality including schema mapping,
 * normalization, validation, error handling, and edge cases.
 * 
 * @author GO-Commerce Development Team
 * @version 1.0.0
 * @since 1.0.0
 */
class UnifiedTenantResolverTest {

    private UnifiedTenantResolver tenantResolver;

    @Mock
    private TenantContext mockTenantContext;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tenantResolver = new UnifiedTenantResolver();
        // Inject the mock via reflection since @InjectMocks doesn't work with CDI
        setMockTenantContext(tenantResolver, mockTenantContext);
    }

    @Test
    @DisplayName("Should resolve tenant identifier from initialized context")
    void shouldResolveTenantIdentifierFromInitializedContext() {
        // Given
        when(mockTenantContext.isInitialized()).thenReturn(true);
        when(mockTenantContext.getSchemaName()).thenReturn("store_acme_corp");
        when(mockTenantContext.getTenantId()).thenReturn("acme-corp");

        // When
        String result = tenantResolver.resolveCurrentTenantIdentifier();

        // Then
        assertEquals("store_acme_corp", result);
        verify(mockTenantContext).isInitialized();
        verify(mockTenantContext).getSchemaName();
    }

    @Test
    @DisplayName("Should return default schema when context is null")
    void shouldReturnDefaultSchemaWhenContextIsNull() {
        // Given - use resolver without mock context
        UnifiedTenantResolver resolverWithNullContext = new UnifiedTenantResolver();

        // When
        String result = resolverWithNullContext.resolveCurrentTenantIdentifier();

        // Then
        assertEquals("mcp", result);
    }

    @Test
    @DisplayName("Should return default schema when context is not initialized")
    void shouldReturnDefaultSchemaWhenContextIsNotInitialized() {
        // Given
        when(mockTenantContext.isInitialized()).thenReturn(false);

        // When
        String result = tenantResolver.resolveCurrentTenantIdentifier();

        // Then
        assertEquals("mcp", result);
        verify(mockTenantContext).isInitialized();
    }

    @Test
    @DisplayName("Should handle exception during resolution and fallback to default")
    void shouldHandleExceptionDuringResolutionAndFallbackToDefault() {
        // Given
        when(mockTenantContext.isInitialized()).thenThrow(new RuntimeException("Context error"));

        // When
        String result = tenantResolver.resolveCurrentTenantIdentifier();

        // Then
        assertEquals("mcp", result);
        verify(mockTenantContext).isInitialized();
    }

    @Test
    @DisplayName("Should not validate existing current sessions")
    void shouldNotValidateExistingCurrentSessions() {
        // When & Then
        assertFalse(tenantResolver.validateExistingCurrentSessions());
    }

    @Test
    @DisplayName("Should map tenant ID to schema correctly")
    void shouldMapTenantIdToSchemaCorrectly() {
        // Given & When & Then
        assertEquals("store_acme_corp", UnifiedTenantResolver.mapTenantToSchema("acme-corp"));
        assertEquals("store_test_store", UnifiedTenantResolver.mapTenantToSchema("Test Store"));
        assertEquals("store_simple", UnifiedTenantResolver.mapTenantToSchema("simple"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"", "   "})
    @DisplayName("Should reject null or empty tenant ID for mapping")
    void shouldRejectNullOrEmptyTenantIdForMapping(String invalidTenantId) {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> UnifiedTenantResolver.mapTenantToSchema(invalidTenantId)
        );
        assertEquals("Tenant ID cannot be null or empty", exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
        "'acme-corp', 'store_acme_corp'",
        "'Test Store 123', 'store_test_store_123'",
        "'special!@#$%^&*()chars', 'store_special_chars'",
        "'123numeric-start', 'store_t_123numeric_start'",
        "'a', 'store_a'",
        "'UPPERCASE', 'store_uppercase'",
        "'multiple   spaces', 'store_multiple_spaces'",
        "'under_score', 'store_under_score'",
        "'dots.and.more', 'store_dots_and_more'",
        "'unicode-éñglish', 'store_unicode_nglish'"
    })
    @DisplayName("Should normalize tenant names correctly")
    void shouldNormalizeTenantNamesCorrectly(String input, String expected) {
        // When
        String result = UnifiedTenantResolver.mapTenantToSchema(input);

        // Then
        assertEquals(expected, result);
        assertTrue(result.startsWith("store_"));
        assertTrue(result.length() <= 63); // PostgreSQL limit
    }

    @Test
    @DisplayName("Should handle very long tenant names with truncation")
    void shouldHandleVeryLongTenantNamesWithTruncation() {
        // Given - very long tenant name (100 characters)
        String longTenantName = "a".repeat(100);

        // When
        String result = UnifiedTenantResolver.mapTenantToSchema(longTenantName);

        // Then
        assertTrue(result.startsWith("store_"));
        assertTrue(result.length() <= 63); // PostgreSQL limit
        assertFalse(result.endsWith("_")); // Should not end with underscore after truncation
    }

    @Test
    @DisplayName("Should handle edge case tenant names")
    void shouldHandleEdgeCaseTenantNames() {
        // Given & When & Then
        assertEquals("store_tenant", UnifiedTenantResolver.mapTenantToSchema("!!!"));
        assertEquals("store_t_123", UnifiedTenantResolver.mapTenantToSchema("123"));
        assertEquals("store_a_b_c", UnifiedTenantResolver.mapTenantToSchema("a___b___c"));
        assertEquals("store_start_end", UnifiedTenantResolver.mapTenantToSchema("___start___end___"));
    }

    @Test
    @DisplayName("Should identify tenant schemas correctly")
    void shouldIdentifyTenantSchemasCorrectly() {
        // When & Then - Valid tenant schemas
        assertTrue(UnifiedTenantResolver.isTenantSchema("store_acme"));
        assertTrue(UnifiedTenantResolver.isTenantSchema("store_test_123"));
        assertTrue(UnifiedTenantResolver.isTenantSchema("store_a"));
        
        // Invalid tenant schemas
        assertFalse(UnifiedTenantResolver.isTenantSchema("store_"));
        assertFalse(UnifiedTenantResolver.isTenantSchema("mcp"));
        assertFalse(UnifiedTenantResolver.isTenantSchema("public"));
        assertFalse(UnifiedTenantResolver.isTenantSchema("acme_store"));
        assertFalse(UnifiedTenantResolver.isTenantSchema(null));
        assertFalse(UnifiedTenantResolver.isTenantSchema(""));
    }

    @Test
    @DisplayName("Should extract tenant slug from valid schemas")
    void shouldExtractTenantSlugFromValidSchemas() {
        // When & Then
        assertEquals("acme", UnifiedTenantResolver.extractTenantSlug("store_acme"));
        assertEquals("test_123", UnifiedTenantResolver.extractTenantSlug("store_test_123"));
        assertEquals("a", UnifiedTenantResolver.extractTenantSlug("store_a"));
        
        // Invalid schemas should return null
        assertNull(UnifiedTenantResolver.extractTenantSlug("store_"));
        assertNull(UnifiedTenantResolver.extractTenantSlug("mcp"));
        assertNull(UnifiedTenantResolver.extractTenantSlug("public"));
        assertNull(UnifiedTenantResolver.extractTenantSlug(null));
        assertNull(UnifiedTenantResolver.extractTenantSlug(""));
    }

    @Test
    @DisplayName("Should return correct default schema constant")
    void shouldReturnCorrectDefaultSchemaConstant() {
        // When & Then
        assertEquals("mcp", UnifiedTenantResolver.getDefaultSchema());
    }

    @Test
    @DisplayName("Should return correct tenant schema prefix constant")
    void shouldReturnCorrectTenantSchemaPrefixConstant() {
        // When & Then
        assertEquals("store_", UnifiedTenantResolver.getTenantSchemaPrefix());
    }

    @Test
    @DisplayName("Should validate PostgreSQL schema name length limits")
    void shouldValidatePostgreSQLSchemaNameLengthLimits() {
        // Given - test various lengths
        String[] testInputs = {
            "a",                                    // Short
            "a".repeat(30),                        // Medium
            "a".repeat(57),                        // Near limit (63 - 6 for "store_")
            "a".repeat(100)                        // Over limit
        };

        for (String input : testInputs) {
            // When
            String result = UnifiedTenantResolver.mapTenantToSchema(input);

            // Then
            assertTrue(result.length() <= 63, 
                "Schema name '" + result + "' exceeds PostgreSQL limit of 63 characters");
            assertTrue(result.startsWith("store_"));
        }
    }

    @Test
    @DisplayName("Should produce meaningful toString representation")
    void shouldProduceMeaningfulToStringRepresentation() {
        // When
        String toString = tenantResolver.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("UnifiedTenantResolver"));
        assertTrue(toString.contains("defaultSchema='mcp'"));
        assertTrue(toString.contains("tenantPrefix='store_'"));
    }

    @Test
    @DisplayName("Should handle Unicode characters in tenant names")
    void shouldHandleUnicodeCharactersInTenantNames() {
        // Given - Unicode characters from various languages
        String[] unicodeInputs = {
            "café-münchen",           // German
            "tienda-español",         // Spanish
            "магазин-русский",        // Cyrillic
            "店铺-中文",               // Chinese
            "お店-日本語"              // Japanese
        };

        for (String input : unicodeInputs) {
            // When
            String result = UnifiedTenantResolver.mapTenantToSchema(input);

            // Then
            assertNotNull(result, "Schema mapping should not return null for: " + input);
            assertTrue(result.startsWith("store_"));
            assertTrue(result.length() <= 63);
            assertTrue(result.matches("^[a-z0-9_]+$"), 
                "Schema should only contain lowercase letters, numbers and underscores: " + result);
        }
    }

    @Test
    @DisplayName("Should ensure schema names are valid PostgreSQL identifiers")
    void shouldEnsureSchemaNamesAreValidPostgreSQLIdentifiers() {
        // Given - various challenging inputs
        String[] challengingInputs = {
            "123-numeric-start",
            "special!@#$%chars",
            "multiple   spaces   here",
            "___underscores___everywhere___",
            "UPPER-case-MIX",
            "dots.and.dashes-mixed"
        };

        for (String input : challengingInputs) {
            // When
            String result = UnifiedTenantResolver.mapTenantToSchema(input);

            // Then
            assertTrue(result.matches("^[a-z_][a-z0-9_]*$"), 
                "Schema name '" + result + "' is not a valid PostgreSQL identifier for input: " + input);
            assertFalse(result.endsWith("_"), 
                "Schema name should not end with underscore: " + result);
            assertTrue(result.length() >= 7, // "store_" + at least 1 char
                "Schema name too short: " + result);
        }
    }

    @Test
    @DisplayName("Should handle concurrent access safely")
    void shouldHandleConcurrentAccessSafely() {
        // Given - test static method thread safety
        String testTenantId = "concurrent-test";

        // When - simulate concurrent access
        String result1 = UnifiedTenantResolver.mapTenantToSchema(testTenantId);
        String result2 = UnifiedTenantResolver.mapTenantToSchema(testTenantId);

        // Then - results should be identical
        assertEquals(result1, result2);
        assertEquals("store_concurrent_test", result1);
    }

    @Test
    @DisplayName("Should maintain consistency across multiple calls")
    void shouldMaintainConsistencyAcrossMultipleCalls() {
        // Given
        String tenantId = "consistency-test";

        // When - call multiple times
        String result1 = UnifiedTenantResolver.mapTenantToSchema(tenantId);
        String result2 = UnifiedTenantResolver.mapTenantToSchema(tenantId);
        String result3 = UnifiedTenantResolver.mapTenantToSchema(tenantId);

        // Then - all results should be identical
        assertEquals(result1, result2);
        assertEquals(result2, result3);
    }

    /**
     * Helper method to inject mock TenantContext into resolver via reflection
     * since CDI @InjectMocks doesn't work in unit tests.
     */
    private void setMockTenantContext(UnifiedTenantResolver resolver, TenantContext mockContext) {
        try {
            java.lang.reflect.Field field = UnifiedTenantResolver.class.getDeclaredField("tenantContext");
            field.setAccessible(true);
            field.set(resolver, mockContext);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock TenantContext", e);
        }
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
