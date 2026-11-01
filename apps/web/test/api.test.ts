import { afterEach, describe, expect, it, vi } from 'vitest';
import { fetchFeaturedProducts, fetchProduct, fetchProducts } from '@/lib/api';

const json = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });

afterEach(() => vi.unstubAllGlobals());

describe('web API helpers', () => {
  it('fetchProducts asks for 24 items of a category', async () => {
    const fetchMock = vi.fn().mockResolvedValue(json({ data: [], total: 0, page: 1, pageSize: 24, totalPages: 0 }));
    vi.stubGlobal('fetch', fetchMock);

    await fetchProducts('MATTRESS');

    const url = new URL(fetchMock.mock.calls[0]![0], 'http://x');
    expect(url.pathname).toBe('/api/products');
    expect(url.searchParams.get('pageSize')).toBe('24');
    expect(url.searchParams.get('category')).toBe('MATTRESS');
  });

  it('fetchProducts returns an empty page when the API fails', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(json({}, 500)));
    expect(await fetchProducts()).toMatchObject({ data: [], total: 0 });
  });

  it('fetchFeaturedProducts returns the product list, or [] on error', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(json({ data: [{ id: 'p1' }] })));
    expect(await fetchFeaturedProducts()).toEqual([{ id: 'p1' }]);

    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(json({}, 503)));
    expect(await fetchFeaturedProducts()).toEqual([]);
  });

  it('fetchProduct returns null for an unknown product', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(json({ message: 'Product not found' }, 404)));
    expect(await fetchProduct('nope')).toBeNull();
  });
});
