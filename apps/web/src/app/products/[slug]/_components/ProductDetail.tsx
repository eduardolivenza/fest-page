'use client';

import { useState, useEffect } from 'react';
import { useParams } from 'next/navigation';
import { notFound } from 'next/navigation';
import type { Product } from '@festpage/types';
import { Badge } from '@festpage/ui';
import { useT } from '@/i18n';
import { fetchProduct } from '@/lib/api';

export function ProductDetail() {
  const { slug } = useParams<{ slug: string }>();
  const { t, locale } = useT();
  const p = t.product;
  const priceLocale = locale === 'ca' ? 'ca-ES' : 'es-ES';
  const [product, setProduct] = useState<Product | null>(null);
  const [loading, setLoading] = useState(true);
  const [isNotFound, setIsNotFound] = useState(false);

  useEffect(() => {
    fetchProduct(slug)
      .then((data) => {
        if (!data) setIsNotFound(true);
        else setProduct(data);
      })
      .catch(() => setIsNotFound(true))
      .finally(() => setLoading(false));
  }, [slug]);

  if (loading) {
    return (
      <div className="mx-auto max-w-6xl px-4 py-12">
        <div className="grid gap-12 lg:grid-cols-2">
          <div className="animate-pulse rounded-2xl bg-gray-100 aspect-[4/3]" />
          <div className="space-y-4">
            <div className="animate-pulse h-6 w-24 rounded-full bg-gray-100" />
            <div className="animate-pulse h-10 w-3/4 rounded-lg bg-gray-100" />
            <div className="animate-pulse h-4 w-full rounded bg-gray-100" />
            <div className="animate-pulse h-4 w-5/6 rounded bg-gray-100" />
          </div>
        </div>
      </div>
    );
  }

  if (isNotFound || !product) notFound();

  const images = product.images;
  const mainImage = images[0];

  return (
    <div className="mx-auto max-w-6xl px-4 py-12">
      <div className="grid gap-12 lg:grid-cols-2">
        {/* Images */}
        <div className="space-y-4">
          <div className="aspect-[4/3] overflow-hidden rounded-2xl bg-gray-100">
            {mainImage ? (
              <img src={mainImage.url} alt={mainImage.alt ?? product.name} className="h-full w-full object-cover" />
            ) : (
              <div className="flex h-full items-center justify-center text-6xl text-gray-300">🛏️</div>
            )}
          </div>
          {images.length > 1 && (
            <div className="flex gap-2 overflow-x-auto">
              {images.map((img) => (
                <div key={img.id} className="h-20 w-20 flex-shrink-0 overflow-hidden rounded-lg bg-gray-100">
                  <img src={img.url} alt={img.alt ?? ''} className="h-full w-full object-cover" />
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Details */}
        <div className="flex flex-col">
          <Badge variant="blue">{p.categories[product.category]}</Badge>
          <h1 className="mt-2 font-display text-4xl font-bold text-brand-blue-900">{product.name}</h1>
          {product.description && (
            <p className="mt-4 text-lg leading-relaxed text-gray-600">{product.description}</p>
          )}

          <div className="mt-6 flex items-center gap-4">
            <span className="text-4xl font-bold text-brand-blue-800">
              {(product.sizes ?? []).length > 0
                ? `${p.from} ${Math.min(...(product.sizes ?? []).map((s) => s.price)).toLocaleString(priceLocale, { style: 'currency', currency: 'EUR' })}`
                : product.price.toLocaleString(priceLocale, { style: 'currency', currency: 'EUR' })}
            </span>
          </div>

          {(product.sizes ?? []).length > 0 && (
            <div className="mt-6">
              <p className="text-sm font-medium text-gray-700">{p.sizes}</p>
              <div className="mt-2 overflow-hidden rounded-xl border border-gray-100">
                <table className="w-full text-sm">
                  <thead className="bg-gray-50 text-xs font-semibold uppercase tracking-wider text-gray-500">
                    <tr>
                      <th className="px-4 py-2 text-left">{p.sizeCol}</th>
                      <th className="px-4 py-2 text-right">{p.priceCol}</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-50">
                    {(product.sizes ?? []).map((size) => (
                      <tr key={size.id} className="hover:bg-gray-50">
                        <td className="px-4 py-2 font-medium text-brand-blue-900">
                          {size.width} × {size.length} cm
                        </td>
                        <td className="px-4 py-2 text-right font-semibold text-brand-blue-800">
                          {size.price.toLocaleString(priceLocale, { style: 'currency', currency: 'EUR' })}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          <div className="mt-8 space-y-3">
            <a
              href="/contact"
              className="block w-full rounded-xl bg-brand-blue-700 px-6 py-4 text-center font-semibold text-white transition-colors hover:bg-brand-blue-800"
            >
              {p.cta}
            </a>
            <p className="text-center text-sm text-gray-500">{p.visitStore}</p>
          </div>
        </div>
      </div>
    </div>
  );
}
