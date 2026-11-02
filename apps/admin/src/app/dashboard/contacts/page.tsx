'use client';

import { useEffect, useState } from 'react';
import { Badge } from '@festpage/ui';
import { apiFetch } from '@/lib/api';
import type { ContactRequest, PaginatedResponse } from '@festpage/types';

export default function ContactsPage() {
  const [result, setResult] = useState<PaginatedResponse<ContactRequest> | null>(null);
  const [deleting, setDeleting] = useState<string | null>(null);

  async function load() {
    const data = await apiFetch<PaginatedResponse<ContactRequest>>('/api/admin/contacts?pageSize=50');
    setResult(data);
  }

  useEffect(() => { load().catch(console.error); }, []);

  async function markRead(id: string) {
    await apiFetch(`/api/admin/contacts/${id}/read`, { method: 'PATCH' });
    await load();
  }

  async function handleDelete(id: string) {
    if (!confirm('¿Eliminar este mensaje?')) return;
    setDeleting(id);
    try {
      await apiFetch(`/api/admin/contacts/${id}`, { method: 'DELETE' });
      await load();
    } catch (err) {
      alert(err instanceof Error ? err.message : 'No se pudo eliminar el mensaje');
    } finally {
      setDeleting(null);
    }
  }

  return (
    <div>
      <h1 className="font-display text-3xl font-bold text-brand-blue-900">Contactos</h1>
      <div className="mt-6 space-y-3">
        {result === null ? (
          <p className="text-gray-400">Cargando…</p>
        ) : result.data.map((c) => (
          <div key={c.id} className={`rounded-2xl border bg-white p-5 shadow-xs transition-colors ${c.read ? 'border-gray-100' : 'border-brand-blue-200'}`}>
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
              <div className="flex shrink-0 gap-2">
                {!c.read && (
                  <button
                    onClick={() => markRead(c.id)}
                    className="rounded-lg border border-gray-200 px-3 py-1.5 text-xs font-medium text-gray-600 hover:bg-gray-50"
                  >
                    Marcar leído
                  </button>
                )}
                <button
                  onClick={() => handleDelete(c.id)}
                  disabled={deleting === c.id}
                  className="rounded-lg border border-red-200 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50 disabled:opacity-40"
                >
                  Eliminar
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
