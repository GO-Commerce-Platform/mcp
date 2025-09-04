# MCP Service MVP Plan

The goal of the MCP MVP is to create a functional, secure, and tenant-aware API bridge for the AI agent, focusing on core e-commerce interactions.

---

### **Phase 1: Core Product Inquiry**

**Goal:** Enable an AI agent to securely query product information for a specific store. This is the foundational step for any shopping interaction.

**Key Features:**
*   A secure REST endpoint for product queries.
*   Tenant resolution using a JWT token from the AI agent.
*   Basic product search functionality (e.g., by name or SKU).

**Tasks:**
1.  **Project Setup:**
    *   Initialize the Quarkus project for the MCP service.
    *   Configure dependencies: JAX-RS (for REST), Hibernate (for data), and Keycloak client (for auth).
2.  **Authentication & Tenant Resolution:**
    *   Implement JWT validation to authenticate the AI agent and extract the `storeId`.
    *   Implement the multi-tenant `TenantResolver` to ensure all database queries are correctly scoped to the store's schema.
3.  **API Endpoint:**
    *   Create a REST endpoint: `GET /api/v1/products`
    *   Implement query parameters for basic filtering (e.g., `?search=...`).
4.  **Data Access:**
    *   Create a read-only repository to fetch product data from the correct tenant schema.
5.  **Testing:**
    *   Write integration tests for the product query endpoint, using a test JWT to simulate a request from the AI agent.

---

### **Phase 2: Basic Cart Management**

**Goal:** Empower the AI agent to manage a customer's shopping cart, moving from inquiry to action.

**Key Features:**
*   Endpoints to add items to a cart, view the cart, and remove items.
*   Cart operations must be tied to the specific customer and store.

**Tasks:**
1.  **API Endpoints:**
    *   `POST /api/v1/cart/items` (Add an item)
    *   `GET /api/v1/cart` (View cart contents)
    *   `DELETE /api/v1/cart/items/{itemId}` (Remove an item)
2.  **Cart Logic:**
    *   Implement the business logic to manage the shopping cart. For the MVP, this could be a simple in-memory or Redis-based cache solution.
    *   Ensure the cart is associated with the correct user and store context.
3.  **Integration Definition:**
    *   Define the API contract for how the cart will be handed off to the main GO-Commerce server for the final checkout process.
4.  **Testing:**
    *   Write integration tests for all cart management endpoints.
