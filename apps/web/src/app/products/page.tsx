import type { Metadata } from 'next';
import type { Product, PaginatedResponse } from '@festpage/types';
import { ProductsContent } from './_components/ProductsContent';

export const metadata: Metadata = { title: 'Catàleg' };

async function getProducts(category?: string): Promise<PaginatedResponse<Product>> {
  try {
    const params = new URLSearchParams({ pageSize: '24' });
    if (category) params.set('category', category);

    const res = await fetch(
      `${process.env['NEXT_PUBLIC_API_URL']}/api/products?${params.toString()}`,
      { next: { revalidate: 30 } },
    );
    if (!res.ok) return { data: [], total: 0, page: 1, pageSize: 24, totalPages: 0 };
    return res.json();
  } catch {
    return { data: [], total: 0, page: 1, pageSize: 24, totalPages: 0 };
  }
}

export default async function ProductsPage({
  searchParams,
}: {
  searchParams: Promise<{ category?: string }>;
}) {
  const params = await searchParams;
  const activeCategory = params.category ?? '';
  const result = await getProducts(activeCategory || undefined);

  return <ProductsContent products={result.data} activeCategory={activeCategory} />;
}
