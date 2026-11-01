import fp from 'fastify-plugin';
import { PrismaPg } from '@prisma/adapter-pg';
import { PrismaClient } from '../generated/prisma/client.js';

declare module 'fastify' {
  interface FastifyInstance {
    prisma: PrismaClient;
  }
}

export const prismaPlugin = fp(async (app) => {
  const adapter = new PrismaPg({
    connectionString: process.env['DATABASE_URL'],
    // pg waits forever by default; Prisma 6 gave up after 5s.
    connectionTimeoutMillis: 5000,
  });
  const prisma = new PrismaClient({
    adapter,
    log: process.env['NODE_ENV'] === 'development' ? ['query', 'error'] : ['error'],
  });

  await prisma.$connect();

  app.decorate('prisma', prisma);
  app.addHook('onClose', async () => {
    await prisma.$disconnect();
  });
});
