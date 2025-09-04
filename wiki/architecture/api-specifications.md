# GO-Commerce MCP Server API & Integration Specifications

## Overview

This document specifies the API design and integration patterns for the GO-Commerce MCP (Model Context Protocol) service. It details the RESTful endpoints, WebSocket connections, event streams, and external service integrations required for the MCP server to function as a bridge between AI systems and the GO-Commerce platform.

## 1. RESTful API Design

### 1.1 API Structure

The API follows a resource-oriented design with the following base structure:

```plaintext path=null start=null
/api/v1/
  ├── context/          # Context management endpoints
  ├── data/            # Data access endpoints
  ├── events/          # Event handling endpoints
  ├── tenants/         # Tenant management endpoints
  └── health/          # Health and monitoring endpoints
```

### 1.2 OpenAPI Specification

```yaml path=null start=null
openapi: 3.0.3
info:
  title: GO-Commerce MCP API
  version: 1.0.0
  description: Model Context Protocol API for GO-Commerce
servers:
  - url: https://mcp.gocommerce.dev/api/v1
    description: Production server
  - url: https://mcp-staging.gocommerce.dev/api/v1
    description: Staging server
security:
  - bearerAuth: []
paths:
  /context:
    post:
      summary: Create a new context for AI interaction
      operationId: createContext
      tags: [Context]
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ContextRequest'
      responses:
        '201':
          description: Context created successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Context'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '403':
          $ref: '#/components/responses/Forbidden'

  /data/{contextId}:
    get:
      summary: Retrieve data for a specific context
      operationId: getData
      tags: [Data]
      parameters:
        - name: contextId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: Data retrieved successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/DataResponse'

components:
  schemas:
    ContextRequest:
      type: object
      required:
        - tenantId
        - scope
      properties:
        tenantId:
          type: string
          format: uuid
        scope:
          type: array
          items:
            type: string
            enum: [products, orders, customers]
        timeRange:
          type: object
          properties:
            start:
              type: string
              format: date-time
            end:
              type: string
              format: date-time

    Context:
      type: object
      required:
        - id
        - tenantId
        - status
      properties:
        id:
          type: string
          format: uuid
        tenantId:
          type: string
          format: uuid
        status:
          type: string
          enum: [ACTIVE, EXPIRED, REVOKED]
        created:
          type: string
          format: date-time
        expires:
          type: string
          format: date-time

  responses:
    Unauthorized:
      description: Authentication required
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'
    Forbidden:
      description: Permission denied
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'

  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
```

## 2. MCP Protocol Implementation

### 2.1 Context Management Endpoints

```java path=null start=null
@Path("/api/v1/context")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ContextResource {
    @Inject
    ContextService contextService;
    
    @POST
    @RolesAllowed({"mcp-client"})
    public Response createContext(ContextRequest request) {
        Context context = contextService.createContext(request);
        return Response.status(Status.CREATED)
            .entity(context)
            .build();
    }
    
    @GET
    @Path("/{contextId}")
    @RolesAllowed({"mcp-client"})
    public Response getContext(@PathParam("contextId") UUID contextId) {
        Context context = contextService.getContext(contextId);
        return Response.ok(context).build();
    }
    
    @DELETE
    @Path("/{contextId}")
    @RolesAllowed({"mcp-client"})
    public Response releaseContext(@PathParam("contextId") UUID contextId) {
        contextService.releaseContext(contextId);
        return Response.noContent().build();
    }
}
```

### 2.2 Data Access Endpoints

```java path=null start=null
@Path("/api/v1/data")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DataResource {
    @Inject
    DataService dataService;
    
    @GET
    @Path("/{contextId}")
    @RolesAllowed({"mcp-client"})
    public Response getData(
            @PathParam("contextId") UUID contextId,
            @QueryParam("query") String query) {
        DataResponse data = dataService.queryData(contextId, query);
        return Response.ok(data).build();
    }
    
    @POST
    @Path("/{contextId}/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RolesAllowed({"mcp-client"})
    public Multi<DataEvent> streamData(
            @PathParam("contextId") UUID contextId,
            StreamRequest request) {
        return dataService.streamData(contextId, request);
    }
}
```

