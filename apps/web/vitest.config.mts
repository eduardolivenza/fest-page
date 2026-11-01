import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vitest/config';

const path = (p: string) => fileURLToPath(new URL(p, import.meta.url));

export default defineConfig({
  // Next's tsconfig has "jsx": "preserve"; tests need JSX compiled (Vite 8 transforms with oxc).
  oxc: { jsx: { runtime: 'automatic' } },
  resolve: {
    alias: {
      '@/': path('./src/'),
      '@festpage/ui': path('../../packages/ui/src/index.ts'),
      '@festpage/types': path('../../packages/types/src/index.ts'),
    },
  },
  test: {
    include: ['test/**/*.test.{ts,tsx}'],
    environment: 'jsdom',
    setupFiles: ['./test/setup.ts'],
  },
});
