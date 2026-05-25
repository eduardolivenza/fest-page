import { PostgreSqlContainer, type StartedPostgreSqlContainer } from 'testcontainers';
import { execSync } from 'child_process';

export interface TestDatabase {
  container: StartedPostgreSqlContainer;
  connectionString: string;
  teardown: () => Promise<void>;
}

export async function startTestDatabase(): Promise<TestDatabase> {
  const container = await new PostgreSqlContainer('postgres:16-alpine')
    .withDatabase('festpage_test')
    .withUsername('testuser')
    .withPassword('testpass')
    .start();

  const connectionString = container.getConnectionUri();
  process.env['DATABASE_URL'] = connectionString;

  // Run migrations against the test container
  execSync('pnpm --filter @festpage/db run migrate:prod', {
    env: { ...process.env, DATABASE_URL: connectionString },
  });

  return {
    container,
    connectionString,
    teardown: async () => {
      await container.stop();
    },
  };
}
