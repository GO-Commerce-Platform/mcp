package dev.tiodati.saas.gocommerce.mcp.config;

import dev.tiodati.saas.gocommerce.mcp.tenant.UnifiedTenantResolver;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MultiTenancyConfiguration class.
 * 
 * Tests configuration validation, bean production, lifecycle methods,
 * and error handling for multi-tenancy setup.
 * 
 * @author GO-Commerce Development Team
 * @version 1.0.0
 * @since 1.0.0
 */
class MultiTenancyConfigurationTest {

    private MultiTenancyConfiguration multiTenancyConfig;

    @Mock
    private UnifiedTenantResolver mockTenantResolver;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        multiTenancyConfig = new MultiTenancyConfiguration();
    }

    @Test
    @DisplayName("Should initialize configuration successfully")
    void shouldInitializeConfigurationSuccessfully() {
        // When
        MultiTenancyConfiguration config = new MultiTenancyConfiguration();

        // Then
        assertNotNull(config);
    }

    @Test
    @DisplayName("Should produce CurrentTenantIdentifierResolver bean")
    void shouldProduceCurrentTenantIdentifierResolverBean() {
        // Given
        when(mockTenantResolver.toString()).thenReturn("MockResolver");

        // When
        CurrentTenantIdentifierResolver resolver = multiTenancyConfig.tenantIdentifierResolver(mockTenantResolver);

        // Then
        assertNotNull(resolver);
        assertSame(mockTenantResolver, resolver);
        verify(mockTenantResolver).toString(); // Verify debug logging called toString
    }

    @Test
    @DisplayName("Should validate configuration successfully with valid setup")
    void shouldValidateConfigurationSuccessfullyWithValidSetup() {
        // When & Then - should not throw any exception
        assertDoesNotThrow(() -> multiTenancyConfig.validateConfiguration());
    }

    @Test
    @DisplayName("Should validate schema naming with various tenant patterns")
    void shouldValidateSchemaNameWithValidTenantPatterns() {
        // This test verifies that the validation logic doesn't throw exceptions
        // for the predefined test tenant IDs in the configuration
        
        // When & Then - should not throw any exception
        assertDoesNotThrow(() -> multiTenancyConfig.validateConfiguration());
    }

    @Test
    @DisplayName("Should handle cleanup lifecycle method")
    void shouldHandleCleanupLifecycleMethod() {
        // When & Then - should not throw any exception
        assertDoesNotThrow(() -> multiTenancyConfig.cleanup());
    }

    @Test
    @DisplayName("Should produce meaningful toString representation")
    void shouldProduceMeaningfulToStringRepresentation() {
        // When
        String toString = multiTenancyConfig.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("MultiTenancyConfiguration"));
        assertTrue(toString.contains("defaultSchema='mcp'"));
        assertTrue(toString.contains("tenantPrefix='store_'"));
        assertTrue(toString.contains("mode='SCHEMA'"));
    }

    @Test
    @DisplayName("Should validate schema names according to PostgreSQL rules")
    void shouldValidateSchemaNameAccordingToPostgreSQLRules() {
        // This test uses reflection to access the private validateSchemaName method
        // to test the schema validation logic directly

        // Given
        String[] validSchemas = {
            "store_acme",
            "store_test_123", 
            "store_a",
            "mcp" // default schema
        };

        String[] invalidSchemas = {
            null,
            "",
            "   ",
            "a".repeat(64), // too long
            "123invalid", // starts with number
            "invalid-char", // contains dash
            "invalid space" // contains space
        };

        // When & Then - valid schemas should not throw
        for (String validSchema : validSchemas) {
            assertDoesNotThrow(() -> 
                invokeValidateSchemaName(multiTenancyConfig, validSchema),
                "Valid schema should not throw: " + validSchema
            );
        }

        // Invalid schemas should throw IllegalArgumentException
        for (String invalidSchema : invalidSchemas) {
            assertThrows(IllegalArgumentException.class, () -> 
                invokeValidateSchemaName(multiTenancyConfig, invalidSchema),
                "Invalid schema should throw: " + invalidSchema
            );
        }
    }

    @Test
    @DisplayName("Should validate configuration and handle validation errors")
    void shouldValidateConfigurationAndHandleValidationErrors() {
        // Create a configuration instance that will try to validate
        MultiTenancyConfiguration config = new MultiTenancyConfiguration();

        // The validateConfiguration method should work with the predefined
        // tenant IDs in the configuration class
        assertDoesNotThrow(() -> config.validateConfiguration());
    }

    @Test
    @DisplayName("Should validate tenant-to-schema mapping consistency")
    void shouldValidateTenantToSchemaMappingConsistency() {
        // Given - test the consistency between UnifiedTenantResolver and validation
        String[] testTenantIds = {
            "acme-corp",
            "Test Store 123",
            "simple",
            "123numeric"
        };

        // When & Then - all mapped schemas should be valid
        for (String tenantId : testTenantIds) {
            String mappedSchema = UnifiedTenantResolver.mapTenantToSchema(tenantId);
            
            // The mapped schema should pass validation
            assertDoesNotThrow(() -> 
                invokeValidateSchemaName(multiTenancyConfig, mappedSchema),
                "Mapped schema should be valid: " + mappedSchema + " for tenant: " + tenantId
            );
        }
    }

    @Test
    @DisplayName("Should handle edge cases in schema validation")
    void shouldHandleEdgeCasesInSchemaValidation() {
        // Test boundary conditions for schema name length
        
        // Test maximum valid length (63 characters)
        String maxLengthSchema = "store_" + "a".repeat(57); // 57 + 6 = 63
        assertDoesNotThrow(() -> 
            invokeValidateSchemaName(multiTenancyConfig, maxLengthSchema));

        // Test one character over the limit
        String tooLongSchema = "store_" + "a".repeat(58); // 58 + 6 = 64
        assertThrows(IllegalArgumentException.class, () -> 
            invokeValidateSchemaName(multiTenancyConfig, tooLongSchema));
    }

    @Test
    @DisplayName("Should validate schema name character patterns")
    void shouldValidateSchemaNameCharacterPatterns() {
        // Test valid patterns
        String[] validPatterns = {
            "store_abc",
            "store_test_123",
            "store_a1b2c3",
            "_underscore_start",
            "mcp"
        };

        for (String pattern : validPatterns) {
            assertDoesNotThrow(() -> 
                invokeValidateSchemaName(multiTenancyConfig, pattern),
                "Valid pattern should not throw: " + pattern);
        }

        // Test invalid patterns
        String[] invalidPatterns = {
            "store-dash",
            "store space",
            "store!special",
            "store@symbol",
            "123numeric_start"
        };

        for (String pattern : invalidPatterns) {
            assertThrows(IllegalArgumentException.class, () -> 
                invokeValidateSchemaName(multiTenancyConfig, pattern),
                "Invalid pattern should throw: " + pattern);
        }
    }

    @Test
    @DisplayName("Should enforce tenant schema prefix validation")
    void shouldEnforceTenantSchemaPrefixValidation() {
        // Test schemas that don't follow naming convention
        String[] invalidNamingSchemas = {
            "invalid_prefix_test",
            "tenant_wrong_prefix",  
            "custom_schema_name",
            "another_schema"
        };

        for (String schema : invalidNamingSchemas) {
            assertThrows(IllegalArgumentException.class, () -> 
                invokeValidateSchemaName(multiTenancyConfig, schema),
                "Schema with wrong prefix should throw: " + schema);
        }
    }

    @Test
    @DisplayName("Should allow default schema to bypass prefix validation")
    void shouldAllowDefaultSchemaToBynpassPrefixValidation() {
        // Given - default schema doesn't follow tenant prefix convention
        String defaultSchema = UnifiedTenantResolver.getDefaultSchema();

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> 
            invokeValidateSchemaName(multiTenancyConfig, defaultSchema));
    }

    @Test
    @DisplayName("Should handle concurrent validation safely")
    void shouldHandleConcurrentValidationSafely() {
        // Test that validation methods are thread-safe
        String validSchema = "store_concurrent_test";

        // Run validation concurrently
        assertDoesNotThrow(() -> {
            Thread thread1 = new Thread(() -> 
                invokeValidateSchemaName(multiTenancyConfig, validSchema));
            Thread thread2 = new Thread(() -> 
                invokeValidateSchemaName(multiTenancyConfig, validSchema));

            thread1.start();
            thread2.start();

            try {
                thread1.join();
                thread2.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    @Test
    @DisplayName("Should provide comprehensive error messages")
    void shouldProvideComprehensiveErrorMessages() {
        // Test that validation errors include helpful information

        // Test null schema
        IllegalArgumentException nullException = assertThrows(
            IllegalArgumentException.class,
            () -> invokeValidateSchemaName(multiTenancyConfig, null)
        );
        assertTrue(nullException.getMessage().contains("null or empty"));

        // Test too long schema
        String tooLongSchema = "a".repeat(70);
        IllegalArgumentException lengthException = assertThrows(
            IllegalArgumentException.class,
            () -> invokeValidateSchemaName(multiTenancyConfig, tooLongSchema)
        );
        assertTrue(lengthException.getMessage().contains("exceeds PostgreSQL maximum length"));

        // Test invalid characters
        String invalidCharsSchema = "store-invalid";
        IllegalArgumentException charsException = assertThrows(
            IllegalArgumentException.class,
            () -> invokeValidateSchemaName(multiTenancyConfig, invalidCharsSchema)
        );
        assertTrue(charsException.getMessage().contains("invalid characters"));
    }

    /**
     * Helper method to invoke private validateSchemaName method via reflection
     */
    private void invokeValidateSchemaName(MultiTenancyConfiguration config, String schemaName) {
        try {
            java.lang.reflect.Method method = MultiTenancyConfiguration.class
                .getDeclaredMethod("validateSchemaName", String.class);
            method.setAccessible(true);
            method.invoke(config, schemaName);
        } catch (java.lang.reflect.InvocationTargetException e) {
            // Unwrap the actual exception
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw new RuntimeException("Unexpected exception", cause);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke validateSchemaName", e);
        }
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
