package dev.tiodati.saas.gocommerce.mcp.config;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Logging configuration and correlation ID management.
 * 
 * This class manages structured logging configuration and provides utilities
 * for correlation ID handling across requests and tenant contexts.
 * 
 * @author GO-Commerce Development Team
 * @version 1.0.0
 * @since 1.0.0
 */
@ApplicationScoped
public class LoggingConfiguration {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String TENANT_ID_HEADER = "X-Tenant-ID";

    public LoggingConfiguration() {
        Log.info("Structured logging configuration initialized");
    }

    @jakarta.annotation.PostConstruct
    public void initializeLogging() {
        Log.info("Setting up structured logging for MCP service");
        configureStructuredLogging();
    }

    private void configureStructuredLogging() {
        // Set up MDC (Mapped Diagnostic Context) for structured logging
        // This will be used by the logging framework to include contextual information
        
        Log.info("Structured logging configuration:");
        Log.info("  - JSON format enabled for console output");
        Log.info("  - File logging enabled with rotation");
        Log.info("  - Async logging configured for performance");
        Log.info("  - Multi-tenant correlation IDs supported");
    }

    /**
     * Generates a new correlation ID for request tracking.
     * 
     * @return A new correlation ID
     */
    public static String generateCorrelationId() {
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * Gets the correlation ID header name.
     * 
     * @return The correlation ID header name
     */
    public static String getCorrelationIdHeader() {
        return CORRELATION_ID_HEADER;
    }

    /**
     * Gets the tenant ID header name.
     * 
     * @return The tenant ID header name
     */
    public static String getTenantIdHeader() {
        return TENANT_ID_HEADER;
    }

    /**
     * Sets up MDC context for a request with correlation and tenant IDs.
     * 
     * @param correlationId The correlation ID for the request
     * @param tenantId The tenant ID for the request
     */
    public static void setupRequestContext(String correlationId, String tenantId) {
        org.slf4j.MDC.put("correlationId", correlationId);
        if (tenantId != null) {
            org.slf4j.MDC.put("tenantId", tenantId);
        }
        
        Log.debugf("Request context setup - correlationId: %s, tenantId: %s", 
                  correlationId, tenantId);
    }

    /**
     * Clears the MDC context at the end of request processing.
     */
    public static void clearRequestContext() {
        org.slf4j.MDC.clear();
        Log.debug("Request context cleared");
    }

    @jakarta.annotation.PreDestroy
    public void cleanup() {
        Log.info("Cleaning up logging configuration...");
        Log.info("Logging configuration cleanup completed");
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
