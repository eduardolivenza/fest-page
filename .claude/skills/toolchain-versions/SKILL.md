---
name: toolchain-versions
description: Which Node, pnpm, Next, React, TypeScript, Java, Gradle, Spring Boot and Postgres versions FestPage uses, where each is pinned, and how to change one safely. Use BEFORE writing a version number anywhere (Dockerfile, workflow, package.json, docs, commands you suggest), when adding or upgrading a dependency, or when a Renovate PR is open.
---

# Toolchain versions

**Never write a version from memory.** Read it from the repo, and check upstream before upgrading.

```bash
pnpm check:versions            # every pin vs the source of truth; exit 1 on drift (also runs in PR checks)
pnpm check:versions --latest   # + latest releases from nodejs.org, npm, Adoptium (Java LTS), Gradle and Maven Central (Spring Boot)
```

## Sources of truth

| What | Source of truth | Must match it |
|---|---|---|
| pnpm | `package.json` → `"packageManager": "pnpm@x.y.z"` (exact) | `npm install -g pnpm@x.y.z` in `apps/{web,admin}/Dockerfile`; `engines.pnpm` |
| Node (major) | `package.json` → `engines.node` | `FROM node:<x.y.z>-alpine` in the 2 Node Dockerfiles (same exact tag in both); `node-version:` in every `.github/workflows/*.yml`; `@types/node` major (root) |
| Java (major, LTS) | `apps/api/gradle/libs.versions.toml` → `java` | Gradle toolchain (reads the catalog); `FROM eclipse-temurin:<x.y.z_b>-jdk-alpine` / `-jre-alpine` in `apps/api/Dockerfile` (same exact tag in both stages); `java-version:` in `ci-api.yml` and `pr-checks.yml` |
| Gradle | `apps/api/gradle/wrapper/gradle-wrapper.properties` (`distributionUrl` + `distributionSha256Sum`) | change with `./gradlew wrapper --gradle-version <x.y.z>` and re-pin the checksum from `https://services.gradle.org/distributions/gradle-<x.y.z>-bin.zip.sha256` |
| Spring Boot and api libraries | `apps/api/gradle/libs.versions.toml` (`spring-boot` drives both the plugin and the BOM; the BOM manages Hibernate, Flyway, Jackson, JUnit, PostgreSQL driver…) | only add a `version.ref` for libraries the BOM does not manage (springdoc, BouncyCastle, zonky embedded-postgres) |
| Next / React | `next` same in web + admin; `react`/`react-dom` and `@types/react`/`@types/react-dom` same everywhere | |
| Postgres | `helm/api/values.yaml` → `postgresql.image` (official `postgres`, exact tag + digest; template `helm/api/templates/postgresql.yaml`) | `docker-compose.yml` same major; `postgres-binaries` in `apps/api/gradle/libs.versions.toml` same major (embedded test DB) (the CI smoke test reads the image from the values). A **major** bump is a data migration (dump/restore), never just a tag change: see homelab skill `fest-descans-postgres` |

`pnpm/action-setup` must **not** set `version:`; it reads `packageManager`. pnpm itself also switches to the pinned version, so running a different pnpm in this repo silently uses the pinned one.

## Choosing "latest stable"

