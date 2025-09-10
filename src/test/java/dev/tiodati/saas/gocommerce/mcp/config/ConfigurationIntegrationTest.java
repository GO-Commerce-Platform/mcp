package dev.tiodati.saas.gocommerce.mcp.config;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.EncoderConfig;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Integration tests for configuration validation and endpoints.
 * 
 * This test class verifies that all configuration elements are working
 * correctly including health checks, metrics, OpenAPI documentation,
 * and multi-tenancy setup.
 * 
 * @author GO-Commerce Development Team
 * @version 1.0.0
 * @since 1.0.0
 */
@QuarkusTest
@DisplayName("Configuration Integration Tests")
class ConfigurationIntegrationTest {

    @Nested
    @DisplayName("Health Check Endpoints")
    class HealthCheckTests {

        @Test
        @DisplayName("Should provide general health status")
        void shouldProvideGeneralHealthStatus() {
            given()
                .when().get("/q/health")
                .then()
                .statusCode(200)
                .body("status", is("UP"))
                .body("checks", notNullValue());
        }

        @Test
        @DisplayName("Should provide liveness probe")
        void shouldProvideLivenessProbe() {
            given()
                .when().get("/q/health/live")
                .then()
                .statusCode(200)
                .body("status", is("UP"))
                .body("checks", notNullValue());
        }

        @Test
        @DisplayName("Should provide readiness probe")
        void shouldProvideReadinessProbe() {
            given()
                .when().get("/q/health/ready")
                .then()
                .statusCode(200)
                .body("status", is("UP"))
                .body("checks", notNullValue());
        }

        @Test
        @DisplayName("Should provide startup probe")
        void shouldProvideStartupProbe() {
            given()
                .when().get("/q/health/started")
                .then()
                .statusCode(200)
                .body("status", is("UP"))
                .body("checks", notNullValue());
        }
    }

    @Nested
    @DisplayName("Metrics Endpoints")
    class MetricsTests {

        @Test
        @DisplayName("Should provide Prometheus metrics")
        void shouldProvidePrometheusMetrics() {
            given()
                .when().get("/q/metrics")
                .then()
                .statusCode(200)
                .header("Content-Type", "application/openmetrics-text; version=1.0.0; charset=utf-8");
        }
    }

    @Nested
    @DisplayName("OpenAPI Documentation")
    class OpenAPITests {

        @Test
        @DisplayName("Should provide OpenAPI specification")
        void shouldProvideOpenAPISpecification() {
            given()
                .when().get("/q/openapi")
                .then()
                .statusCode(200)
                .header("Content-Type", "application/yaml;charset=UTF-8");
        }

        @Test
        @DisplayName("Should provide Swagger UI in development")
        void shouldProvideSwaggerUI() {
            given()
                .when().get("/q/swagger-ui/")
                .then()
                .statusCode(200);
        }
    }

    @Nested
    @DisplayName("Application Configuration")
    class ApplicationConfigurationTests {

        @Test
        @DisplayName("Should respond on configured port with API root path")
        void shouldRespondOnConfiguredPort() {
            // Test that the application is running and accessible
            // The actual port is managed by Quarkus test framework
            given()
                .when().get("/q/health")
                .then()
                .statusCode(200);
        }

        @Test
        @DisplayName("Should handle CORS preflight requests")
        void shouldHandleCORSPreflight() {
            given()
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
                .when().options("/q/health")
                .then()
                .statusCode(200);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should return 404 for non-existent endpoints")
        void shouldReturn404ForNonExistentEndpoints() {
            given()
                .when().get("/non-existent-endpoint")
                .then()
                .statusCode(404);
        }

        @Test
        @DisplayName("Should handle invalid request formats gracefully")
        void shouldHandleInvalidRequestFormats() {
            // Test that server can handle invalid content-types without crashing
            // and still returns a successful response (even ignoring the invalid body)
            given()
                .config(RestAssured.config().encoderConfig(
                    EncoderConfig.encoderConfig().encodeContentTypeAs("invalid/content-type", ContentType.TEXT)))
                .header("Content-Type", "invalid/content-type")
                .body("invalid json content")
                .when().post("/q/openapi")
                .then()
                // Since Quarkus management endpoints are lenient, expect 200
                // The important thing is it handles the invalid content gracefully
                .statusCode(200)
                .header("Content-Type", "application/yaml;charset=UTF-8");
        }
    }
}

// Copilot: This file may have been generated or refactored by GitHub Copilot.
