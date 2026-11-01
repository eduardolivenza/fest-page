import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import type { FastifyInstance } from 'fastify';
import { hash } from 'argon2';
import { Prisma } from '../src/generated/prisma/client.js';
import { adminToken, buildTestApp, type FakePrisma } from './helpers.js';

let app: FastifyInstance;
let prisma: FakePrisma;

beforeEach(async () => {
  ({ app, prisma } = await buildTestApp());
});

afterEach(async () => {
  await app.close();
});

describe('POST /api/admin/auth/login', () => {
  it('returns a token for valid credentials', async () => {
    prisma.adminUser.findUnique.mockResolvedValue({
      id: 'admin-1',
      email: 'admin@festdescans.com',
      name: 'Admin',
      passwordHash: await hash('correct-password'),
    });

    const res = await app.inject({
      method: 'POST',
      url: '/api/admin/auth/login',
      payload: { email: 'admin@festdescans.com', password: 'correct-password' },
    });

    expect(res.statusCode).toBe(200);
    const { token, user } = res.json();
    expect(user).toEqual({ id: 'admin-1', email: 'admin@festdescans.com', name: 'Admin' });
    expect(app.jwt.verify<{ sub: string }>(token).sub).toBe('admin-1');
  });

  it('rejects a wrong password', async () => {
    prisma.adminUser.findUnique.mockResolvedValue({ id: 'admin-1', email: 'a@b.com', passwordHash: await hash('right') });

    const res = await app.inject({ method: 'POST', url: '/api/admin/auth/login', payload: { email: 'a@b.com', password: 'wrong' } });

    expect(res.statusCode).toBe(401);
  });

  it('rejects an unknown user', async () => {
    const res = await app.inject({ method: 'POST', url: '/api/admin/auth/login', payload: { email: 'x@y.com', password: 'whatever' } });
    expect(res.statusCode).toBe(401);
  });
});

describe('admin routes require a token', () => {
  it.each([
    ['GET', '/api/admin/contacts'],
    ['PATCH', '/api/admin/contacts/c1/read'],
    ['DELETE', '/api/admin/contacts/c1'],
    ['GET', '/api/admin/products'],
    ['GET', '/api/admin/orders'],
  ] as const)('%s %s → 401 without a token', async (method, url) => {
    const res = await app.inject({ method, url });
    expect(res.statusCode).toBe(401);
  });

  it('rejects a token signed with another secret', async () => {
    const res = await app.inject({
      method: 'GET',
      url: '/api/admin/contacts',
      headers: { authorization: 'Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4In0.invalid' },
    });
    expect(res.statusCode).toBe(401);
  });
});

describe('admin contacts', () => {
  it('lists contacts', async () => {
    prisma.contactRequest.findMany.mockResolvedValue([{ id: 'c1', name: 'Maria' }]);
    prisma.contactRequest.count.mockResolvedValue(1);

    const res = await app.inject({
      method: 'GET',
      url: '/api/admin/contacts',
      headers: { authorization: `Bearer ${adminToken(app)}` },
    });

    expect(res.statusCode).toBe(200);
    expect(res.json()).toMatchObject({ data: [{ id: 'c1' }], total: 1 });
  });

  it('marks a contact as read', async () => {
    prisma.contactRequest.update.mockResolvedValue({ id: 'c1', read: true });

    const res = await app.inject({
      method: 'PATCH',
      url: '/api/admin/contacts/c1/read',
      headers: { authorization: `Bearer ${adminToken(app)}` },
    });

    expect(res.statusCode).toBe(200);
    expect(prisma.contactRequest.update).toHaveBeenCalledWith({ where: { id: 'c1' }, data: { read: true } });
  });

  it('deletes a contact', async () => {
    prisma.contactRequest.delete.mockResolvedValue({ id: 'c1' });

    const res = await app.inject({
      method: 'DELETE',
      url: '/api/admin/contacts/c1',
      headers: { authorization: `Bearer ${adminToken(app)}` },
    });

    expect(res.statusCode).toBe(204);
    expect(prisma.contactRequest.delete).toHaveBeenCalledWith({ where: { id: 'c1' } });
  });

  it('returns 404 when deleting a contact that does not exist', async () => {
    prisma.contactRequest.delete.mockRejectedValue(
      new Prisma.PrismaClientKnownRequestError('Record not found', { code: 'P2025', clientVersion: 'test' }),
    );

    const res = await app.inject({
      method: 'DELETE',
      url: '/api/admin/contacts/missing',
      headers: { authorization: `Bearer ${adminToken(app)}` },
    });

    expect(res.statusCode).toBe(404);
  });
});
