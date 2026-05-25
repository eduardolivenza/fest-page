FROM node:20-alpine AS base
ENV PNPM_HOME="/pnpm"
ENV PATH="$PNPM_HOME:$PATH"
RUN corepack enable

FROM base AS deps
WORKDIR /repo
COPY package.json pnpm-workspace.yaml pnpm-lock.yaml* ./
COPY packages/types/package.json packages/types/
COPY packages/db/package.json packages/db/
COPY packages/db/prisma packages/db/prisma
COPY apps/api/package.json apps/api/
RUN --mount=type=cache,id=pnpm,target=/pnpm/store pnpm install --frozen-lockfile

FROM base AS builder
WORKDIR /repo
COPY --from=deps /repo/node_modules ./node_modules
COPY --from=deps /repo/packages/types/node_modules packages/types/node_modules
COPY --from=deps /repo/packages/db/node_modules packages/db/node_modules
COPY --from=deps /repo/apps/api/node_modules apps/api/node_modules
COPY package.json .
COPY packages/types packages/types
COPY packages/db packages/db
COPY apps/api apps/api
COPY tsconfig.base.json .
WORKDIR /repo/apps/api
RUN pnpm run build

FROM base AS runner
WORKDIR /app
ENV NODE_ENV=production
COPY --from=builder /repo/apps/api/dist ./dist
COPY --from=builder /repo/packages/db/prisma ./prisma
COPY --from=deps /repo/apps/api/node_modules ./node_modules
RUN addgroup -g 1001 -S nodejs && adduser -S appuser -u 1001
USER appuser
EXPOSE 3001
CMD ["node", "dist/index.js"]
