FROM node:20-alpine AS base
ENV PNPM_HOME="/pnpm"
ENV PATH="$PNPM_HOME:$PATH"
RUN corepack enable

FROM base AS deps
WORKDIR /repo
COPY package.json pnpm-workspace.yaml pnpm-lock.yaml* ./
COPY packages/types/package.json packages/types/
COPY packages/ui/package.json packages/ui/
COPY packages/config/package.json packages/config/
COPY packages/config/src packages/config/src
COPY apps/web/package.json apps/web/
RUN --mount=type=cache,id=pnpm,target=/pnpm/store pnpm install --frozen-lockfile

FROM base AS builder
WORKDIR /repo
COPY --from=deps /repo/node_modules ./node_modules
COPY --from=deps /repo/packages/ui/node_modules packages/ui/node_modules
COPY --from=deps /repo/apps/web/node_modules apps/web/node_modules
COPY package.json .
COPY packages packages
COPY apps/web apps/web
COPY tsconfig.base.json .
WORKDIR /repo/apps/web
ARG NEXT_PUBLIC_API_URL=http://api-service:3001
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL
RUN pnpm run build

FROM base AS runner
WORKDIR /app
ENV NODE_ENV=production
COPY --from=builder /repo/apps/web/.next/standalone ./
COPY --from=builder /repo/apps/web/.next/static ./.next/static
COPY --from=builder /repo/apps/web/public ./public
RUN addgroup -g 1001 -S nodejs && adduser -S appuser -u 1001
USER appuser
EXPOSE 3000
CMD ["node", "server.js"]
