import type { Product, PaginatedResponse } from '@festpage/types';
import { HomeContent } from './_components/HomeContent';

async function getFeaturedProducts(): Promise<Product[]> {
  try {
    const res = await fetch(
      `${process.env['NEXT_PUBLIC_API_URL']}/api/products?featured=true&pageSize=6`,
      { next: { revalidate: 60 } },
    );
    if (!res.ok) return [];
    const json: PaginatedResponse<Product> = await res.json();
    return json.data;
  } catch {
    return [];
  }
}

export default async function HomePage() {
  const featured = await getFeaturedProducts();
  return <HomeContent featured={featured} />;
}