## 3. WebSocket Integration

### 3.1 WebSocket Endpoint Configuration

```java path=null start=null
@ServerEndpoint("/ws/v1/events/{contextId}")
@ApplicationScoped
public class EventWebSocket {
    @Inject
    EventService eventService;
    
    @OnOpen
    public void onOpen(Session session, @PathParam("contextId") String contextId) {
        // Validate context and establish connection
        eventService.registerSession(UUID.fromString(contextId), session);
    }
    
    @OnMessage
    public void onMessage(String message, Session session) {
        // Handle incoming message
        EventMessage event = parseMessage(message);
        eventService.handleEvent(event, session);
    }
    
    @OnClose
    public void onClose(Session session, @PathParam("contextId") String contextId) {
        // Cleanup resources
        eventService.unregisterSession(UUID.fromString(contextId));
    }
}
```

### 3.2 Real-time Event Handling

```java path=null start=null
@ApplicationScoped
public class EventService {
    private final ConcurrentMap<UUID, Session> sessions = new ConcurrentHashMap<>();
    
    public void registerSession(UUID contextId, Session session) {
        sessions.put(contextId, session);
    }
    
    public void broadcastEvent(UUID contextId, EventMessage event) {
        Session session = sessions.get(contextId);
        if (session != null && session.isOpen()) {
            session.getAsyncRemote().sendObject(event,
                result -> handleSendResult(result, contextId));
        }
    }
    
    private void handleSendResult(SendResult result, UUID contextId) {
        if (!result.isOK()) {
            log.error("Failed to send event to context {}: {}",
                contextId, result.getException());
        }
    }
}
```

## 4. Event Stream Integration

### 4.1 Kafka Event Consumers

```java path=null start=null
@ApplicationScoped
public class EventConsumer {
    @Inject
    EventProcessor processor;
    
    @Incoming("data-updates")
    public Uni<Void> consumeDataUpdates(Message<DataUpdateEvent> message) {
        return processor.processDataUpdate(message.getPayload())
            .onItem().transform(result -> {
                message.ack();
                return null;
            })
            .onFailure().recoverWithItem(() -> {
                message.nack(new ProcessingException("Failed to process event"));
                return null;
            });
    }
}
```

### 4.2 Event Publishers

```java path=null start=null
@ApplicationScoped
public class EventPublisher {
    @Inject
    @Channel("context-events")
    Emitter<ContextEvent> eventEmitter;
    
    public Uni<Void> publishContextEvent(ContextEvent event) {
        return Uni.createFrom().emitter(em -> 
            eventEmitter.send(event)
                .whenComplete((success, failure) -> {
                    if (failure != null) {
                        em.fail(failure);
                    } else {
                        em.complete(null);
                    }
                }));
    }
}
```

## 5. External Service Integration

### 5.1 Service Client Configuration

```java path=null start=null
@ApplicationScoped
public class ServiceClientConfig {
    @Produces
    public KeycloakClient keycloakClient(
            @ConfigProperty(name = "keycloak.url") String url,
            @ConfigProperty(name = "keycloak.realm") String realm) {
        return KeycloakClient.builder()
            .serverUrl(url)
            .realm(realm)
            .build();
    }
    
    @Produces
    public DataWarehouseClient dataWarehouseClient(
            @ConfigProperty(name = "warehouse.url") String url) {
        return DataWarehouseClient.builder()
            .url(url)
            .build();
    }
}
```

### 5.2 Integration Patterns