- Node: the newest **LTS** line (even majors). Odd majors and `-alpha` are not stable.
- Java: the newest **LTS** (Adoptium `most_recent_lts`); Temurin images `eclipse-temurin:<x.y.z_b>-{jdk,jre}-alpine`.
- npm packages: the `latest` dist-tag, unless it is a prerelease, then the highest `x.y.z`. Some projects (pnpm) publish to `next` first; `--latest` shows both.
- Maven artifacts (Spring Boot, springdoc…): highest plain `x.y.z` in `maven-metadata.xml` (Maven Central's `<release>` can be an `-RC`/`-M`). Gradle: `https://services.gradle.org/versions/current`.
- Same release-age rule as npm for everything: don't pick a release published less than a day ago (Renovate waits 3 days).
- A new **major** needs its upgrade guide read first (official release notes / migration page), not just a version bump.

## Changing a version: checklist

1. Run `pnpm check:versions` (must be clean before you start).
2. Change the source of truth **and every place in its row** above, in one commit.
3. `pnpm install` with the repo's pnpm, never hand-edit or hand-merge `pnpm-lock.yaml`. On a lockfile merge conflict: take `main`'s lockfile, then `pnpm install`. (A hand-merged lockfile broke every pipeline once: missing `safe-regex2` entry.)
4. `pnpm check:versions && pnpm typecheck && pnpm -r --if-present run test && pnpm --filter "./apps/*" run build && pnpm api:build`.
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
- **Spring Boot 4** (api): modular starters (`spring-boot-starter-webmvc`, `-security-oauth2-resource-server`, `-data-jpa`, `-flyway` (Flyway auto-config is no longer in the core autoconfigure jar), `-actuator`; test slices in `-webmvc-test`, `-data-jpa-test`) and moved packages (`org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`, `org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer`, `org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration` / `FlywayMigrationStrategy`, `org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration`). Jackson **3** (`tools.jackson.*` packages). JUnit Jupiter 6 (the JUnit 5 programming model; managed by Boot).
- **Duration properties**: Spring's `Duration` binding accepts `5s`, but Hikari's `connection-timeout` is a `long` in ms.
- **Argon2**: Spring Security's `Argon2PasswordEncoder` rejects node-argon2's `m,p,t` parameter order; the api parses PHC strings itself (`Argon2PasswordHasher`).
- **Nimbus JOSE** (JWT) needs an HS256 key of ≥ 256 bits: `JWT_SECRET` must be ≥ 32 bytes.
- **Gradle 9**: tests need `junit-platform-launcher` on `testRuntimeOnly` (set in the root build).
- **Next 16**: Turbopack by default: CSS `@import url(...)` must come first in `globals.css`. `next lint` no longer exists.
- **TypeScript 7** (Go-native `tsc`, statically linked so it runs on Alpine):
  - `baseUrl` is removed: `paths` are relative to the tsconfig that declares them (`./packages/...` in the root config).
  - `types` defaults to `[]`: code that uses Node globals declares `"types": ["node"]` (api). `strict`, `esModuleInterop` are always on.
  - **No JS compiler API in 7.0** (`import ts from 'typescript'` fails). `next build` type-checks through the `tsc` CLI (Next's default, `experimental.useTypeScriptCli`), so it works. If a new tool needs the API (typescript-eslint, ts-node, …), install TypeScript 6 side by side as the docs recommend: `"@typescript/native": "npm:typescript@^7"` + `"typescript": "npm:@typescript/typescript6@^6"`.
  - Editors: Next's TS language-service plugin may not load under the TS 7 language server; in VS Code "Disable TypeScript 7 Language Server" falls back to the editor's bundled TS.
- **Vitest 5** (runs on Vite 8, installed automatically as a peer):
  - JSX in web/admin tests is compiled by **oxc**, not esbuild: `oxc: { jsx: { runtime: 'automatic' } }` in `vitest.config.mts` (Next's tsconfig says `"jsx": "preserve"`; without the override test files with JSX fail to parse). The old `esbuild` option is ignored.
  - `clearMocks` is on by default (mock call history is cleared before every test); `mockReset` restores a `vi.fn(impl)` to `impl`.
  - `vi.mock`/`vi.hoisted` must be at module top level (inside a function or test it now throws); `expect(...).resolves/.rejects` must be awaited or the test fails.
  - Requires Node >= 22.12.
- **Tailwind 4**: CSS-first config (see `arch-frontend`); `tailwindcss` and `@tailwindcss/postcss` must share one version. The `@tailwindcss/upgrade` tool does not work in this pnpm monorepo; the class renames were done by hand following the official upgrade guide.
