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
| New endpoint | use case interface in `application/port/in`, logic in `application/service` (+ domain record methods), controller + request/response records in `adapter-in-web/<area>/` under `/api/...` (admin: `/api/admin/...`); new beans wired in `bootstrap/UseCaseConfiguration` |
| New outbound dependency (DB table, external API) | port interface in `application/port/out`, implementation in an `adapter-out-*` module (new module for a new technology), in-memory fake in `application` testFixtures |
| Type shared by API and frontends | `packages/types` |
| Component used by both frontends | `packages/ui` |
| Schema change | new `apps/api/adapter-out-persistence/src/main/resources/db/migration/V<n>__<what>.sql` + JPA entity/mapper changes |
| User-visible text on the public site | `apps/web/src/i18n/messages.ts`, both `ca` and `es` |
| Runtime config / secret | Helm values / SealedSecret (see `arch-delivery`), never hardcoded |

Don't add a new app, database, cache, queue or external SaaS without discussing it with Edu first.

## API

- Respect the hexagon: `domain` and `application` stay framework-free (no Spring/JPA imports); adapters depend inward only, never on each other. Records for everything except JPA entities; no Lombok.
- Bean-Validate all input (request records); `{ message }` errors with the status codes in `arch-api`, thrown as domain/application exceptions and mapped in `ApiExceptionHandler` (missing row → `NotFoundException` → 404, conflicts → 409).
- Admin endpoints live under `/api/admin/*` (protected by `SecurityConfiguration`). Public **write** endpoints need abuse protection (the `CaptchaVerifier` port, like contacts).
- Never return secrets or password hashes; never log request bodies with personal data.
- Keep responses shaped like `@festpage/types` (prices as numbers, paginated lists as `{ data, total, page, pageSize, totalPages }`).

## Database

- Migrations are applied automatically on deploy, before old pods are gone: make them **backwards compatible** (add nullable/defaulted columns; drop or rename in a later release after the code stops using them).
- Never edit an applied migration. Never run destructive SQL in production without a `pg_dump` first (homelab skill `fest-descans-postgres`).
- Keep enums in sync in three places: the Flyway SQL (`CREATE TYPE`), the Java enums in `domain`, and `packages/types`.

## Frontend

- Fetch through `src/lib/api.ts` helpers; handle loading, empty and error states; never let a failed API call crash a page.
- Public site: every text through `useT()`. Admin: Spanish text inline.
- Use `@festpage/ui` components and brand tokens; no new CSS framework or component library.
- Destructive admin actions: confirmation + disabled state while running.

## Tests and CI

- Every PR passes the PR checks; a change in behaviour comes with a test (API: JUnit service test with the in-memory fakes + a `MockMvcTester` test in `adapter-in-web`, and a persistence test on the embedded PostgreSQL for new queries/migrations; frontend: Testing Library). Bug fix → regression test.
- Run locally before pushing: `pnpm check:versions && pnpm typecheck && pnpm -r --if-present run test && pnpm --filter "./apps/*" run build && pnpm api:build`.
- Docker/Dockerfile/chart changes are only verified by the `Image (*)` jobs: read their logs.
- Never weaken a check (skip tests, relax `allowBuilds`/`minimumReleaseAge`, `--no-frozen-lockfile` in CI) silently. If it's necessary, say so explicitly and leave a dated comment to undo it.

## Dependencies and versions

- Follow `toolchain-versions`: no version from memory, all pins in one commit, lockfile only via `pnpm install`; api versions only in `apps/api/gradle/libs.versions.toml` (Spring Boot's BOM manages the rest: don't pin a version it manages).
- Before adding a dependency: is it maintained, small, and really needed? Native npm modules need an `allowBuilds` entry and must build on Alpine; the api image is `eclipse-temurin` Alpine, so prefer pure-Java libraries.
- Majors are planned upgrades (read the migration guide, upgrade on a branch, verify on the target Node).

## Security

- Secrets only as SealedSecrets; never in code, logs, chat or build args (only *public* keys like the Turnstile site key may be build args).
- Cloudflare is the only way in; don't expose new ports or hostnames without adding them to the tunnel config in homelab deliberately.
- Keep `security.txt` valid (renew `Expires` before 2027-10-31).

## Working with Edu

- Edu writes in Spanish or English; answer in the same language.
- Deliver changes as a branch + PR link + PR title/description; explain what to verify after merge with exact commands.
- Ask before anything outward-facing or irreversible (publishing, deleting data, rotating credentials).
