import type { Metadata } from 'next';
import { Suspense } from 'react';
import { ProductsContent } from './_components/ProductsContent';

export const metadata: Metadata = { title: 'Catàleg' };

export default function ProductsPage() {
  return (
    <Suspense>
      <ProductsContent />
    </Suspense>
  );
}
