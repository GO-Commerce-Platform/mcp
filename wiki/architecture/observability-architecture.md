# GO-Commerce MCP Server Observability & Monitoring Architecture

## Overview

This document outlines the comprehensive observability and monitoring architecture for the GO-Commerce MCP service. The architecture ensures complete visibility into the service's health, performance, and behavior through distributed tracing, metrics collection, structured logging, and alerting systems.

## 1. Distributed Tracing

### 1.1 OpenTelemetry Configuration

```properties path=null start=null
# OpenTelemetry configuration
quarkus.opentelemetry.enabled=true
quarkus.opentelemetry.tracer.enabled=true
quarkus.opentelemetry.tracer.exporter.otlp.endpoint=http://jaeger:4317
quarkus.opentelemetry.tracer.sampler=ratio
quarkus.opentelemetry.tracer.sampler.ratio=1.0
quarkus.opentelemetry.tracer.resource.attributes."service.name"=mcp-server
quarkus.opentelemetry.propagators=tracecontext,baggage,b3multi

# Span processors configuration
quarkus.opentelemetry.tracer.span-processor.batch.enabled=true
quarkus.opentelemetry.tracer.span-processor.batch.max-queue-size=2048
quarkus.opentelemetry.tracer.span-processor.batch.max-export-batch-size=512
quarkus.opentelemetry.tracer.span-processor.batch.schedule-delay=5s
```

### 1.2 Trace Context Propagation

```java path=null start=null
@ApplicationScoped
public class TraceContextManager {
    @Inject
    Tracer tracer;
    
    public <T> T traceOperation(String operationName, Supplier<T> operation) {
        Span span = tracer.spanBuilder(operationName)
            .setSpanKind(SpanKind.INTERNAL)
            .startSpan();
            
        try (Scope scope = span.makeCurrent()) {
            // Add common attributes
            span.setAttribute("tenant.id", TenantContext.getCurrentTenant());
            span.setAttribute("operation.type", operationName);
            
            // Execute operation
            return operation.get();
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
```

### 1.3 Custom Span Attributes

```java path=null start=null
@ApplicationScoped
public class SpanAttributeEnricher {
    private static final AttributeKey<String> TENANT_ID = 
        AttributeKey.stringKey("tenant.id");
    private static final AttributeKey<String> CONTEXT_ID = 
        AttributeKey.stringKey("context.id");
    
    public void enrichSpan(Span span, String tenantId, UUID contextId) {
        span.setAttribute(TENANT_ID, tenantId);
        span.setAttribute(CONTEXT_ID, contextId.toString());
        span.setAttribute("service.version", getServiceVersion());
        span.setAttribute("deployment.environment", getEnvironment());
    }
}
```

## 2. Metrics Collection

### 2.1 Micrometer Configuration

```properties path=null start=null
# Micrometer configuration
quarkus.micrometer.enabled=true
quarkus.micrometer.export.prometheus.enabled=true
quarkus.micrometer.export.prometheus.path=/metrics
quarkus.micrometer.binder.http-client.enabled=true
quarkus.micrometer.binder.http-server.enabled=true
quarkus.micrometer.binder.jvm=true
quarkus.micrometer.binder.system=true
quarkus.micrometer.binder.vertx.enabled=true
```

### 2.2 Custom Metrics

```java path=null start=null
@ApplicationScoped
public class MCPMetrics {
    private final MeterRegistry registry;
    
    private final Counter contextCreations;
    private final Counter dataRequests;
    private final Timer queryExecution;
    private final DistributionSummary responseSize;
    
    public MCPMetrics(MeterRegistry registry) {
        this.registry = registry;
        
        // Initialize metrics
        this.contextCreations = Counter.builder("mcp.context.creations")
            .description("Total number of contexts created")
            .register(registry);
            
        this.dataRequests = Counter.builder("mcp.data.requests")
            .description("Total number of data requests")
            .register(registry);
            
        this.queryExecution = Timer.builder("mcp.query.execution")
            .description("Query execution time")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
            
        this.responseSize = DistributionSummary.builder("mcp.response.size")
            .description("Response size in bytes")
            .baseUnit("bytes")
            .register(registry);
    }
    
    public void recordContextCreation(String tenantId) {
        contextCreations.increment();
        
        // Record tenant-specific metrics
        Counter.builder("mcp.context.creations.tenant")
            .tag("tenant", tenantId)
            .register(registry)
            .increment();
    }
    
    public Timer.Sample startQueryTimer() {
        return Timer.start(registry);
    }
    
    public void stopQueryTimer(Timer.Sample sample, String queryType) {
        sample.stop(Timer.builder("mcp.query.execution")
            .tag("type", queryType)
            .register(registry));
    }
}
```

### 2.3 Resource Metrics

