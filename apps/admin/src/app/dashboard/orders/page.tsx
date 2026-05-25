'use client';

import { useEffect, useState } from 'react';
import { Badge } from '@festpage/ui';
import { apiFetch } from '@/lib/api';
import type { Order, PaginatedResponse, OrderStatus } from '@festpage/types';

const STATUS_LABEL: Record<OrderStatus, string> = {
  PENDING: 'Pendiente', CONFIRMED: 'Confirmado', DELIVERED: 'Entregado', CANCELLED: 'Cancelado',
};
const STATUS_VARIANT: Record<OrderStatus, 'gray' | 'blue' | 'green' | 'red'> = {
  PENDING: 'gray', CONFIRMED: 'blue', DELIVERED: 'green', CANCELLED: 'red',
};

export default function OrdersPage() {
  const [result, setResult] = useState<PaginatedResponse<Order> | null>(null);

  useEffect(() => {
    apiFetch<PaginatedResponse<Order>>('/api/admin/orders?pageSize=50')
      .then(setResult)
      .catch(console.error);
  }, []);

  async function updateStatus(id: string, status: OrderStatus) {
    await apiFetch(`/api/admin/orders/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    });
    const data = await apiFetch<PaginatedResponse<Order>>('/api/admin/orders?pageSize=50');
    setResult(data);
  }

  return (
    <div>
      <h1 className="font-display text-3xl font-bold text-brand-blue-900">Pedidos</h1>
      <div className="mt-6 overflow-hidden rounded-2xl border border-gray-100 bg-white shadow-sm">
        <table className="w-full text-sm">
          <thead className="border-b border-gray-100 bg-gray-50 text-left text-xs font-semibold uppercase tracking-wider text-gray-500">
            <tr>
              <th className="px-4 py-3">ID</th>
              <th className="px-4 py-3">Cliente</th>
              <th className="px-4 py-3">Items</th>
              <th className="px-4 py-3">Estado</th>
              <th className="px-4 py-3">Fecha</th>
              <th className="px-4 py-3">Cambiar estado</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-50">
            {result === null ? (
              <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-400">Cargando…</td></tr>
            ) : result.data.map((o) => (
              <tr key={o.id} className="hover:bg-gray-50">
                <td className="px-4 py-3 font-mono text-xs text-gray-500">{o.id.slice(0, 8)}</td>
                <td className="px-4 py-3">
                  <p className="font-medium">{o.customer.name}</p>
                  <p className="text-gray-400">{o.customer.email}</p>
                </td>
                <td className="px-4 py-3">{o.items.length}</td>
                <td className="px-4 py-3"><Badge variant={STATUS_VARIANT[o.status]}>{STATUS_LABEL[o.status]}</Badge></td>
                <td className="px-4 py-3 text-gray-500">{new Date(o.createdAt).toLocaleDateString('es-ES')}</td>
                <td className="px-4 py-3">
                  <select
                    value={o.status}
                    onChange={(e) => updateStatus(o.id, e.target.value as OrderStatus)}
                    className="rounded-lg border border-gray-200 px-2 py-1 text-xs"
                  >
                    {Object.entries(STATUS_LABEL).map(([val, label]) => (
                      <option key={val} value={val}>{label}</option>
                    ))}
                  </select>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
