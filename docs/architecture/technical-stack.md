# GO-Commerce MCP Server Technical Stack & Dependencies Specification

## Overview

This document specifies the technical stack, dependencies, and configuration required for the GO-Commerce MCP (Model Context Protocol) service. The service is built using Quarkus, leveraging its extensions and integration capabilities to create a robust, scalable, multi-tenant microservice.

## 1. Core Dependencies

### 1.1 Maven Project Configuration

```xml path=null start=null
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>dev.tiodati.saas.gocommerce</groupId>
    <artifactId>mcp-server</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <properties>
        <compiler-plugin.version>3.11.0</compiler-plugin.version>
        <maven.compiler.release>21</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <quarkus.platform.version>3.23.4</quarkus.platform.version>
        <lombok.version>1.18.30</lombok.version>
        <testcontainers.version>1.19.7</testcontainers.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>io.quarkus.platform</groupId>
                <artifactId>quarkus-bom</artifactId>
                <version>${quarkus.platform.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

### 1.2 Quarkus Extensions

```xml path=null start=null
<dependencies>
    <!-- Core Extensions -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-arc</artifactId>
    </dependency>
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-resteasy-reactive-jackson</artifactId>
    </dependency>

    <!-- Database & ORM -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-hibernate-orm-panache</artifactId>
    </dependency>
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-jdbc-postgresql</artifactId>
    </dependency>
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-flyway</artifactId>
    </dependency>

    <!-- Security -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-oidc</artifactId>
    </dependency>
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-keycloak-authorization</artifactId>
    </dependency>

    <!-- Reactive & Async -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-mutiny</artifactId>
    </dependency>
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-reactive-pg-client</artifactId>
    </dependency>

    <!-- Observability -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-micrometer-registry-prometheus</artifactId>
    </dependency>
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-opentelemetry</artifactId>
    </dependency>
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-smallrye-health</artifactId>
    </dependency>

    <!-- Event Streaming -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-kafka-client</artifactId>
    </dependency>
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-smallrye-reactive-messaging-kafka</artifactId>
    </dependency>

    <!-- Caching -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-redis-client</artifactId>
    </dependency>
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-cache</artifactId>
    </dependency>

    <!-- Development -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>${lombok.version}</version>
        <scope>provided</scope>
    </dependency>

    <!-- Testing -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-junit5</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers</artifactId>
        <version>${testcontainers.version}</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <version>${testcontainers.version}</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## 2. Entity Model Structure

### 2.1 Base Entity Pattern

```java path=null start=null
@MappedSuperclass
@NoArgsConstructor
@Getter @Setter
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
```

### 2.2 Tenant-Aware Entity Pattern

```java path=null start=null
@MappedSuperclass
@NoArgsConstructor
@Getter @Setter
public abstract class TenantAwareEntity extends BaseEntity {
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        tenantId = TenantContext.getCurrentTenant();
    }
}
```

## 3. Repository Pattern

### 3.1 Base Repository

```java path=null start=null
@ApplicationScoped
public abstract class BaseRepository<T extends BaseEntity> implements PanacheRepository<T> {
    
    public T findByIdOrThrow(UUID id) {
        return findByIdOptional(id)
            .orElseThrow(() -> new EntityNotFoundException(
                String.format("Entity with id %s not found", id)));
    }
    
    public List<T> findAllPaginated(int page, int size) {
        return findAll()
            .page(Page.of(page, size))
            .list();
    }
}
```

### 3.2 Tenant-Aware Repository

```java path=null start=null
@ApplicationScoped
public abstract class TenantAwareRepository<T extends TenantAwareEntity> 
    extends BaseRepository<T> {
    
    @Override
    public PanacheQuery<T> findAll() {
        return find("tenantId", TenantContext.getCurrentTenant());
    }
    
    @Override
    public Optional<T> findByIdOptional(UUID id) {
        return find("id = ?1 and tenantId = ?2", 
            id, TenantContext.getCurrentTenant()).firstResultOptional();
    }
}
```

## 4. Reactive Programming Model

### 4.1 Reactive Service Pattern

```java path=null start=null
@ApplicationScoped
public class ReactiveDataService {
    @Inject
    ReactivePostgresClient pgClient;
    
    public Multi<Data> streamData(String query) {
        return pgClient.query(query)
            .onItem().transformToMulti(rows -> Multi.createFrom().iterable(rows))
            .onItem().transform(this::mapToData)
            .onFailure().invoke(this::handleError);
    }
    
    public Uni<List<Data>> fetchDataBatch(String query) {
        return pgClient.query(query)
            .onItem().transform(rows -> 
                rows.stream()
                    .map(this::mapToData)
                    .collect(Collectors.toList()))
            .onFailure().recoverWithItem(this::handleError);
    }
}
```

### 4.2 Event Handling Pattern

```java path=null start=null
@ApplicationScoped
public class EventProcessor {
    @Inject
    ReactiveDataService dataService;
    
    @Incoming("data-stream")
    public Multi<Message<String>> processDataStream(Multi<Message<String>> input) {
        return input
            .onItem().transformToUniAndMerge(message ->
                dataService.processData(message.getPayload())
                    .onItem().transform(result -> 
                        Message.of(result, () -> message.ack())))
            .onFailure().invoke(this::handleStreamError);
    }
}
```

## 5. Monitoring Configuration

### 5.1 Micrometer Setup

