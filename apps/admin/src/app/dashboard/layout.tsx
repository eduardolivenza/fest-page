'use client';

import { useEffect, useState } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import Link from 'next/link';
import Image from 'next/image';

const NAV = [
  { href: '/dashboard', label: 'Inicio', icon: '🏠' },
  { href: '/dashboard/products', label: 'Productos', icon: '📦' },
  { href: '/dashboard/orders', label: 'Pedidos', icon: '🛒' },
  { href: '/dashboard/contacts', label: 'Contactos', icon: '✉️' },
];

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const token = localStorage.getItem('admin_token');
    if (!token) {
      router.replace('/login');
    } else {
      setReady(true);
    }
  }, [router]);

  function handleLogout() {
    localStorage.removeItem('admin_token');
    router.push('/login');
  }

  if (!ready) return null;

  return (
    <div className="flex min-h-screen">
      {/* Sidebar */}
      <aside className="hidden w-64 flex-col bg-brand-blue-900 text-white md:flex">
        <div className="border-b border-brand-blue-800 px-6 py-5">
          <Image src="/LOGO_fest.png" alt="Fest Descans" width={120} height={48} className="object-contain" />
          <p className="mt-1 text-xs text-brand-blue-300">Admin</p>
        </div>
        <nav className="flex-1 space-y-1 px-3 py-4">
          {NAV.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className={[
                'flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
                pathname === item.href
                  ? 'bg-brand-blue-700 text-white'
                  : 'text-brand-blue-200 hover:bg-brand-blue-800 hover:text-white',
              ].join(' ')}
            >
              <span>{item.icon}</span>
              {item.label}
            </Link>
          ))}
        </nav>
        <div className="border-t border-brand-blue-800 px-3 py-4">
          <button
            onClick={handleLogout}
            className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-brand-blue-200 transition-colors hover:bg-brand-blue-800 hover:text-white"
          >
            <span>🚪</span> Cerrar sesión
          </button>
        </div>
      </aside>

      {/* Main */}
      <div className="flex flex-1 flex-col">
        <header className="border-b border-gray-200 bg-white px-6 py-4 md:hidden">
          <Image src="/Logo.PNG" alt="Fest Descans" width={100} height={40} className="object-contain" />
        </header>
        <main className="flex-1 p-6">{children}</main>
      </div>
    </div>
  );
}
