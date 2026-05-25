'use client';

import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import { messages, type Locale, type Messages } from './messages';

type LanguageCtx = {
  locale: Locale;
  setLocale: (l: Locale) => void;
  t: typeof messages.ca;
};

const LanguageContext = createContext<LanguageCtx | null>(null);

export function LanguageProvider({ children }: { children: ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>('ca');

  useEffect(() => {
    const saved = localStorage.getItem('lang') as Locale | null;
    if (saved === 'ca' || saved === 'es') setLocaleState(saved);
  }, []);

  useEffect(() => {
    document.documentElement.lang = locale;
  }, [locale]);

  function setLocale(l: Locale) {
    setLocaleState(l);
    localStorage.setItem('lang', l);
  }

  return (
    <LanguageContext.Provider value={{ locale, setLocale, t: messages[locale] as Messages }}>
      {children}
    </LanguageContext.Provider>
  );
}

export function useT() {
  const ctx = useContext(LanguageContext);
  if (!ctx) throw new Error('useT must be used inside LanguageProvider');
  return ctx;
}

export type { Locale };
