package dev.tiodati.saas.gocommerce.mcp;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Main application class for the GO-Commerce Model Context Protocol (MCP) service.
 * 
 * This service acts as a secure bridge between AI agents and the GO-Commerce ecosystem,
 * providing tenant-aware access to product catalogs, inventory, and order processing
 * through RESTful APIs and real-time event streaming.
 * 
 * Key Features:
 * - Multi-tenant data isolation using PostgreSQL schema-per-tenant
 * - JWT-based authentication and role-based access control via Keycloak
 * - Real-time updates through Kafka event streaming
 * - Redis-based distributed caching for performance optimization
 * - OpenTelemetry observability with Prometheus metrics
 * 
 * Architecture:
 * - Framework: Quarkus 3.x with Java 21
 * - Database: PostgreSQL with schema-based multi-tenancy
 * - Security: Keycloak OAuth2/OIDC
 * - Messaging: Apache Kafka
 * - Caching: Redis
 * 
 * @author GO-Commerce Development Team
 * @version 1.0.0-SNAPSHOT
 * @since 1.0.0
 */
@QuarkusMain
@ApplicationScoped
public class MCPServerApplication implements QuarkusApplication {

    /**
     * Main entry point for the MCP service.
     * 
     * This method initializes the Quarkus runtime and starts the application.
     * The service will be available at the configured HTTP port (default: 8080).
     * 
     * @param args Command-line arguments passed to the application
     */
    public static void main(String... args) {
        Log.info("Starting GO-Commerce MCP Server...");
        Quarkus.run(MCPServerApplication.class, args);
    }

    /**
     * Application startup hook called by Quarkus runtime.
     * 
     * This method is executed after all CDI beans are initialized and 
     * before the HTTP server starts accepting requests. It's the perfect
     * place to perform any application-specific initialization.
     * 
     * @param args Command-line arguments
     * @return Exit code (0 for success, non-zero for error)
     */
    @Override
    public int run(String... args) {
        Log.info("GO-Commerce MCP Server started successfully");
        Log.info("Service ready to accept AI agent requests");
        Log.info("Health endpoint available at: /q/health");
        Log.info("Metrics endpoint available at: /q/metrics");
        Log.info("OpenAPI documentation available at: /q/swagger-ui (dev mode only)");
        
        // Keep the application running
        Quarkus.waitForExit();
        return 0;
    }

    /**
     * Application shutdown hook called when the service is stopping.
     * 
     * This method is called during graceful shutdown and provides an opportunity
     * to clean up resources, flush caches, or perform any final operations.
     */
    public void onStop() {
        Log.info("GO-Commerce MCP Server shutdown initiated");
        Log.info("Performing graceful cleanup...");
        
        // Future: Add cleanup logic here
        // - Close database connections
        // - Flush Redis cache
        // - Complete in-flight Kafka messages
        
        Log.info("GO-Commerce MCP Server stopped");
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
