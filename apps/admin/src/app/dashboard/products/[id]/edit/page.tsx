'use client';

import { useEffect, useState } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { Button } from '@festpage/ui';
import { apiFetch } from '@/lib/api';
import { SizesEditor, SIZED_CATEGORIES } from '@/components/SizesEditor';
import type { Product } from '@festpage/types';

export default function EditProductPage() {
  const router = useRouter();
  const { id } = useParams<{ id: string }>();
  const [product, setProduct] = useState<Product | null>(null);
  const [category, setCategory] = useState('BED');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    apiFetch<Product>(`/api/admin/products/${id}`)
      .then((p) => {
        setProduct(p);
        setCategory(p.category);
      })
      .catch(() => setError('No se pudo cargar el producto'));
  }, [id]);

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setSaving(true);
    setError('');

    const fd = new FormData(e.currentTarget);
    const rawSizes = fd.get('sizes') as string;
    const sizes = SIZED_CATEGORIES.includes(category) ? JSON.parse(rawSizes) : [];

    const body = {
      name: fd.get('name') as string,
      slug: fd.get('slug') as string,
      description: (fd.get('description') as string) || undefined,
      price: parseFloat(fd.get('price') as string),
      category,
      featured: fd.get('featured') === 'on',
      sizes,
    };

    try {
      await apiFetch(`/api/admin/products/${id}`, { method: 'PUT', body: JSON.stringify(body) });
      router.push('/dashboard/products');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al guardar');
    } finally {
      setSaving(false);
    }
  }

  if (!product && !error) return <p className="text-gray-400">Cargando…</p>;
  if (error && !product) return <p className="text-red-600">{error}</p>;

  return (
    <div className="mx-auto max-w-xl">
      <h1 className="font-display text-3xl font-bold text-brand-blue-900">Editar producto</h1>

      <form onSubmit={handleSubmit} className="mt-8 space-y-5 rounded-2xl border border-gray-100 bg-white p-6 shadow-sm">
        <div>
          <label className="block text-sm font-medium text-gray-700" htmlFor="name">Nombre</label>
          <input
            id="name" name="name" type="text" required defaultValue={product!.name}
            className="mt-1 w-full rounded-lg border border-gray-200 px-3 py-2.5 outline-none focus:border-brand-blue-500 focus:ring-2 focus:ring-brand-blue-200"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700" htmlFor="slug">Slug (URL)</label>
          <input
            id="slug" name="slug" type="text" required pattern="[a-z0-9-]+" defaultValue={product!.slug}
            className="mt-1 w-full rounded-lg border border-gray-200 px-3 py-2.5 outline-none focus:border-brand-blue-500 focus:ring-2 focus:ring-brand-blue-200"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700" htmlFor="category">Categoría</label>
          <select
            id="category" name="category" required
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            className="mt-1 w-full rounded-lg border border-gray-200 px-3 py-2.5 outline-none focus:border-brand-blue-500"
          >
            <option value="BED">Cama</option>
            <option value="MATTRESS">Colchón</option>
            <option value="PILLOW">Almohada</option>
            <option value="ACCESSORY">Accesorio</option>
          </select>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700" htmlFor="price">Precio base (€)</label>
          <input
            id="price" name="price" type="number" required step="0.01" min="0" defaultValue={product!.price}
            className="mt-1 w-full rounded-lg border border-gray-200 px-3 py-2.5 outline-none focus:border-brand-blue-500 focus:ring-2 focus:ring-brand-blue-200"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700" htmlFor="description">Descripción</label>
          <textarea
            id="description" name="description" rows={4} defaultValue={product!.description ?? ''}
            className="mt-1 w-full rounded-lg border border-gray-200 px-3 py-2.5 outline-none focus:border-brand-blue-500"
          />
        </div>

        <label className="flex items-center gap-2 text-sm font-medium text-gray-700">
          <input type="checkbox" name="featured" defaultChecked={product!.featured} className="rounded" />
          Producto destacado
        </label>

        {SIZED_CATEGORIES.includes(category) && (
          <div className="rounded-xl border border-brand-blue-100 bg-brand-blue-50 p-4">
            <SizesEditor initial={product!.sizes.map(({ width, length, price }) => ({ width, length, price }))} />
          </div>
        )}

        {error && <p className="text-sm text-red-600">{error}</p>}

        <div className="flex gap-3">
          <Button type="submit" loading={saving}>Guardar cambios</Button>
          <Button type="button" variant="outline" onClick={() => router.back()}>Cancelar</Button>
        </div>
      </form>
    </div>
  );
}