```java path=null start=null
@ApplicationScoped
public class ExternalServiceIntegration {
    @Inject
    KeycloakClient keycloak;
    
    @Inject
    DataWarehouseClient warehouse;
    
    @CircuitBreaker(requestVolumeThreshold = 4)
    @Retry(maxRetries = 3)
    @Bulkhead(value = 10)
    public Uni<AuthResponse> authenticateRequest(AuthRequest request) {
        return Uni.createFrom().item(() ->
            keycloak.authenticate(request));
    }
    
    @CircuitBreaker(requestVolumeThreshold = 4)
    @Timeout(250)
    @Fallback(fallbackMethod = "getLocalData")
    public Uni<DataResponse> fetchWarehouseData(DataRequest request) {
        return warehouse.fetchData(request)
            .onFailure().transform(e -> 
                new ServiceException("Warehouse fetch failed", e));
    }
}
```

## 6. API Versioning Strategy

### 6.1 Version Management

1. **URI Versioning**
   - Base path includes version: `/api/v1/`
   - Major version changes only
   - Support for multiple active versions

2. **Compatibility Rules**
   - Backward compatible within same major version
   - Breaking changes require version bump
   - Deprecation with sunset period

### 6.2 Version Header Support

```java path=null start=null
@Provider
public class VersioningRequestFilter implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext requestContext) {
        String apiVersion = requestContext.getHeaderString("X-API-Version");
        if (apiVersion != null) {
            // Override URI version if header is present
            requestContext.setProperty("api.version", apiVersion);
        }
    }
}
```

## 7. Error Handling

### 7.1 Standard Error Response

```json path=null start=null
{
    "error": {
        "code": "RESOURCE_NOT_FOUND",
        "message": "The requested resource was not found",
        "details": {
            "resourceType": "Context",
            "resourceId": "123e4567-e89b-12d3-a456-426614174000"
        },
        "timestamp": "2025-09-02T01:38:49Z",
        "traceId": "ab23cd45-ef67-89gh-ij01"
    }
}
```

### 7.2 Error Handler Implementation

```java path=null start=null
@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Throwable> {
    @Inject
    ObjectMapper objectMapper;
    
    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof WebApplicationException) {
            return handleWebException((WebApplicationException) exception);
        }
        
        if (exception instanceof ValidationException) {
            return handleValidationException((ValidationException) exception);
        }
        
        // Default to internal server error
        return Response.status(Status.INTERNAL_SERVER_ERROR)
            .entity(createErrorResponse(exception))
            .build();
    }
    
    private ErrorResponse createErrorResponse(Throwable exception) {
        return ErrorResponse.builder()
            .code(determineErrorCode(exception))
            .message(exception.getMessage())
            .timestamp(Instant.now())
            .traceId(MDC.get("traceId"))
            .build();
    }
}
```

## 8. Documentation Strategy

### 8.1 OpenAPI Documentation Generation

```java path=null start=null
@ApplicationScoped
public class OpenAPIConfig {
    @ConfigProperty(name = "app.version")
    String appVersion;
    
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("GO-Commerce MCP API")
                .version(appVersion)
                .description("Model Context Protocol API for GO-Commerce")
                .contact(new Contact()
                    .name("GO-Commerce Team")
                    .email("dev@gocommerce.dev")))
            .components(new Components()
                .addSecuritySchemes("bearer-key",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
```

### 8.2 API Documentation Guidelines

1. **Endpoint Documentation**
   - Clear description of purpose
   - Request/response examples
   - Authentication requirements
   - Error scenarios

2. **Schema Documentation**
   - Field descriptions
   - Validation rules
   - Relationships
   - Examples

## Implementation Guidelines

1. **API Design Principles**
   - RESTful resource modeling
   - Consistent error handling
   - Proper HTTP method usage
   - Comprehensive documentation

2. **Integration Best Practices**
   - Circuit breaker patterns
   - Retry with backoff
   - Timeout handling
   - Graceful degradation

3. **Versioning Guidelines**
   - Semantic versioning
   - Deprecation process
   - Migration support
   - Documentation updates

4. **Security Considerations**
   - Authentication enforcement
   - Authorization checks
   - Input validation
   - Rate limiting

// Copilot: This file may have been generated or refactored by GitHub Copilot.
