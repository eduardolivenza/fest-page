'use client';

import { useT } from '@/i18n';

export function LanguageSwitcher() {
  const { locale, setLocale } = useT();

  return (
    <div className="flex items-center gap-1 text-xs font-bold tracking-wider">
      <button
        onClick={() => setLocale('ca')}
        className={
          locale === 'ca'
            ? 'text-brand-blue-700'
            : 'text-gray-400 transition-colors hover:text-gray-600'
        }
      >
        CA
      </button>
      <span className="text-gray-300">|</span>
      <button
        onClick={() => setLocale('es')}
        className={
          locale === 'es'
            ? 'text-brand-blue-700'
            : 'text-gray-400 transition-colors hover:text-gray-600'
        }
      >
        ES
      </button>
    </div>
  );
}
