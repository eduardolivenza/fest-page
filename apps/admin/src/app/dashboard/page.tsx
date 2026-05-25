'use client';

import { useEffect, useState } from 'react';
import { Card, CardBody } from '@festpage/ui';
import { apiFetch } from '@/lib/api';
import type { PaginatedResponse, Order, ContactRequest, Product } from '@festpage/types';

interface Stats {
  totalProducts: number;
  pendingOrders: number;
  unreadContacts: number;
}

export default function DashboardPage() {
  const [stats, setStats] = useState<Stats | null>(null);

  useEffect(() => {
    async function load() {
      const [products, orders, contacts] = await Promise.all([
        apiFetch<PaginatedResponse<Product>>('/api/admin/products?pageSize=1'),
        apiFetch<PaginatedResponse<Order>>('/api/admin/orders?status=PENDING&pageSize=1'),
        apiFetch<PaginatedResponse<ContactRequest>>('/api/admin/contacts?read=false&pageSize=1'),
      ]);
      setStats({
        totalProducts: products.total,
        pendingOrders: orders.total,
        unreadContacts: contacts.total,
      });
    }
    load().catch(console.error);
  }, []);

  const tiles = [
    { label: 'Productos', value: stats?.totalProducts, icon: '📦', href: '/dashboard/products' },
    { label: 'Pedidos pendientes', value: stats?.pendingOrders, icon: '🛒', href: '/dashboard/orders' },
    { label: 'Contactos sin leer', value: stats?.unreadContacts, icon: '✉️', href: '/dashboard/contacts' },
  ];

  return (
    <div>
      <h1 className="font-display text-3xl font-bold text-brand-blue-900">Resumen</h1>
      <div className="mt-6 grid gap-5 sm:grid-cols-3">
        {tiles.map((tile) => (
          <a key={tile.label} href={tile.href}>
            <Card className="hover:border-brand-blue-200 hover:shadow-md transition-all">
              <CardBody className="flex items-center gap-4">
                <span className="text-4xl">{tile.icon}</span>
                <div>
                  <p className="text-sm text-gray-500">{tile.label}</p>
                  <p className="text-3xl font-bold text-brand-blue-800">
                    {stats === null ? '—' : tile.value}
                  </p>
                </div>
              </CardBody>
            </Card>
          </a>
        ))}
      </div>
    </div>
  );
}
