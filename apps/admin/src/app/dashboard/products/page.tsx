'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { Badge, Button } from '@festpage/ui';
import { apiFetch } from '@/lib/api';
import type { Product, PaginatedResponse } from '@festpage/types';

const CATEGORY_LABELS: Record<Product['category'], string> = {
  BED: 'Cama', MATTRESS: 'Colchón', PILLOW: 'Almohada', ACCESSORY: 'Accesorio',
};

export default function ProductsPage() {
  const [result, setResult] = useState<PaginatedResponse<Product> | null>(null);
  const [deleting, setDeleting] = useState<string | null>(null);

  async function load() {
    const data = await apiFetch<PaginatedResponse<Product>>('/api/admin/products?pageSize=50');
    setResult(data);
  }

  useEffect(() => { load().catch(console.error); }, []);

  async function handleDelete(id: string) {
    if (!confirm('¿Eliminar este producto?')) return;
    setDeleting(id);
    await apiFetch(`/api/admin/products/${id}`, { method: 'DELETE' });
    await load();
    setDeleting(null);
  }

  return (
    <div>
      <div className="flex items-center justify-between">
        <h1 className="font-display text-3xl font-bold text-brand-blue-900">Productos</h1>
        <Link href="/dashboard/products/new">
          <Button size="sm">+ Nuevo producto</Button>
        </Link>
      </div>

      <div className="mt-6 overflow-hidden rounded-2xl border border-gray-100 bg-white shadow-sm">
        <table className="w-full text-sm">
          <thead className="border-b border-gray-100 bg-gray-50 text-left text-xs font-semibold uppercase tracking-wider text-gray-500">
            <tr>
              <th className="px-4 py-3">Nombre</th>
              <th className="px-4 py-3">Categoría</th>
              <th className="px-4 py-3">Precio</th>
              <th className="px-4 py-3">Destacado</th>
              <th className="px-4 py-3"></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-50">
            {result === null ? (
              <tr><td colSpan={5} className="px-4 py-8 text-center text-gray-400">Cargando…</td></tr>
            ) : result.data.map((p) => (
              <tr key={p.id} className="hover:bg-gray-50">
                <td className="px-4 py-3 font-medium text-brand-blue-900">{p.name}</td>
                <td className="px-4 py-3"><Badge variant="blue">{CATEGORY_LABELS[p.category]}</Badge></td>
                <td className="px-4 py-3">{p.price.toLocaleString('es-ES', { style: 'currency', currency: 'EUR' })}</td>
                <td className="px-4 py-3">{p.featured ? <Badge variant="green">Sí</Badge> : <Badge variant="gray">No</Badge>}</td>
                <td className="px-4 py-3">
                  <div className="flex gap-2">
                    <Link href={`/dashboard/products/${p.id}/edit`} className="text-brand-blue-600 hover:underline">Editar</Link>
                    <button
                      onClick={() => handleDelete(p.id)}
                      disabled={deleting === p.id}
                      className="text-red-500 hover:underline disabled:opacity-40"
                    >
                      Eliminar
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
