---
name: arch-guidelines
description: Rules for future FestPage changes — where new code goes, API/frontend/database conventions, testing and CI expectations, security, dependencies and what not to do. Read before implementing any feature, refactor or dependency change in this repo, and use as the review checklist before opening a PR.
---

# FestPage — Guidelines for future work

The server is small (3.2 GB RAM, HDD) and run by one person. Prefer **simple, boring, few moving parts** over new infrastructure. Every new runtime component, dependency or service must justify its RAM, maintenance and security cost.

## Where things go

| Change | Location |
|---|---|
| New public page | `apps/web/src/app/<route>/page.tsx` (server: metadata) + `_components/<Name>Content.tsx` (client) |
| New admin page | `apps/admin/src/app/dashboard/<route>/page.tsx` ('use client') + nav entry in `dashboard/layout.tsx` |
| New endpoint | `apps/api/src/routes/<area>.ts` (public) or `routes/admin/<area>.ts`, registered in `app.ts` with an `/api/...` prefix |
| Type shared by API and frontends | `packages/types` |
| Component used by both frontends | `packages/ui` |
| Schema change | `packages/db/prisma/schema.prisma` + a migration (`pnpm --filter @festpage/db run migrate`) |
| User-visible text on the public site | `apps/web/src/i18n/messages.ts`, both `ca` and `es` |
| Runtime config / secret | Helm values / SealedSecret (see `arch-delivery`), never hardcoded |

Don't add a new app, database, cache, queue or external SaaS without discussing it with Edu first.

## API

- Zod-validate all input; `{ message }` errors with the status codes in `arch-api`; map Prisma `P2025`→404, `P2003`→409.
- Admin endpoints live under `/api/admin/*` behind `app.authenticate`. Public **write** endpoints need abuse protection (Turnstile, like contacts).
- Never return secrets or password hashes; never log request bodies with personal data.
- Keep responses shaped like `@festpage/types` (prices as numbers, paginated lists as `{ data, total, page, pageSize, totalPages }`).

## Database

- Migrations are applied automatically on deploy, before old pods are gone: make them **backwards compatible** (add nullable/defaulted columns; drop or rename in a later release after the code stops using them).
- Never edit an applied migration. Never run destructive SQL in production without a `pg_dump` first (homelab skill `fest-descans-postgres`).
- Keep enums in the Prisma schema and `packages/types` in sync.

## Frontend

- Fetch through `src/lib/api.ts` helpers; handle loading, empty and error states; never let a failed API call crash a page.
- Public site: every text through `useT()`. Admin: Spanish text inline.
- Use `@festpage/ui` components and brand tokens; no new CSS framework or component library.
- Destructive admin actions: confirmation + disabled state while running.

## Tests and CI

- Every PR passes the PR checks; a change in behaviour comes with a test (API: `app.inject` with the fake Prisma; frontend: Testing Library). Bug fix → regression test.
- Run locally before pushing: `pnpm check:versions && pnpm typecheck && pnpm -r --if-present run test && pnpm --filter "./apps/*" run build`.
- Docker/Dockerfile/chart changes are only verified by the `Image (*)` jobs: read their logs.
- Never weaken a check (skip tests, relax `allowBuilds`/`minimumReleaseAge`, `--no-frozen-lockfile` in CI) silently. If it's necessary, say so explicitly and leave a dated comment to undo it.

## Dependencies and versions

- Follow `toolchain-versions`: no version from memory, all pins in one commit, lockfile only via `pnpm install`.
- Before adding a dependency: is it maintained, small, and really needed? Native modules need an `allowBuilds` entry and must build on Alpine.
- Majors are planned upgrades (read the migration guide, upgrade on a branch, verify on the target Node).

## Security

- Secrets only as SealedSecrets; never in code, logs, chat or build args (only *public* keys like the Turnstile site key may be build args).
- Cloudflare is the only way in; don't expose new ports or hostnames without adding them to the tunnel config in homelab deliberately.
- Keep `security.txt` valid (renew `Expires` before 2027-10-31).

## Working with Edu

- Edu writes in Spanish or English; answer in the same language.
- Deliver changes as a branch + PR link + PR title/description; explain what to verify after merge with exact commands.
- Ask before anything outward-facing or irreversible (publishing, deleting data, rotating credentials).
