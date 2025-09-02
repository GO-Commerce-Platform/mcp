# GO-Commerce MCP Server

Model Context Protocol server providing intelligent context-aware assistance for GO-Commerce development. The server is designed to work with GO-Commerce's multi-tenant architecture, providing schema management, code generation, and development workflow integrations.

## Features

- **Store Management**
  - Store schema creation and migration
  - Schema validation and analysis
  - Multi-tenant context management

- **Query Operations**
  - Tenant-aware database queries
  - Safe schema isolation
  - Query validation and optimization

- **Code Generation**
  - Entity and DTO generation
  - Repository and service templates
  - Multi-tenant aware code snippets

- **Model Suggestions**
  - Schema-based model recommendations
  - Best practice suggestions
  - Code pattern analysis

## Prerequisites

- Node.js 18+
- PostgreSQL 15+
- Redis 7+
- GO-Commerce server running locally

## Installation

1. Clone the repository:
   ```bash
   git clone [mcp-repo-url] gocommerce/mcp
   ```

2. Install dependencies:
   ```bash
   cd gocommerce/mcp
   npm install
   ```

3. Set up environment variables:
   ```bash
   cp .env.example .env
   # Edit .env with your configuration
   ```

## Development

Start the server in development mode:
```bash
npm run dev
```

Run tests:
```bash
npm test
```

Build for production:
```bash
npm run build
```

## Configuration

The MCP server requires the following environment variables:

```env
# Database
DB_HOST=localhost
DB_PORT=5432
DB_USER=postgres
DB_PASS=postgres
DB_NAME=gocommerce

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Server
PORT=3000
NODE_ENV=development
```

## API Documentation

### Store Operations

- `POST /store/create`
  - Create new store schema
  - Body: `{ storeKey: string }`

- `POST /store/migrate`
  - Run migrations for store
  - Body: `{ storeKey: string }`

- `POST /store/validate`
  - Validate store schema
  - Body: `{ storeKey: string }`

### Query Operations

- `POST /query/execute`
  - Execute store-specific query
  - Body: `{ query: string, params?: any[] }`

### Schema Operations

- `POST /schema/analyze`
  - Analyze store schema structure
  - Body: `{ storeKey: string }`

### Code Generation

- `POST /code/generate`
  - Generate code from template
  - Body: `{ template: string, params: object }`

### Model Operations

- `POST /model/suggest`
  - Get model suggestions
  - Body: `{ context: object }`

## Architecture

The MCP server is built with TypeScript and follows a modular architecture:

```
src/
├── config/        # Configuration and environment
├── services/      # Core services (DB, Redis, etc)
├── handlers/      # Operation handlers
├── utils/         # Utility functions
└── types/         # TypeScript type definitions
```

## Contributing

1. Create feature branch
2. Make changes
3. Add tests
4. Run linting: `npm run lint`
5. Submit pull request

## License

MIT License - see LICENSE file

## Related Projects

- [GO-Commerce Server](../server)
- [GO-Commerce Documentation](../wiki)
- [Docker Configurations](../docker)
