'use client';

import { useEffect, useState } from 'react';
import { Badge } from '@festpage/ui';
import { apiFetch } from '@/lib/api';
import type { ContactRequest, PaginatedResponse } from '@festpage/types';

export default function ContactsPage() {
  const [result, setResult] = useState<PaginatedResponse<ContactRequest> | null>(null);

  async function load() {
    const data = await apiFetch<PaginatedResponse<ContactRequest>>('/api/admin/contacts?pageSize=50');
    setResult(data);
  }

  useEffect(() => { load().catch(console.error); }, []);

  async function markRead(id: string) {
    await apiFetch(`/api/admin/contacts/${id}/read`, { method: 'PATCH' });
    await load();
  }

  return (
    <div>
      <h1 className="font-display text-3xl font-bold text-brand-blue-900">Contactos</h1>
      <div className="mt-6 space-y-3">
        {result === null ? (
          <p className="text-gray-400">Cargando…</p>
        ) : result.data.map((c) => (
          <div key={c.id} className={`rounded-2xl border bg-white p-5 shadow-sm transition-colors ${c.read ? 'border-gray-100' : 'border-brand-blue-200'}`}>
            <div className="flex items-start justify-between gap-4">
              <div>
                <div className="flex items-center gap-2">
                  <p className="font-semibold text-brand-blue-900">{c.name}</p>
                  {!c.read && <Badge variant="blue">Nuevo</Badge>}
                </div>
                <p className="text-sm text-gray-500">{c.email}{c.phone ? ` · ${c.phone}` : ''}</p>
                <p className="mt-2 text-gray-700">{c.message}</p>
                <p className="mt-1 text-xs text-gray-400">{new Date(c.createdAt).toLocaleString('es-ES')}</p>
              </div>
              {!c.read && (
                <button
                  onClick={() => markRead(c.id)}
                  className="shrink-0 rounded-lg border border-gray-200 px-3 py-1.5 text-xs font-medium text-gray-600 hover:bg-gray-50"
                >
                  Marcar leído
                </button>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
