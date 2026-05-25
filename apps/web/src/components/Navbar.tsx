'use client';

import Link from 'next/link';
import Image from 'next/image';
import { useT } from '@/i18n';
import { LanguageSwitcher } from './LanguageSwitcher';

export function Navbar() {
  const { t } = useT();

  return (
    <header className="sticky top-0 z-40 border-b border-gray-100 bg-white/95 shadow-sm backdrop-blur-sm">
      <nav className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3">
        <Link href="/" className="flex items-center">
          <Image src="/Logo.PNG" alt="Fest Descans" width={180} height={60} priority />
        </Link>
        <div className="flex items-center gap-6">
          <Link
            href="/products"
            className="text-sm font-semibold text-brand-blue-700 transition-colors hover:text-brand-green-500"
          >
            {t.nav.catalog}
          </Link>
          <Link
            href="/contact"
            className="rounded-lg bg-brand-blue-700 px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-brand-blue-800"
          >
            {t.nav.contact}
          </Link>
          <LanguageSwitcher />
        </div>
      </nav>
    </header>
  );
}
