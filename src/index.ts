import { MCPServer } from '@modelcontextprotocol/server';
import express from 'express';
import { config } from './config';
import { setupHandlers } from './handlers';
import { setupServices } from './services';

async function bootstrap() {
  // Create Express app
  const app = express();
  
  // Initialize MCP server
  const mcp = new MCPServer({
    name: 'gocommerce-mcp',
    version: '1.0.0',
    description: 'Model Context Protocol server for GO-Commerce',
    config
  });

  // Set up MCP services (database connections, caches, etc)
  await setupServices(mcp);

  // Set up MCP request handlers
  setupHandlers(mcp);

  // Start server
  const port = process.env.PORT || 3000;
  app.listen(port, () => {
    console.log(`GO-Commerce MCP server listening at http://localhost:${port}`);
  });
}

bootstrap().catch(console.error);
