import type { NextConfig } from 'next';

const config: NextConfig = {
  transpilePackages: ['@festpage/ui', '@festpage/types'],
  env: {
    NEXT_PUBLIC_API_URL: process.env['NEXT_PUBLIC_API_URL'] ?? 'http://localhost:3001',
  },
};

export default config;
