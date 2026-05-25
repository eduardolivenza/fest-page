import type { Metadata } from 'next';
import { notFound } from 'next/navigation';
import type { Product } from '@festpage/types';
import { ProductDetail } from './_components/ProductDetail';

async function getProduct(slug: string): Promise<Product | null> {
  const res = await fetch(`${process.env['NEXT_PUBLIC_API_URL']}/api/products/${slug}`, {
    next: { revalidate: 60 },
  });
  if (res.status === 404) return null;
  if (!res.ok) return null;
  return res.json();
}

export async function generateMetadata({ params }: { params: Promise<{ slug: string }> }): Promise<Metadata> {
  const { slug } = await params;
  const product = await getProduct(slug);
  if (!product) return { title: 'Producte no trobat' };
  return { title: product.name, description: product.description ?? undefined };
}

export default async function ProductPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  const product = await getProduct(slug);

  if (!product) notFound();

  return <ProductDetail product={product} />;
}
