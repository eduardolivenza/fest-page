FROM node:20-alpine AS base
ENV PNPM_HOME="/pnpm"
ENV PATH="$PNPM_HOME:$PATH"
RUN corepack enable

FROM base AS deps
# Toolchain for native modules whose prebuilt binary does not load on Alpine
# (argon2 >= 0.45 falls back to node-gyp). Build stage only: not in the runner image.
RUN apk add --no-cache python3 make g++
WORKDIR /repo
COPY package.json pnpm-workspace.yaml pnpm-lock.yaml ./
COPY packages/types/package.json packages/types/
COPY packages/db/package.json packages/db/
COPY packages/db/prisma packages/db/prisma
COPY packages/db/prisma.config.ts packages/db/
COPY apps/api/package.json apps/api/
RUN --mount=type=cache,id=pnpm,target=/pnpm/store pnpm install --frozen-lockfile

FROM base AS builder
WORKDIR /repo
COPY --from=deps /repo/node_modules ./node_modules
COPY --from=deps /repo/packages/db/node_modules packages/db/node_modules
COPY --from=deps /repo/apps/api/node_modules apps/api/node_modules
COPY package.json .
COPY packages/types packages/types
COPY packages/db packages/db
COPY apps/api apps/api
COPY tsconfig.base.json .
RUN pnpm --filter @festpage/db run generate
WORKDIR /repo/apps/api
RUN pnpm run build

FROM base AS runner
WORKDIR /repo
ENV NODE_ENV=production
COPY --from=deps /repo/node_modules ./node_modules
COPY --from=deps /repo/apps/api/node_modules apps/api/node_modules
# Copy db node_modules so prisma CLI is available for the migrate job
COPY --from=deps /repo/packages/db/node_modules packages/db/node_modules
COPY --from=builder /repo/apps/api/dist apps/api/dist
COPY --from=builder /repo/packages/db/prisma packages/db/prisma
COPY --from=builder /repo/packages/db/prisma.config.ts packages/db/package.json packages/db/
RUN addgroup -g 1001 -S nodejs && adduser -S appuser -u 1001
USER appuser
EXPOSE 3001
CMD ["node", "apps/api/dist/index.js"]

