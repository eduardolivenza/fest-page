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

Adding a new string: add it to **both** `ca` and `es` objects in `messages.ts`. TypeScript enforces structural parity via `as const` + `Messages` type alias.

## Context + hook

```
apps/web/src/i18n/index.tsx
```

- `<LanguageProvider>` — wraps the entire app in `layout.tsx`. Reads locale from `localStorage` on mount. Updates `<html lang>` attribute reactively.
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
- These are intentional trade-offs for simplicity. If URL-based i18n is needed in the future, migrate to `next-intl` with middleware.

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
