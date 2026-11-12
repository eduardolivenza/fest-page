---
name: arch-delivery
description: How FestPage code gets from a branch to production — PR checks that gate merges, per-app CI pipelines, image tags committed back to main, ArgoCD deployment, SealedSecrets, Cloudflare in front, and Renovate. Use when changing workflows, Dockerfiles, Helm charts or secrets, preparing a PR, or when a deploy/pipeline fails.
---

# FestPage — Delivery

## Flow

```
branch → PR ──(PR checks must pass)──→ merge to main
  → ci-<app>.yml (only for paths of that app): typecheck + tests → tag <app>-vX.Y.Z → build & push ghcr.io/eduardolivenza/festpage/<app>
  → "Update image tag" job commits helm/<app>/values.yaml image.tag to main ("ci: update … [skip ci]")
  → ArgoCD (homelab repo, ApplicationSet fest-descans) syncs helm/<app> → K3s namespace fest-descans
```

Claude cannot push to main or touch the cluster: work on a branch, push it, give the PR link (`gh` is not installed), and give Edu copy-pasteable `kubectl` commands for verification.

## PR checks (`.github/workflows/pr-checks.yml`): the merge gate

- **Checks**: version pins (`scripts/check-versions.mjs`), `pnpm install --frozen-lockfile`, typecheck, all tests, build web/admin, and the api's `./gradlew build` (JDK 25; includes the embedded-PostgreSQL tests).
- **Checks** also runs `helm lint` + `helm template` on every chart in `helm/`.
- **Image (api|web|admin)**: Docker build without push. The api job also starts the production Postgres image with the chart's init script and settings (app user not a superuser), starts the server (it applies the migrations on startup) with a 512m memory cap, calls `/health/readiness`, `/health` (includes the DB), `/api/products` and checks an admin route answers 401 without a token.
- They run on every PR without path filters, so the required checks always report. Intended ruleset on `main` (configured in GitHub settings, not in the repo): PR required, these checks required, branch up to date, GitHub Actions in the bypass list (for the tag-bump commits).
- Never merge with red or pending checks; never "fix" a check by weakening it without saying so.

## Docker images

web/admin: multi-stage (`base` → `deps` → `builder` → `runner`), `node:<pinned>-alpine`, non-root `appuser`, `--mount=type=cache` pnpm store at `/pnpm/store`, Next standalone `server.js`.
api: `eclipse-temurin:<pinned>-jdk-alpine` builder (`./gradlew bootJar`, Gradle cache mount, no tests) → `-jre-alpine` runner with the extracted jar in `/app` (`app.jar` + `lib/`), non-root `appuser`, JVM flags in `CMD` sized for the 512Mi limit (heap 60%, SerialGC). Migrations run inside the app on startup (no initContainer). Probes: startup/liveness `/health/liveness`, readiness `/health/readiness` (Actuator). Measured: ~300Mi RSS, ~9s start on one fast core (on the server the first start took >3 min at a 500m CPU limit: the chart allows 2 CPUs and a 10-minute `startupProbe`).
Cross-stage `COPY --from=` sources must be **absolute**. Test files, `vitest.config.*` and the api's `build/`, `.gradle/`, `src/test` are excluded via `.dockerignore`.

## Runtime config

| Variable | Where it comes from |
|---|---|
| `NEXT_PUBLIC_API_URL`, `NEXT_PUBLIC_TURNSTILE_SITE_KEY` | **build args** in `ci-web.yml`/`ci-admin.yml` (repo variables override defaults) |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | api chart; password from Secret `fest-descans-api` key `password` |
| `JWT_SECRET` (≥ 32 bytes or the api won't start) | api chart, from Secret `fest-descans-api` |
| `TURNSTILE_SECRET_KEY` | optional Secret `fest-descans-api-turnstile` |
| `WEB_URL`, `ADMIN_URL` (CORS) | `helm/api/values.yaml` `cors.*` |

## Secrets

Only **SealedSecrets** are committed, in `helm/api/templates/sealed-*.yaml` (`fest-descans-api`, `ghcr-credentials` used by all three apps, `fest-descans-api-turnstile`). Never ask Edu to paste a plaintext secret into the chat; give Edu the one-line `kubectl create secret … --dry-run=client -o yaml | kubeseal -o yaml` command (no trailing spaces after `\`, leading space to keep it out of bash history) and commit the sealed output Edu pastes back. `ghcr-credentials` uses a classic PAT with only `read:packages` and expires: an `ErrImagePull … 403` means it must be rotated. Details: homelab skill `homelab-secrets`.

## Edge (Cloudflare)

Tunnel routes in the homelab repo (`components/cloudflared/values.yaml`): `festdescans.com`→web:3000, `admin.`→admin:3002, `api.`→api:3001. Cloudflare provides TLS (Always HTTPS, min TLS 1.2, HSTS), Web Analytics and Turnstile. Traefik is disabled and the charts have no Ingress: cloudflared talks to the Services directly. A new hostname is a tunnel route in homelab, not an Ingress.

## Renovate

`renovate.json`: npm, Maven and Gradle-wrapper updates wait 3 days (pnpm 12 rejects packages < 1 day old); security PRs (`vulnerabilityAlerts`) wait 1 day, because Renovate's default skips the wait for them and their CI would fail the install. Spring Boot's plugin and BOM share one catalog version. `eclipse-temurin` images stay on Java 25 (LTS); the embedded test PostgreSQL never gets a major (it follows production). **Majors need approval on the Dependency Dashboard**. Merge Renovate PRs one at a time; with "branch up to date" required, Renovate rebases the next one.

## After a deploy

```bash
kubectl -n fest-descans get pods -o custom-columns=NAME:.metadata.name,IMAGE:.spec.containers[0].image,STATUS:.status.phase
kubectl -n fest-descans logs deploy/fest-descans-api | grep -i flyway   # "... No migration necessary." or "Successfully applied N migrations"
```
`/health/liveness` and `/health/readiness` do not touch the DB (`/health` does), so also check a real endpoint (`curl -s https://api.festdescans.com/api/products | head -c 200`) and the admin login.
