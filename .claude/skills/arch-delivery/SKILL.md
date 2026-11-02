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

- **Checks**: version pins (`scripts/check-versions.mjs`), `pnpm install --frozen-lockfile`, typecheck, all tests, build all apps.
- **Checks** also runs `helm lint` + `helm template` on every chart in `helm/`.
- **Image (api|web|admin)**: Docker build without push. The api job also starts the production Postgres image with the chart's init script and settings (app user not a superuser), runs `prisma migrate deploy` exactly like the initContainer, starts the server and calls `/health` and `/api/products`, and loads argon2.
- They run on every PR without path filters, so the required checks always report. Intended ruleset on `main` (configured in GitHub settings, not in the repo): PR required, these checks required, branch up to date, GitHub Actions in the bypass list (for the tag-bump commits).
- Never merge with red or pending checks; never "fix" a check by weakening it without saying so.

## Docker images

Multi-stage (`base` → `deps` → `builder` → `runner`), `node:<pinned>-alpine`, non-root `appuser`, `--mount=type=cache` pnpm store at `/pnpm/store`. web/admin run the Next standalone `server.js`; api runs `node apps/api/dist/index.js` and its image also carries `packages/db` (Prisma CLI + config + migrations) for the migrate initContainer. Cross-stage `COPY --from=` sources must be **absolute** (`/repo/...`). Test files and `vitest.config.*` are excluded via `.dockerignore`.

## Runtime config

| Variable | Where it comes from |
|---|---|
| `NEXT_PUBLIC_API_URL`, `NEXT_PUBLIC_TURNSTILE_SITE_KEY` | **build args** in `ci-web.yml`/`ci-admin.yml` (repo variables override defaults) |
| `DATABASE_URL`, `JWT_SECRET` | api chart, from Secret `fest-descans-api` |
| `TURNSTILE_SECRET_KEY` | optional Secret `fest-descans-api-turnstile` |
| `WEB_URL`, `ADMIN_URL` (CORS) | `helm/api/values.yaml` `cors.*` |

## Secrets

Only **SealedSecrets** are committed, in `helm/api/templates/sealed-*.yaml` (`fest-descans-api`, `ghcr-credentials` used by all three apps, `fest-descans-api-turnstile`). Never ask Edu to paste a plaintext secret into the chat; give Edu the one-line `kubectl create secret … --dry-run=client -o yaml | kubeseal -o yaml` command (no trailing spaces after `\`, leading space to keep it out of bash history) and commit the sealed output Edu pastes back. `ghcr-credentials` uses a classic PAT with only `read:packages` and expires: an `ErrImagePull … 403` means it must be rotated. Details: homelab skill `homelab-secrets`.

## Edge (Cloudflare)

Tunnel routes in the homelab repo (`components/cloudflared/values.yaml`): `festdescans.com`→web:3000, `admin.`→admin:3002, `api.`→api:3001. Cloudflare provides TLS (Always HTTPS, min TLS 1.2, HSTS), Web Analytics and Turnstile. Traefik is disabled and the charts have no Ingress: cloudflared talks to the Services directly. A new hostname is a tunnel route in homelab, not an Ingress.

## Renovate

`renovate.json`: npm updates wait 3 days (pnpm 12 rejects packages < 1 day old), Prisma packages grouped, **majors need approval on the Dependency Dashboard**. Merge Renovate PRs one at a time; with "branch up to date" required, Renovate rebases the next one.

## After a deploy

```bash
kubectl -n fest-descans get pods -o custom-columns=NAME:.metadata.name,IMAGE:.spec.containers[0].image,STATUS:.status.phase
kubectl -n fest-descans logs deploy/fest-descans-api -c migrate | tail -3
```
`/health` does not touch the DB, so also check a real endpoint (`curl -s https://api.festdescans.com/api/products | head -c 200`) and the admin login.
