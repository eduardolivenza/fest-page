# FestPage — Architecture Overview

FestPage is a **mattress/bedding retail shop** (Fest Descans, Valls, Tarragona) built as an Nx monorepo.

## Monorepo structure

```
apps/
  web/      Next.js 15 — public storefront
  admin/    Next.js 15 — back-office (admin panel)
  api/      Fastify 5  — REST API (shared by web + admin)
packages/
  types/    Shared TypeScript interfaces & enums (no runtime code)
  ui/       Shared React components (ProductCard, Badge, Button…)
  db/       Prisma schema, migrations, seed
```

Package manager: **pnpm 9** with workspaces.  
Monorepo tooling: **Nx 20**.

## Tech stack

| Layer        | Choice                          |
|--------------|---------------------------------|
| Frontend     | Next.js 15, App Router, React 19 |
| Styling      | Tailwind CSS 3, custom brand tokens |
| API          | Fastify 5, TypeScript           |
| ORM          | Prisma 6, PostgreSQL            |
| Auth (admin) | Argon2 password hashing, JWT    |
| Infra        | Docker Compose (dev), Kubernetes (prod) |

## Environment

`NEXT_PUBLIC_API_URL` points to the Fastify API (default `http://localhost:3001`).

## Related skills

- `/arch-frontend` — Next.js patterns, server/client split, error handling  
- `/arch-i18n`     — Internationalisation decisions  
- `/arch-api`      — API route conventions and error handling
