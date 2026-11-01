import 'dotenv/config';
import { defineConfig } from 'prisma/config';

export default defineConfig({
  schema: 'prisma/schema.prisma',
  migrations: {
    path: 'prisma/migrations',
    seed: 'tsx src/seed.ts',
  },
  datasource: {
    // Not env('DATABASE_URL'): that throws when unset, and `prisma generate` (postinstall, CI, Docker) has no database.
    url: process.env['DATABASE_URL'] ?? '',
  },
});
