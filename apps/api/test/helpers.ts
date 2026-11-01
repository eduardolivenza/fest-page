import { vi } from 'vitest';
import fp from 'fastify-plugin';
import type { FastifyInstance } from 'fastify';

/** Minimal in-memory stand-in for the Prisma client: one vi.fn per method the routes call. */
export function createFakePrisma() {
  const model = () => ({
    findMany: vi.fn().mockResolvedValue([]),
    findFirst: vi.fn().mockResolvedValue(null),
    findUnique: vi.fn().mockResolvedValue(null),
    findUniqueOrThrow: vi.fn(),
    count: vi.fn().mockResolvedValue(0),
    create: vi.fn(),
    update: vi.fn(),
    upsert: vi.fn(),
    delete: vi.fn(),
  });
  return {
    product: model(),
    contactRequest: model(),
    adminUser: model(),
    customer: model(),
    order: model(),
  };
}

export type FakePrisma = ReturnType<typeof createFakePrisma>;

export const fakePrisma: { current: FakePrisma } = { current: createFakePrisma() };

// Replaces the real plugin (which connects to PostgreSQL) with the fake above.
vi.mock('../src/plugins/prisma.js', () => ({
  prismaPlugin: fp(async (app: FastifyInstance) => {
    app.decorate('prisma', fakePrisma.current as unknown as FastifyInstance['prisma']);
  }),
}));

export async function buildTestApp() {
  fakePrisma.current = createFakePrisma();
  const { buildApp } = await import('../src/app.js');
  const app = await buildApp();
  await app.ready();
  return { app, prisma: fakePrisma.current };
}

export function adminToken(app: FastifyInstance) {
  return app.jwt.sign({ sub: 'admin-1', email: 'admin@festdescans.com' });
}
