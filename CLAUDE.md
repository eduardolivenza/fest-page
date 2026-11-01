# FestPage — context for Claude

Website of Fest Descans (Valls): `apps/web` (public site), `apps/admin` (back office), `apps/api` (Fastify + Prisma), deployed to Edu's home K3s server by ArgoCD (infra lives in `../homelab`).

The knowledge is in project skills under `.claude/skills/`. Read the relevant one before acting:

| Skill | Use it when |
|---|---|
| `arch` | Big picture; start here in a new session |
| `arch-guidelines` | Before implementing anything, and as the PR checklist |
| `arch-api` / `arch-frontend` / `arch-i18n` | Working in that area |
| `arch-delivery` | CI, Dockerfiles, Helm, secrets, deploys, Renovate |
| `toolchain-versions` | Before writing **any** version number or touching dependencies |

## Ground rules

- **Versions: never from memory.** Run `pnpm check:versions` (add `--latest` to see current releases); it also gates PRs.
- Before pushing: `pnpm check:versions && pnpm typecheck && pnpm -r --if-present run test && pnpm --filter "./apps/*" run build`.
- Never hand-edit `pnpm-lock.yaml`; never silently weaken a check (tests, `allowBuilds`, `minimumReleaseAge`, frozen lockfile).
- Work on a branch and hand over a PR link; Claude has no access to the cluster: give Edu copy-pasteable commands and expected output.
- Answer in the language Edu writes in (Spanish or English).
