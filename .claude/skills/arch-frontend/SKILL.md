---
name: arch-frontend
description: FestPage frontends (apps/web public site and apps/admin back office) — page structure, how data is fetched from the API, shared UI package, styling tokens, logos, Turnstile widget and frontend tests. Use when adding or changing a page or component in web or admin.
---

# FestPage — Frontends (`apps/web`, `apps/admin`)

Next.js App Router (Turbopack), React 19, Tailwind CSS 3, `output: 'standalone'` (Docker runs `apps/<app>/server.js`).

## Data fetching: client-side

Both apps fetch the API **from the browser**, in client components (`useEffect` + `useState`), through one helper per app:

- web: `src/lib/api.ts` (`fetchProducts`, `fetchFeaturedProducts`, `fetchProduct`): returns an empty page / `[]` / `null` on any non-OK response, never throws for HTTP errors.
- admin: `src/lib/api.ts` → `apiFetch<T>(path, init)`: adds `Authorization: Bearer <admin_token>`, sets JSON content type only when there is a body, returns `undefined` for 204, throws `Error(message)` with the API's message, and on **401 clears the token and redirects to `/login`**.

New API calls go through these helpers, not raw `fetch` in components. `NEXT_PUBLIC_API_URL` is inlined at **build time** (CI build arg), so changing it needs a new image.

## web structure

```
src/app/
  layout.tsx                 server: metadata (Catalan), <LanguageProvider>, Navbar, Footer
  page.tsx                   server wrapper → _components/HomeContent.tsx ('use client')
  products/page.tsx          server: metadata + <Suspense> → _components/ProductsContent.tsx (useSearchParams)
  products/[slug]/page.tsx   server wrapper → _components/ProductDetail.tsx (useParams)
  contact/page.tsx           'use client' (form + Turnstile)
src/components/  Navbar, Footer, LanguageSwitcher, Turnstile ('use client', they use useT())
src/i18n/        messages.ts + LanguageProvider/useT (see arch-i18n)
public/.well-known/security.txt  (expires 2027-10-31, renew yearly)
```

Pattern for a page: `page.tsx` stays a **server component** that only exports `metadata` and renders a client component from `_components/` that fetches and translates. A component using `useSearchParams` must be inside `<Suspense>`.

## admin structure

Everything under `/dashboard` is client-side. `dashboard/layout.tsx` checks `localStorage.admin_token` and redirects to `/login` if missing (the API still enforces auth on every call). Pages: dashboard, products (list/new/[id]/edit with `SizesEditor`), orders, contacts. UI language: Spanish, hardcoded (no i18n in admin). Destructive actions use `confirm()` + disabled button while running (see products/contacts delete).

## Shared UI and styling

- `@festpage/ui` (`packages/ui`): `Button` (variants, `loading`), `Badge`, `Card`, `ProductCard`, `Spinner`. Prefer these over new one-off components; add to the package when used by both apps.
- `@festpage/types`: shared API shapes (`Product`, `PaginatedResponse`, `ContactRequest`…), must match what the API returns.
- Tailwind brand tokens in `tailwind.config.ts`: `brand-blue-*` (navy, #1B3566) and `brand-green-*` (lime, #8CC63F); fonts via Google Fonts `@import` at the **top** of `globals.css` (Turbopack rejects it after `@tailwind`).
- Logos in `apps/web/public/`: `Logo.PNG` for the navbar (light background), `LOGO_fest.png` for the footer (dark background; don't apply `brightness-0 invert`).
- Images: `next/image` with `remotePatterns` allowing any https host (product images are external URLs).

## Turnstile

`src/components/Turnstile.tsx` (explicit render, `forwardRef` with `reset()`). Rendered only when `NEXT_PUBLIC_TURNSTILE_SITE_KEY` is set (CI default = festdescans.com widget). The submit button stays disabled until a token arrives; reset the widget after a failed submit (tokens are single-use).

## Tests

`apps/<app>/test/*.test.ts(x)`, Vitest + jsdom + Testing Library, `pnpm --filter @festpage/<app> test`. `test/setup.ts` restores jsdom's `localStorage` (Node 25+ has its own) — keep it. Mock `fetch` with `vi.stubGlobal` and return a **new** `Response` per call (bodies are single-use). Pages reading env at import time need `vi.resetModules()` + dynamic `import()` after `vi.stubEnv`. New pages/components with behaviour (forms, API calls, destructive actions) get a test.
