'use client';

import { ProductCard } from '@festpage/ui';
import type { Product, ProductCategory } from '@festpage/types';
import { useT } from '@/i18n';

type Props = {
  products: Product[];
  activeCategory: string;
};

export function ProductsContent({ products, activeCategory }: Props) {
  const { t } = useT();
  const p = t.products;

  const CATEGORIES: Array<{ value: ProductCategory | ''; label: string }> = [
    { value: '', label: p.filters.all },
    { value: 'BED', label: p.filters.beds },
    { value: 'MATTRESS', label: p.filters.mattresses },
    { value: 'PILLOW', label: p.filters.pillows },
    { value: 'ACCESSORY', label: p.filters.accessories },
  ];

  return (
    <div className="mx-auto max-w-6xl px-4 py-12">
      <h1 className="font-display text-4xl font-bold text-brand-blue-900">{p.title}</h1>

      <div className="mt-6 flex flex-wrap gap-2">
        {CATEGORIES.map((cat) => (
          <a
            key={cat.value}
            href={cat.value ? `/products?category=${cat.value}` : '/products'}
            className={[
              'rounded-full px-4 py-1.5 text-sm font-medium transition-colors',
              activeCategory === cat.value
                ? 'bg-brand-blue-700 text-white'
                : 'bg-gray-100 text-gray-600 hover:bg-brand-blue-50 hover:text-brand-blue-700',
            ].join(' ')}
          >
            {cat.label}
          </a>
        ))}
      </div>

      {products.length > 0 ? (
        <div className="mt-8 grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {products.map((product) => (
            <ProductCard key={product.id} product={product} href={`/products/${product.slug}`} />
          ))}
        </div>
      ) : (
        <div className="mt-16 text-center text-gray-400">
          <p className="text-lg">{p.empty}</p>
        </div>
      )}
    </div>
  );
}
