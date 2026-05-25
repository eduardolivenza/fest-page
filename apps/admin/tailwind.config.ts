import type { Config } from 'tailwindcss';
import preset from '@festpage/config/tailwind';

const config: Config = {
  presets: [preset],
  content: [
    './src/**/*.{ts,tsx}',
    '../../packages/ui/src/**/*.{ts,tsx}',
  ],
};

export default config;
