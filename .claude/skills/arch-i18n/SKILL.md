---
name: arch-i18n
description: How Catalan/Spanish translations work in the FestPage public site (apps/web) — the single messages file, the useT() hook, the language switcher, and the trade-offs (no URL locales, Catalan metadata). Use when adding or changing any user-visible text in apps/web.
---

# FestPage — i18n Architecture

Languages supported: **Catalan (ca)** — default, **Spanish (es)**.

## Single source of truth

All translatable strings live in **one file**:

```
apps/web/src/i18n/messages.ts
```

Structure mirrors the page/section hierarchy:
```
messages.ca / messages.es
  meta        — page <title> and description
  nav         — navbar links
  footer      — footer sections
  home        — hero, categories, featured, gallery, brandStrip
  contact     — labels, hours, form, map
  products    — title, filters, empty state
  product     — category labels, stock, sizes, CTA
```

Adding a new string: add it to **both** `ca` and `es` objects in `messages.ts`. The `Messages` type is the Catalan shape, so missing Spanish keys are not always a type error: the test `apps/web/test/messages.test.ts` checks that both languages have exactly the same keys and no empty strings. Run `pnpm --filter @festpage/web test`.

## Context + hook

```
apps/web/src/i18n/index.tsx
```

- `<LanguageProvider>` — wraps the entire app in `layout.tsx`. Reads locale from `localStorage` (`lang`) on mount. Updates `<html lang>` attribute reactively.
- `useT()` — returns `{ t, locale, setLocale }`.
  - `t` is typed as `Messages` (the Catalan shape), so all keys are autocompleted.
  - `locale` is `'ca' | 'es'`.

## Language switcher

`apps/web/src/components/LanguageSwitcher.tsx` — rendered inside the Navbar, top-right.  
Calls `setLocale('ca' | 'es')`, which updates context + localStorage.

## Default language

**Catalan** (`'ca'`) is the default on first visit and in `<html lang>`.  
The `layout.tsx` metadata is also written in Catalan.

## Limitations (by design)

- **No URL-based routing** (`/ca/`, `/es/`). Language is purely client-side.  
  Consequence: SEO metadata is always in Catalan; page titles don't change per language.
- **No SSR language detection** (no cookie on first render). The initial render is always Catalan; the correct language loads after hydration if the user had a stored preference.
- These are intentional trade-offs for simplicity. If URL-based i18n is needed in the future, migrate to `next-intl` (Next 16 calls middleware `proxy`).

## How to use in a new component

```tsx
'use client';
import { useT } from '@/i18n';

export function MyComponent() {
  const { t } = useT();
  return <h1>{t.nav.catalog}</h1>;
}
```

Server components cannot use `useT()`. Pass translated strings as props from a client wrapper, or keep server-only text hardcoded in Catalan.

The admin app is Spanish-only and does not use this system.
