import Fastify from 'fastify';
import cors from '@fastify/cors';
import jwt from '@fastify/jwt';
import swagger from '@fastify/swagger';
import swaggerUi from '@fastify/swagger-ui';

import { prismaPlugin } from './plugins/prisma.js';
import { authPlugin } from './plugins/auth.js';
import { productsRoutes } from './routes/products.js';
import { contactsRoutes } from './routes/contacts.js';
import { ordersRoutes } from './routes/orders.js';
import { adminAuthRoutes } from './routes/admin/auth.js';
import { adminProductsRoutes } from './routes/admin/products.js';
import { adminOrdersRoutes } from './routes/admin/orders.js';
import { adminContactsRoutes } from './routes/admin/contacts.js';

export async function buildApp() {
  const app = Fastify({
    logger: {
      level: process.env['NODE_ENV'] === 'production' ? 'warn' : 'info',
    },
  });

  await app.register(cors, {
    origin: [
      process.env['WEB_URL'] ?? 'http://localhost:3000',
      process.env['ADMIN_URL'] ?? 'http://localhost:3002',
    ],
    credentials: true,
  });

  await app.register(jwt, {
    secret: process.env['JWT_SECRET'] ?? 'dev-secret-change-in-production',
    sign: { expiresIn: '8h' },
  });

  await app.register(swagger, {
    openapi: {
      info: { title: 'FestPage API', version: '1.0.0' },
    },
  });

  await app.register(swaggerUi, { routePrefix: '/docs' });

  await app.register(prismaPlugin);
  await app.register(authPlugin);

  // Public routes
  await app.register(productsRoutes, { prefix: '/api/products' });
  await app.register(contactsRoutes, { prefix: '/api/contacts' });
  await app.register(ordersRoutes, { prefix: '/api/orders' });

  // Admin routes (JWT-protected)
  await app.register(adminAuthRoutes, { prefix: '/api/admin/auth' });
  await app.register(adminProductsRoutes, { prefix: '/api/admin/products' });
  await app.register(adminOrdersRoutes, { prefix: '/api/admin/orders' });
  await app.register(adminContactsRoutes, { prefix: '/api/admin/contacts' });

  app.get('/health', () => ({ status: 'ok' }));

  return app;
}
