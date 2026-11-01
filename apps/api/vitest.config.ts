import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    include: ['test/**/*.test.ts'],
    environment: 'node',
    // app.ts only logs warnings and errors in production; keeps request logs out of the test output.
    env: { NODE_ENV: 'production' },
  },
});
