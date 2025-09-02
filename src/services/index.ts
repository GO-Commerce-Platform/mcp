import { MCPServer } from '@modelcontextprotocol/server';
import { Pool } from 'pg';
import Redis from 'redis';

// Database service for managing PostgreSQL connections
class DatabaseService {
  private pool: Pool;

  constructor(config: any) {
    this.pool = new Pool(config.database);
  }

  async query(text: string, params?: any[]) {
    return this.pool.query(text, params);
  }

  async getConnection() {
    return this.pool.connect();
  }

  async setSchema(schema: string) {
    return this.query(`SET search_path TO ${schema};`);
  }
}

// Cache service for Redis operations
class CacheService {
  private client: Redis.RedisClientType;

  constructor(config: any) {
    this.client = Redis.createClient({
      url: `redis://${config.redis.host}:${config.redis.port}`
    });
  }

  async connect() {
    await this.client.connect();
  }

  async get(key: string) {
    return this.client.get(key);
  }

  async set(key: string, value: string, ttl?: number) {
    if (ttl) {
      return this.client.setEx(key, ttl, value);
    }
    return this.client.set(key, value);
  }
}

// Store context service for managing tenant contexts
class StoreContextService {
  private currentStore?: string;
  private db: DatabaseService;

  constructor(db: DatabaseService) {
    this.db = db;
  }

  async setCurrentStore(storeSchema: string) {
    await this.db.setSchema(storeSchema);
    this.currentStore = storeSchema;
  }

  getCurrentStore() {
    return this.currentStore;
  }

  async validateStoreAccess(storeSchema: string, role: string) {
    // Implement store access validation logic
    return true;
  }
}

export async function setupServices(mcp: MCPServer) {
  // Initialize services
  const db = new DatabaseService(mcp.config);
  const cache = new CacheService(mcp.config);
  const storeContext = new StoreContextService(db);

  // Connect cache service
  await cache.connect();

  // Register services with MCP server
  mcp.registerService('database', db);
  mcp.registerService('cache', cache);
  mcp.registerService('storeContext', storeContext);

  return {
    database: db,
    cache,
    storeContext
  };
}