```properties path=null start=null
# Metrics configuration
quarkus.micrometer.enabled=true
quarkus.micrometer.registry-enabled=true
quarkus.micrometer.export.prometheus.enabled=true
quarkus.micrometer.export.prometheus.path=/metrics
quarkus.micrometer.binder.http-client.enabled=true
quarkus.micrometer.binder.http-server.enabled=true
quarkus.micrometer.binder.jvm=true
quarkus.micrometer.binder.system=true
```

### 5.2 OpenTelemetry Configuration

```properties path=null start=null
# Tracing configuration
quarkus.opentelemetry.enabled=true
quarkus.opentelemetry.tracer.enabled=true
quarkus.opentelemetry.tracer.exporter.otlp.endpoint=http://jaeger:4317
quarkus.opentelemetry.tracer.sampler=ratio
quarkus.opentelemetry.tracer.sampler.ratio=1.0
```

## 6. Development Tools

### 6.1 Build Configuration

```xml path=null start=null
<build>
    <plugins>
        <plugin>
            <groupId>io.quarkus.platform</groupId>
            <artifactId>quarkus-maven-plugin</artifactId>
            <version>${quarkus.platform.version}</version>
            <executions>
                <execution>
                    <goals>
                        <goal>build</goal>
                        <goal>generate-code</goal>
                        <goal>generate-code-tests</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>${compiler-plugin.version}</version>
            <configuration>
                <compilerArgs>
                    <arg>-parameters</arg>
                </compilerArgs>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>${lombok.version}</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### 6.2 Development Mode Configuration

```properties path=null start=null
# Development configuration
%dev.quarkus.live-reload.enabled=true
%dev.quarkus.live-reload.url=http://localhost:8080
%dev.quarkus.swagger-ui.enable=true
%dev.quarkus.swagger-ui.always-include=true

# Test configuration
%test.quarkus.datasource.db-kind=postgresql
%test.quarkus.datasource.username=test
%test.quarkus.datasource.password=test
%test.quarkus.datasource.reactive.url=vertx-reactive:postgresql://localhost:5432/test
```

## 7. Testing Framework

### 7.1 Integration Test Pattern

```java path=null start=null
@QuarkusTest
@TestProfile(TestProfiles.Integration.class)
public class DataServiceIntegrationTest {
    @Inject
    DataService dataService;
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        "postgres:15-alpine")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");
    
    @BeforeAll
    static void setUp() {
        postgres.start();
    }
    
    @Test
    @TestSecurity(user = "testuser", roles = {"tenant-admin"})
    @TestTransaction
    void testDataRetrieval() {
        // Test implementation
    }
}
```

### 7.2 Repository Test Pattern

```java path=null start=null
@QuarkusTest
public class TenantAwareRepositoryTest {
    @Inject
    TenantAwareRepository<TestEntity> repository;
    
    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenant("test-tenant");
    }
    
    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }
    
    @Test
    @TestTransaction
    void testTenantIsolation() {
        // Test implementation
    }
}
```

## 8. Configuration Management

### 8.1 Application Properties

```properties path=null start=null
# Core Configuration
quarkus.application.name=mcp-server
quarkus.application.version=${project.version}
quarkus.http.port=8080
quarkus.http.read-timeout=30s
quarkus.ssl.native=true

# Datasource Configuration
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/gocommerce
quarkus.datasource.username=${DB_USER}
quarkus.datasource.password=${DB_PASSWORD}
quarkus.datasource.jdbc.min-size=10
quarkus.datasource.jdbc.max-size=20

# Hibernate Configuration
quarkus.hibernate-orm.database.generation=none
quarkus.hibernate-orm.multitenant=SCHEMA
quarkus.hibernate-orm.physical-naming-strategy=org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy

# Cache Configuration
quarkus.cache.caffeine."data-cache".initial-capacity=100
quarkus.cache.caffeine."data-cache".maximum-size=1000
quarkus.cache.caffeine."data-cache".expire-after-write=5m
```

### 8.2 Logging Configuration

```properties path=null start=null
# Logging Configuration
quarkus.log.category."dev.tiodati.saas.gocommerce".level=INFO
quarkus.log.category."io.quarkus".level=INFO
quarkus.log.console.format=%d{HH:mm:ss} %-5p [%c{2.}] (%t) %s%e%n
quarkus.log.console.level=INFO
quarkus.log.file.enable=true
quarkus.log.file.path=/var/log/mcp-server.log
quarkus.log.file.format=%d{yyyy-MM-dd HH:mm:ss,SSS} %h %N[%i] %-5p [%c{1.}] (%t) %s%e%n
```

## 9. Implementation Guidelines

1. **Code Organization**:
   - Follow package-by-feature structure
   - Keep classes focused and single-responsibility
   - Use dependency injection for loose coupling

2. **Error Handling**:
   - Use custom exceptions for business logic
   - Implement global exception handling
   - Provide meaningful error messages

3. **Testing**:
   - Write integration tests for critical paths
   - Use test containers for database tests
   - Implement proper test cleanup

4. **Performance**:
   - Use reactive programming where appropriate
   - Implement caching strategies
   - Monitor and optimize database queries

5. **Security**:
   - Validate all inputs
   - Use proper authentication/authorization
   - Implement audit logging

## 10. Dependency Upgrade Strategy

1. **Regular Updates**:
   - Monthly security patches
   - Quarterly minor version updates
   - Yearly major version updates

2. **Version Control**:
   - Use version ranges for minor updates
   - Pin exact versions for critical dependencies
   - Document breaking changes

3. **Testing Requirements**:
   - Full test suite execution
   - Performance benchmark comparison
   - Security vulnerability scanning

// Copilot: This file may have been generated or refactored by GitHub Copilot.
