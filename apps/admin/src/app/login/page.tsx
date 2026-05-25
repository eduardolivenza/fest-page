'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Image from 'next/image';
import { Button } from '@festpage/ui';

export default function LoginPage() {
  const router = useRouter();
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setLoading(true);
    setError('');

    const formData = new FormData(e.currentTarget);

    try {
      const res = await fetch(`${process.env['NEXT_PUBLIC_API_URL']}/api/admin/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email: formData.get('email'),
          password: formData.get('password'),
        }),
      });

      if (!res.ok) {
        setError('Credenciales incorrectas');
        return;
      }

      const { token } = await res.json();
      localStorage.setItem('admin_token', token);
      router.push('/dashboard');
    } catch {
      setError('Error de conexión');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-brand-blue-950 px-4">
      <div className="w-full max-w-sm rounded-2xl bg-white p-8 shadow-xl">
        <Image src="/Logo.PNG" alt="Fest Descans" width={140} height={56} className="object-contain" />
        <p className="mt-2 text-sm text-gray-500">Panel de administración</p>

        <form onSubmit={handleSubmit} className="mt-8 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700" htmlFor="email">Email</label>
            <input
              id="email" name="email" type="email" required autoComplete="email"
              className="mt-1 w-full rounded-lg border border-gray-200 px-3 py-2.5 outline-none focus:border-brand-blue-500 focus:ring-2 focus:ring-brand-blue-200"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700" htmlFor="password">Contraseña</label>
            <input
              id="password" name="password" type="password" required autoComplete="current-password"
              className="mt-1 w-full rounded-lg border border-gray-200 px-3 py-2.5 outline-none focus:border-brand-blue-500 focus:ring-2 focus:ring-brand-blue-200"
            />
          </div>
          {error && <p className="text-sm text-red-600">{error}</p>}
          <Button type="submit" loading={loading} className="mt-2 w-full">
            Entrar
          </Button>
        </form>
      </div>
    </div>
  );
}
