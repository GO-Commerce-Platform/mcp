-- GO-Commerce MCP Test Data Import Script
-- This script sets up basic test data for tenant resolution testing

-- Create MCP control schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS mcp;

-- Create test tenant schemas if they don't exist
CREATE SCHEMA IF NOT EXISTS store_acme_corp;
CREATE SCHEMA IF NOT EXISTS store_test_store;
CREATE SCHEMA IF NOT EXISTS store_simple_tenant;

-- Basic test table in the MCP control schema (for metadata)
CREATE TABLE IF NOT EXISTS mcp.test_contexts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) NOT NULL,
    schema_name VARCHAR(63) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id)
);

-- Sample control data for testing
INSERT INTO mcp.test_contexts (tenant_id, schema_name) VALUES 
    ('acme-corp', 'store_acme_corp'),
    ('test-store', 'store_test_store'),
    ('simple-tenant', 'store_simple_tenant')
ON CONFLICT (tenant_id) DO NOTHING;

-- Basic test table in tenant schemas (for isolation testing)
CREATE TABLE IF NOT EXISTS store_acme_corp.test_data (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255),
    value INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS store_test_store.test_data (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255),
    value INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS store_simple_tenant.test_data (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255),
    value INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Sample tenant-specific test data for isolation verification
INSERT INTO store_acme_corp.test_data (name, value) VALUES 
    ('acme-item-1', 100),
    ('acme-item-2', 200);

INSERT INTO store_test_store.test_data (name, value) VALUES 
    ('test-item-1', 300),
    ('test-item-2', 400);

INSERT INTO store_simple_tenant.test_data (name, value) VALUES 
    ('simple-item-1', 500),
    ('simple-item-2', 600);

-- Grant necessary permissions for tests (adjust based on your test user)
-- GRANT ALL PRIVILEGES ON SCHEMA mcp TO test_user;
-- GRANT ALL PRIVILEGES ON SCHEMA store_acme_corp TO test_user;
-- GRANT ALL PRIVILEGES ON SCHEMA store_test_store TO test_user;
-- GRANT ALL PRIVILEGES ON SCHEMA store_simple_tenant TO test_user;

-- Copilot: This file may have been generated or refactored by GitHub Copilot.