```java path=null start=null
@ApplicationScoped
public class ResourceMetricsCollector {
    @Inject
    MeterRegistry registry;
    
    @Scheduled(every = "1m")
    void collectResourceMetrics() {
        // JVM metrics
        recordJvmMetrics();
        
        // Connection pool metrics
        recordConnectionPoolMetrics();
        
        // Cache metrics
        recordCacheMetrics();
        
        // Custom resource metrics
        recordCustomMetrics();
    }
    
    private void recordJvmMetrics() {
        Gauge.builder("jvm.memory.used", Runtime.getRuntime(), 
            runtime -> runtime.totalMemory() - runtime.freeMemory())
            .description("JVM memory used")
            .baseUnit("bytes")
            .register(registry);
    }
}
```

## 3. Structured Logging

### 3.1 Logging Configuration

```properties path=null start=null
# Logging configuration
quarkus.log.category."dev.tiodati.saas.gocommerce".level=INFO
quarkus.log.console.json=true
quarkus.log.console.format=%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n

# File appender
quarkus.log.file.enable=true
quarkus.log.file.path=/var/log/mcp-server.log
quarkus.log.file.format=%d{yyyy-MM-dd HH:mm:ss,SSS} %h %N[%i] %-5p [%c{1.}] (%t) %s%e%n
quarkus.log.file.rotation.max-file-size=10M
quarkus.log.file.rotation.max-backup-index=5

# JSON logging
quarkus.log.handler.gelf.enabled=true
quarkus.log.handler.gelf.host=logstash
quarkus.log.handler.gelf.port=12201
quarkus.log.handler.gelf.include-full-mdc=true
```

### 3.2 Structured Logger Implementation

```java path=null start=null
@ApplicationScoped
public class StructuredLogger {
    private static final Logger logger = 
        LoggerFactory.getLogger(StructuredLogger.class);
    
    public void logOperationStart(
            String operation,
            String tenantId,
            Map<String, Object> context) {
        MDC.put("operation", operation);
        MDC.put("tenant_id", tenantId);
        MDC.put("trace_id", getTraceId());
        
        try {
            logger.info("Operation started: {}", 
                createLogMessage(operation, context));
        } finally {
            MDC.clear();
        }
    }
    
    public void logOperationEnd(
            String operation,
            String tenantId,
            Duration duration,
            Map<String, Object> context) {
        MDC.put("operation", operation);
        MDC.put("tenant_id", tenantId);
        MDC.put("duration_ms", String.valueOf(duration.toMillis()));
        
        try {
            logger.info("Operation completed: {}", 
                createLogMessage(operation, context));
        } finally {
            MDC.clear();
        }
    }
    
    private String createLogMessage(String operation, Map<String, Object> context) {
        return JsonObject.create()
            .put("operation", operation)
            .put("timestamp", Instant.now().toString())
            .put("context", context)
            .toString();
    }
}
```

## 4. Health Checks

### 4.1 Health Check Implementation

```java path=null start=null
@ApplicationScoped
public class MCPHealthCheck implements HealthCheck {
    @Inject
    EntityManager em;
    
    @Inject
    RedisClient redis;
    
    @Inject
    KeycloakClient keycloak;
    
    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("mcp-health");
        
        // Check database connectivity
        try {
            em.createNativeQuery("SELECT 1").getSingleResult();
            builder.withData("database", "UP");
        } catch (Exception e) {
            builder.down().withData("database", "DOWN");
        }
        
        // Check Redis connectivity
        try {
            redis.ping();
            builder.withData("redis", "UP");
        } catch (Exception e) {
            builder.down().withData("redis", "DOWN");
        }
        
        // Check Keycloak connectivity
        try {
            keycloak.getServerInfo();
            builder.withData("keycloak", "UP");
        } catch (Exception e) {
            builder.down().withData("keycloak", "DOWN");
        }
        
        return builder.build();
    }
}
```

### 4.2 Readiness Probe

```java path=null start=null
@ApplicationScoped
public class MCPReadinessCheck implements HealthCheck {
    @Inject
    ConnectionPoolManager poolManager;
    
    @Inject
    CacheManager cacheManager;
    
    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("mcp-readiness");
        
        // Check connection pool
        if (!poolManager.isReady()) {
            return builder.down()
                .withData("connection_pool", "NOT_READY")
                .build();
        }
        
        // Check cache warmup
        if (!cacheManager.isWarmedUp()) {
            return builder.down()
                .withData("cache", "NOT_WARMED")
                .build();
        }
        
        return builder.up().build();
    }
}
```

## 5. Alerting Configuration

### 5.1 Alert Rules

