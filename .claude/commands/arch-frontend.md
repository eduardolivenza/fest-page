# FestPage — Frontend Architecture

App: `apps/web` — Next.js 15, App Router, React 19, Tailwind CSS.

## Server vs Client component split

Pages that **fetch data AND render translatable UI** follow this pattern:

```
app/products/page.tsx                   ← server component
  └── _components/ProductsContent.tsx  ← 'use client', uses useT()

app/products/[slug]/page.tsx
  └── _components/ProductDetail.tsx

app/page.tsx                            ← server fetches featured products
  └── _components/HomeContent.tsx
```

**Rule:** server components own `export const metadata`, `generateMetadata`, and all `fetch` calls. Client components (in `_components/`) own rendering and translations.

Pages that are already `'use client'` (like `contact/page.tsx`) stay as-is and call `useT()` directly.

Navbar and Footer are `'use client'` because they use `useT()`.

## API fetch pattern

Every `fetch` inside a server component must have `try/catch`:

```typescript
async function getData(): Promise<Result> {
  try {
    const res = await fetch(`${process.env['NEXT_PUBLIC_API_URL']}/api/...`, {
      next: { revalidate: 60 },
    });
    if (!res.ok) return fallback;
    return res.json();
  } catch {
    return fallback;  // never crash the page if API is down
  }
}
```

Missing try/catch caused cached pages to work while uncached ones crashed — the ACCESSORY products page was the symptom.

## Logo variants

Two files in `apps/web/public/`:

| File            | Use                          |
|-----------------|------------------------------|
| `Logo.PNG`      | Navbar (white background)    |
| `LOGO_fest.png` | Footer (dark background, transparent bg) |

Do NOT apply `brightness-0 invert` to `LOGO_fest.png` — it already has white/green colours for dark backgrounds.

## Tailwind brand tokens

Defined in `tailwind.config.ts`:
- `brand-blue-*` — navy blue family (#1B3566 base)
- `brand-green-*` — lime green family (#8CC63F base)
