import type { Product, PaginatedResponse } from '@festpage/types';

const API_URL = process.env['NEXT_PUBLIC_API_URL'] ?? '';

const EMPTY_PAGE: PaginatedResponse<Product> = { data: [], total: 0, page: 1, pageSize: 24, totalPages: 0 };

export async function fetchFeaturedProducts(): Promise<Product[]> {
  const res = await fetch(`${API_URL}/api/products?featured=true&pageSize=6`);
  if (!res.ok) return [];
  const json: PaginatedResponse<Product> = await res.json();
  return json.data;
}

export async function fetchProducts(category?: string): Promise<PaginatedResponse<Product>> {
  const params = new URLSearchParams({ pageSize: '24' });
  if (category) params.set('category', category);
  const res = await fetch(`${API_URL}/api/products?${params.toString()}`);
  if (!res.ok) return EMPTY_PAGE;
  return res.json();
}

export async function fetchProduct(slug: string): Promise<Product | null> {
  const res = await fetch(`${API_URL}/api/products/${slug}`);
  if (res.status === 404) return null;
  if (!res.ok) return null;
  return res.json();
}
