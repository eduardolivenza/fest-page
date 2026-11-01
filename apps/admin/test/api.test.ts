import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { apiFetch } from '@/lib/api';

const json = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });

let fetchMock: ReturnType<typeof vi.fn>;

beforeEach(() => {
  fetchMock = vi.fn().mockImplementation(async () => json({ ok: true }));
  vi.stubGlobal('fetch', fetchMock);
  localStorage.clear();
});

afterEach(() => vi.unstubAllGlobals());

describe('apiFetch', () => {
  it('sends the stored admin token as a Bearer header', async () => {
    localStorage.setItem('admin_token', 'jwt-123');

    await apiFetch('/api/admin/contacts');

    expect(fetchMock.mock.calls[0]![1].headers).toMatchObject({ Authorization: 'Bearer jwt-123' });
  });

  it('only sets a JSON content type when there is a body', async () => {
    await apiFetch('/api/admin/contacts/c1', { method: 'DELETE' });
    expect(fetchMock.mock.calls[0]![1].headers).not.toHaveProperty('Content-Type');

    await apiFetch('/api/admin/products', { method: 'POST', body: '{}' });
    expect(fetchMock.mock.calls[1]![1].headers).toMatchObject({ 'Content-Type': 'application/json' });
  });

  it('returns undefined for 204 No Content', async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }));
    expect(await apiFetch('/api/admin/contacts/c1', { method: 'DELETE' })).toBeUndefined();
  });

  it('throws the API error message', async () => {
    fetchMock.mockResolvedValue(json({ message: 'No se puede eliminar' }, 409));
    await expect(apiFetch('/api/admin/products/p1', { method: 'DELETE' })).rejects.toThrow('No se puede eliminar');
  });

  it('on 401 drops the token and sends the user to /login', async () => {
    localStorage.setItem('admin_token', 'expired');
    fetchMock.mockResolvedValue(json({ message: 'Unauthorized' }, 401));
    const location = { href: '/dashboard' };
    vi.stubGlobal('location', location);

    await expect(apiFetch('/api/admin/contacts')).rejects.toThrow('Unauthorized');

    expect(localStorage.getItem('admin_token')).toBeNull();
    expect(location.href).toBe('/login');
  });
});
