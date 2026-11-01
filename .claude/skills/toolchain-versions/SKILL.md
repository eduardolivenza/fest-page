---
name: toolchain-versions
description: Which Node, pnpm, Next, Prisma, React, TypeScript and Postgres versions FestPage uses, where each is pinned, and how to change one safely. Use BEFORE writing a version number anywhere (Dockerfile, workflow, package.json, docs, commands you suggest), when adding or upgrading a dependency, or when a Renovate PR is open.
---

# Toolchain versions

**Never write a version from memory.** Read it from the repo, and check upstream before upgrading.

```bash
pnpm check:versions            # every pin vs the source of truth; exit 1 on drift (also runs in PR checks)
pnpm check:versions --latest   # + recommended latest releases from nodejs.org and npm
```

## Sources of truth

| What | Source of truth | Must match it |
|---|---|---|
| pnpm | `package.json` → `"packageManager": "pnpm@x.y.z"` (exact) | `npm install -g pnpm@x.y.z` in `apps/{api,web,admin}/Dockerfile`; `engines.pnpm` |
| Node (major) | `package.json` → `engines.node` | `FROM node:<x.y.z>-alpine` in the 3 Dockerfiles (same exact tag in all); `node-version:` in every `.github/workflows/*.yml`; `@types/node` major (root, api) |
| Prisma | one version for `prisma`, `@prisma/client`, `@prisma/adapter-pg` | all `package.json` files that declare them (Renovate groups them) |
| Next / React | `next` same in web + admin; `react`/`react-dom` and `@types/react`/`@types/react-dom` same everywhere | |
| Postgres | production: Bitnami subchart in `helm/api` (homelab skill `fest-descans-postgres`) | CI smoke test `postgres:18` in `pr-checks.yml`; `docker-compose.yml` is still 16 (warn: changing it breaks existing local volumes) |

`pnpm/action-setup` must **not** set `version:`; it reads `packageManager`. pnpm itself also switches to the pinned version, so running a different pnpm in this repo silently uses the pinned one.

## Choosing "latest stable"

- Node: the newest **LTS** line (even majors). Odd majors and `-alpha` are not stable.
- npm packages: the `latest` dist-tag, unless it is a prerelease (Prisma tagged `8.0.0-rc.*` as latest), then the highest `x.y.z`. Some projects (pnpm) publish to `next` first; `--latest` shows both.
- A new **major** needs its upgrade guide read first (official release notes / migration page), not just a version bump.

## Changing a version: checklist

1. Run `pnpm check:versions` (must be clean before you start).
2. Change the source of truth **and every place in its row** above, in one commit.
3. `pnpm install` with the repo's pnpm, never hand-edit or hand-merge `pnpm-lock.yaml`. On a lockfile merge conflict: take `main`'s lockfile, then `pnpm install`. (A hand-merged lockfile broke every pipeline once: missing `safe-regex2` entry.)
4. `pnpm check:versions && pnpm typecheck && pnpm -r --if-present run test && pnpm --filter "./apps/*" run build`.
5. Docker images are only built in CI (PR checks: `Image (api|web|admin)`, the api one runs migrations and the server against Postgres). Open a PR and read those jobs before merging.
6. Run tests on the target Node locally when it changes: `source ~/.nvm/nvm.sh && nvm use <major>` (Node 26 is installed with nvm).

## pnpm 12 rules that bite

- **`allowBuilds`** in `pnpm-workspace.yaml`: every dependency with install scripts must be listed `true`/`false`, or the install fails (`ERR_PNPM_IGNORED_BUILDS`). A new native dependency needs an entry.
- **`minimumReleaseAge`** (default 1 day): packages published < 24h ago are rejected, also from the lockfile. Wait, or (only if the user agrees) add the exact `name@version` to `minimumReleaseAgeExclude` with a removal date. Never set it to 0 on your own, and never commit an exclusion pnpm added by itself. Renovate waits 3 days for npm updates for this reason.
- Settings live in `pnpm-workspace.yaml`; the `pnpm` field in `package.json` is ignored.
- If `pnpm-lock.yaml` gains `@pnpm/exe` entries, an older global pnpm handed over to the pinned one and recorded its launcher. Don't commit that: restore the lockfile and rerun with `npx -y pnpm@<pinned> install` (or upgrade the global pnpm).
- `verifyDepsBeforeRun: false` is deliberate (Docker stages copy `node_modules`).
- No corepack (Node 25+ doesn't ship it): Dockerfiles install pnpm with `npm install -g`.

## Known version gotchas

- **Node 25+**: built-in global `localStorage` (undefined without `--localstorage-file`) shadows jsdom's; `apps/{web,admin}/test/setup.ts` fixes tests. Don't remove it.
- **argon2 on Alpine**: its musl prebuild doesn't load on `node:*-alpine`, so the api Dockerfile `deps` stage installs `python3 make g++` to build it. Keep that.
- **Prisma 7**: client is generated into `apps/api/src/generated/prisma` (gitignored); URL in `packages/db/prisma.config.ts`; driver adapter required. See `arch-api`.
- **Next 16**: Turbopack by default: CSS `@import` must come before `@tailwind`. `next lint` no longer exists.
- **TypeScript 7** (Go-native `tsc`, statically linked so it runs on Alpine):
  - `baseUrl` is removed: `paths` are relative to the tsconfig that declares them (`./packages/...` in the root config).
  - `types` defaults to `[]`: code that uses Node globals declares `"types": ["node"]` (api). `strict`, `esModuleInterop` are always on.
  - **No JS compiler API in 7.0** (`import ts from 'typescript'` fails). `next build` type-checks through the `tsc` CLI (Next's default, `experimental.useTypeScriptCli`), so it works. If a new tool needs the API (typescript-eslint, ts-node, …), install TypeScript 6 side by side as the docs recommend: `"@typescript/native": "npm:typescript@^7"` + `"typescript": "npm:@typescript/typescript6@^6"`.
  - Editors: Next's TS language-service plugin may not load under the TS 7 language server; in VS Code "Disable TypeScript 7 Language Server" falls back to the editor's bundled TS.
- Majors with known breaking changes, not yet adopted: Vitest 5, Tailwind 4, Zod 4. Treat each as its own planned upgrade.