```yaml path=null start=null
alerts:
  # Service health alerts
  service_health:
    conditions:
      - metric: "health_check_status"
        threshold: 0
        duration: "5m"
        severity: critical
    
  # Performance alerts
  performance:
    conditions:
      - metric: "http_server_requests_seconds_max"
        threshold: 2
        duration: "5m"
        severity: warning
      - metric: "http_server_requests_seconds_p99"
        threshold: 1
        duration: "5m"
        severity: warning
    
  # Resource utilization alerts
  resources:
    conditions:
      - metric: "system_cpu_usage"
        threshold: 80
        duration: "5m"
        severity: warning
      - metric: "jvm_memory_used_bytes"
        threshold: 85
        duration: "5m"
        severity: warning
    
  # Error rate alerts
  errors:
    conditions:
      - metric: "http_server_requests_error_count"
        threshold: 10
        duration: "5m"
        severity: warning
```

### 5.2 Alert Manager Configuration

```yaml path=null start=null
alertmanager:
  route:
    receiver: 'team-mcp'
    group_by: ['alertname', 'tenant']
    group_wait: 30s
    group_interval: 5m
    repeat_interval: 4h
    
  receivers:
    - name: 'team-mcp'
      slack_configs:
        - channel: '#mcp-alerts'
          username: 'AlertManager'
          send_resolved: true
          title: '{{ template "slack.title" . }}'
          text: '{{ template "slack.text" . }}'
      
      email_configs:
        - to: 'mcp-team@gocommerce.dev'
          send_resolved: true
```

## 6. Operational Dashboards

### 6.1 Service Overview Dashboard

```json path=null start=null
{
  "dashboard": {
    "title": "MCP Service Overview",
    "panels": [
      {
        "title": "Request Rate",
        "type": "graph",
        "metrics": [
          "rate(http_server_requests_seconds_count[5m])"
        ]
      },
      {
        "title": "Response Time",
        "type": "graph",
        "metrics": [
          "http_server_requests_seconds_p95",
          "http_server_requests_seconds_p99"
        ]
      },
      {
        "title": "Error Rate",
        "type": "graph",
        "metrics": [
          "rate(http_server_requests_error_count[5m])"
        ]
      }
    ]
  }
}
```

### 6.2 Tenant Performance Dashboard

```json path=null start=null
{
  "dashboard": {
    "title": "Tenant Performance",
    "panels": [
      {
        "title": "Requests by Tenant",
        "type": "graph",
        "metrics": [
          "sum by (tenant) (rate(mcp_requests_total[5m]))"
        ]
      },
      {
        "title": "Response Time by Tenant",
        "type": "heatmap",
        "metrics": [
          "rate(mcp_request_duration_seconds_bucket[5m])"
        ]
      },
      {
        "title": "Error Rate by Tenant",
        "type": "graph",
        "metrics": [
          "sum by (tenant) (rate(mcp_errors_total[5m]))"
        ]
      }
    ]
  }
}
```

## 7. Implementation Guidelines

### 7.1 Observability Best Practices

1. **Consistent Correlation IDs**
   - Use trace ID in all logs
   - Propagate context across service boundaries
   - Include tenant context in spans

2. **Metric Naming Conventions**
   - Use lowercase with underscores
   - Include service name prefix
   - Use consistent units
   - Add appropriate tags

3. **Log Level Guidelines**
   - ERROR: Service failures
   - WARN: Potential issues
   - INFO: Normal operations
   - DEBUG: Detailed troubleshooting

4. **Health Check Implementation**
   - Fast execution (< 1s)
   - Minimal resource usage
   - Clear failure conditions
   - Appropriate intervals

### 7.2 Monitoring Guidelines

1. **Alert Configuration**
   - Define clear thresholds
   - Include runbooks
   - Set appropriate severity
   - Avoid alert fatigue

2. **Dashboard Organization**
   - Group related metrics
   - Use consistent time ranges
   - Include legends
   - Add documentation

3. **Performance Monitoring**
   - Track key indicators
   - Monitor resource usage
   - Collect business metrics
   - Track SLA compliance

4. **Security Monitoring**
   - Audit access logs
   - Track authentication failures
   - Monitor suspicious patterns
   - Log security events

## 8. Operational Considerations

### 8.1 Log Management

1. **Retention Policy**
   - Application logs: 30 days
   - Audit logs: 1 year
   - Metrics: 6 months
   - Traces: 7 days

2. **Storage Requirements**
   - Calculate based on volume
   - Consider compression
   - Plan for growth
   - Monitor usage

### 8.2 Scaling Considerations

1. **Metrics Collection**
   - Cardinality limits
   - Storage requirements
   - Query performance
   - Retention periods

2. **Log Aggregation**
   - Throughput capacity
   - Buffer sizing
   - Backup strategy
   - Archival process

### 8.3 Troubleshooting Procedures

1. **Service Issues**
   - Check health endpoints
   - Review recent changes
   - Analyze metrics
   - Investigate logs

2. **Performance Issues**
   - Review response times
   - Check resource usage
   - Analyze database metrics
   - Investigate traces

// Copilot: This file may have been generated or refactored by GitHub Copilot.
