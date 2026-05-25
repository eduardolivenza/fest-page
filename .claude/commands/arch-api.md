# FestPage — API Architecture

App: `apps/api` — Fastify 5, TypeScript, Prisma 6.

## Base URL

`http://localhost:3001` (dev). Configured via `NEXT_PUBLIC_API_URL` env var in the web/admin apps.

## Route structure

```
/api/products          GET  (list, supports ?category=, ?featured=, ?page=, ?pageSize=)
/api/products/:idOrSlug  GET  (by id or slug)
/api/contacts          POST (create contact request)
/api/admin/*           protected admin routes (JWT)
```

## Category enum

`ProductCategory = 'BED' | 'MATTRESS' | 'PILLOW' | 'ACCESSORY'`

Defined in `packages/types/src/index.ts` and mirrored in the Prisma schema enum.  
Seed data (`packages/db/src/seed.ts`) only includes BED, MATTRESS, PILLOW — **no ACCESSORY products are seeded**.

## Error handling

API routes return `{ message: string }` with appropriate HTTP status codes.  
Web/admin fetch wrappers must use `try/catch` — network errors are not caught by `res.ok` checks alone.

## Authentication (admin)

- Passwords hashed with Argon2.
- Sessions use JWT tokens.
- Admin routes are under `/api/admin/` and require Bearer token.

## Prisma

Schema at `packages/db/prisma/schema.prisma`.  
To run migrations: `pnpm --filter @festpage/db prisma migrate dev`.  
To seed: `pnpm --filter @festpage/db prisma db seed`.
