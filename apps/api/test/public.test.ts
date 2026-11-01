import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { FastifyInstance } from 'fastify';
import { buildTestApp, type FakePrisma } from './helpers.js';

let app: FastifyInstance;
let prisma: FakePrisma;

beforeEach(async () => {
  ({ app, prisma } = await buildTestApp());
});

afterEach(async () => {
  await app.close();
  vi.unstubAllEnvs();
  vi.unstubAllGlobals();
});

describe('GET /health', () => {
  it('returns ok', async () => {
    const res = await app.inject({ method: 'GET', url: '/health' });
    expect(res.statusCode).toBe(200);
    expect(res.json()).toEqual({ status: 'ok' });
  });
});

describe('GET /api/products', () => {
  it('returns a paginated list with numeric prices', async () => {
    prisma.product.findMany.mockResolvedValue([
      { id: 'p1', name: 'Matalàs', price: '499.90', sizes: [{ id: 's1', price: '549.90' }], images: [] },
    ]);
    prisma.product.count.mockResolvedValue(1);

    const res = await app.inject({ method: 'GET', url: '/api/products?pageSize=12' });

    expect(res.statusCode).toBe(200);
    const body = res.json();
    expect(body).toMatchObject({ total: 1, page: 1, pageSize: 12, totalPages: 1 });
    expect(body.data[0].price).toBe(499.9);
    expect(body.data[0].sizes[0].price).toBe(549.9);
  });

  it('caps pageSize at 50 and filters by category', async () => {
    await app.inject({ method: 'GET', url: '/api/products?pageSize=500&category=PILLOW' });

    const args = prisma.product.findMany.mock.calls[0]![0];
    expect(args.take).toBe(50);
    expect(args.where).toEqual({ category: 'PILLOW' });
  });

  it('returns 404 for an unknown product', async () => {
    const res = await app.inject({ method: 'GET', url: '/api/products/does-not-exist' });
    expect(res.statusCode).toBe(404);
  });
});

describe('POST /api/contacts', () => {
  const valid = { name: 'Maria', email: 'maria@example.com', message: 'Voldria informació sobre matalassos.' };

  it('rejects invalid data with 400', async () => {
    const res = await app.inject({ method: 'POST', url: '/api/contacts', payload: { name: 'M', email: 'nope', message: 'short' } });
    expect(res.statusCode).toBe(400);
    expect(prisma.contactRequest.create).not.toHaveBeenCalled();
  });

  it('saves a valid message when Turnstile is not configured', async () => {
    vi.stubEnv('TURNSTILE_SECRET_KEY', '');
    prisma.contactRequest.create.mockResolvedValue({ id: 'c1', ...valid });

    const res = await app.inject({ method: 'POST', url: '/api/contacts', payload: valid });

    expect(res.statusCode).toBe(201);
    expect(prisma.contactRequest.create).toHaveBeenCalledWith({ data: valid });
  });

  it('rejects with 403 when Turnstile is configured and the token is missing', async () => {
    vi.stubEnv('TURNSTILE_SECRET_KEY', 'secret');
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);

    const res = await app.inject({ method: 'POST', url: '/api/contacts', payload: valid });

    expect(res.statusCode).toBe(403);
    expect(fetchMock).not.toHaveBeenCalled();
    expect(prisma.contactRequest.create).not.toHaveBeenCalled();
  });

  it('verifies the token with Cloudflare, passing the visitor IP, and does not store the token', async () => {
    vi.stubEnv('TURNSTILE_SECRET_KEY', 'secret');
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ success: true })));
    vi.stubGlobal('fetch', fetchMock);
    prisma.contactRequest.create.mockResolvedValue({ id: 'c1', ...valid });

    const res = await app.inject({
      method: 'POST',
      url: '/api/contacts',
      headers: { 'cf-connecting-ip': '203.0.113.7' },
      payload: { ...valid, turnstileToken: 'tok' },
    });

    expect(res.statusCode).toBe(201);
    const sent = fetchMock.mock.calls[0]![1].body as URLSearchParams;
    expect(sent.get('response')).toBe('tok');
    expect(sent.get('remoteip')).toBe('203.0.113.7');
    expect(prisma.contactRequest.create).toHaveBeenCalledWith({ data: valid });
  });

  it('rejects with 403 when Cloudflare says the token is invalid', async () => {
    vi.stubEnv('TURNSTILE_SECRET_KEY', 'secret');
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ success: false }))));

    const res = await app.inject({ method: 'POST', url: '/api/contacts', payload: { ...valid, turnstileToken: 'bad' } });

    expect(res.statusCode).toBe(403);
  });
});

describe('POST /api/orders', () => {
  const order = { customerName: 'Joan', customerEmail: 'joan@example.com', items: [{ productId: 'p1', quantity: 2 }] };

  it('rejects an order with no items', async () => {
    const res = await app.inject({ method: 'POST', url: '/api/orders', payload: { ...order, items: [] } });
    expect(res.statusCode).toBe(400);
  });

  it('rejects an order for unknown products', async () => {
    const res = await app.inject({ method: 'POST', url: '/api/orders', payload: order });
    expect(res.statusCode).toBe(400);
    expect(prisma.order.create).not.toHaveBeenCalled();
  });

  it('creates an order using the product price', async () => {
    prisma.product.findMany.mockResolvedValue([{ id: 'p1', price: '120.00' }]);
    prisma.customer.upsert.mockResolvedValue({ id: 'cu1' });
    prisma.order.create.mockResolvedValue({ id: 'o1', items: [] });

    const res = await app.inject({ method: 'POST', url: '/api/orders', payload: order });

    expect(res.statusCode).toBe(201);
    const data = prisma.order.create.mock.calls[0]![0].data;
    expect(data.customerId).toBe('cu1');
    expect(data.items.create).toEqual([{ productId: 'p1', quantity: 2, unitPrice: '120.00' }]);
  });
});
