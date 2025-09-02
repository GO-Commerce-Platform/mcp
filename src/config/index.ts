import { Config } from '@modelcontextprotocol/server';
import { z } from 'zod';

// Environment variable validation schema
const envSchema = z.object({
  // Database
  DB_HOST: z.string(),
  DB_PORT: z.string().transform(Number),
  DB_USER: z.string(),
  DB_PASS: z.string(),
  DB_NAME: z.string(),
  
  // Redis
  REDIS_HOST: z.string(),
  REDIS_PORT: z.string().transform(Number),
  
  // Server
  PORT: z.string().transform(Number).optional(),
  NODE_ENV: z.enum(['development', 'production', 'test']).default('development'),
});

// Parse and validate environment variables
const env = envSchema.parse(process.env);

export const config: Config = {
  // Database configuration
  database: {
    host: env.DB_HOST,
    port: env.DB_PORT,
    user: env.DB_USER,
    password: env.DB_PASS,
    database: env.DB_NAME,
  },

  // Redis configuration 
  redis: {
    host: env.REDIS_HOST,
    port: env.REDIS_PORT,
  },

  // Server settings
  server: {
    port: env.PORT || 3000,
    environment: env.NODE_ENV,
  },

  // MCP settings
  mcp: {
    // Supported operations
    operations: [
      'store.create',
      'store.migrate',
      'store.validate',
      'query.execute',
      'schema.analyze',
      'code.generate',
      'model.suggest',
    ],

    // Store context settings
    storeContext: {
      schemaPrefix: 'store_',
      supportedRoles: [
        'platform-admin',
        'store-admin',
        'product-manager',
        'order-manager',
        'customer-service',
        'customer'
      ],
    },
  }
};
