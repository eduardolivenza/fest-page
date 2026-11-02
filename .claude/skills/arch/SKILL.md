---
name: arch
description: FestPage architecture overview — what the monorepo contains, the tech stack, how a request flows from browser to database, and which detailed skill to read next. Start here in a new session or before any change that spans more than one app.
---

# FestPage — Architecture overview

FestPage is the website of **Fest Descans**, a bed/mattress shop in Valls (Tarragona): a public catalogue with contact form, plus an admin panel. pnpm workspace monorepo (no Nx/Turbo: root scripts are plain `pnpm --filter`/`-r`, e.g. `pnpm dev`, `pnpm test`, `pnpm db:migrate`).

```
apps/
  web/      Next.js (App Router) — public site, Catalan/Spanish        → festdescans.com
  admin/    Next.js (App Router) — back office (products, orders, contacts) → admin.festdescans.com
  api/      Fastify + Prisma — REST API used by web and admin          → api.festdescans.com
packages/
  db/       Prisma schema, migrations, prisma.config.ts, seed
  types/    Shared TypeScript types (no runtime code)
  ui/       Shared React components (Button, Badge, Card, ProductCard, Spinner)
  config/   Shared Tailwind 4 theme (`@festpage/config/theme.css`)
helm/<app>/ Helm chart per app (deployed by ArgoCD from the homelab repo)
scripts/    check-versions.mjs
```

Versions: never assume them, see skill `toolchain-versions` (`pnpm check:versions`).

## Stack

| Layer | Choice |
|---|---|
| Frontend | Next.js (App Router, Turbopack), React 19, Tailwind CSS 4 |
| API | Fastify 5, Zod validation, JWT (`@fastify/jwt`), Argon2 |
| Data | PostgreSQL, Prisma 7 with `@prisma/adapter-pg` |
| Anti-spam | Cloudflare Turnstile on the contact form |
| Tests | Vitest (api: Fastify `inject` + fake Prisma; web/admin: jsdom + Testing Library) |
| Delivery | GitHub Actions → ghcr.io images → GitOps tag bump → ArgoCD on single-node K3s, exposed via Cloudflare Tunnel |

## Request flow

Browser → Cloudflare (TLS, Web Analytics) → Cloudflare Tunnel (`cloudflared` in the cluster) → Service of web/admin/api.
web and admin pages fetch the API **from the browser** (`NEXT_PUBLIC_API_URL=https://api.festdescans.com`, baked in at build time). Server-side code inside the cluster may use `http://fest-descans-api:3001`.

## Where to read next

| Skill | For |
|---|---|
| `arch-guidelines` | Rules for any new code or change (read before implementing) |
| `arch-frontend` | web/admin structure, data fetching, styling, tests |
| `arch-api` | Routes, validation, errors, auth, Prisma 7 setup, tests |
| `arch-i18n` | Catalan/Spanish texts |
| `arch-delivery` | CI, PR checks, images, deploy, secrets, Renovate |
| `toolchain-versions` | Every version pin and how to change it |

The server itself (K3s, ArgoCD, cloudflared, Postgres operations) is documented in the homelab repo (`../homelab`, skills `homelab-*`, `fest-descans-postgres`).
