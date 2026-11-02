#!/usr/bin/env node
// Checks that every place pinning a toolchain version agrees with the source of truth.
//   node scripts/check-versions.mjs            consistency check (exit 1 on drift)
//   node scripts/check-versions.mjs --latest   also show the latest stable releases (needs network)
import { readFileSync, readdirSync } from 'node:fs';

const read = (p) => readFileSync(p, 'utf8');
const json = (p) => JSON.parse(read(p));
const major = (v) => String(v ?? '').replace(/^[^\d]*/, '').split('.')[0];
const apps = ['api', 'web', 'admin'];
const workflows = readdirSync('.github/workflows').filter((f) => f.endsWith('.yml'));

const problems = [];
const rows = [];
function check(label, expected, actual, where) {
  const ok = String(expected) === String(actual);
  rows.push([ok ? 'ok ' : 'BAD', label, where, String(actual)]);
  if (!ok) problems.push(`${where}: ${label} is ${actual}, expected ${expected}`);
}

const root = json('package.json');
const pnpm = root.packageManager?.match(/^pnpm@(\d+\.\d+\.\d+)$/)?.[1];
if (!pnpm) problems.push('package.json: packageManager must be an exact "pnpm@x.y.z"');
const nodeMajor = major(root.engines?.node);

// Source of truth: package.json "packageManager" (pnpm) and "engines.node" (Node major).
rows.push(['src', 'pnpm', 'package.json packageManager', pnpm], ['src', 'node major', 'package.json engines.node', nodeMajor]);

for (const app of apps) {
  const file = `apps/${app}/Dockerfile`;
  const df = read(file);
  const from = df.match(/^FROM node:(\d+\.\d+\.\d+)-alpine AS base$/m)?.[1];
  check('node image', nodeMajor, major(from), file);
  check('pnpm (npm install -g)', pnpm, df.match(/npm install -g pnpm@(\S+)/)?.[1], file);
  rows.push(['   ', 'node image (exact)', file, from]);
}
const images = new Set(apps.map((a) => read(`apps/${a}/Dockerfile`).match(/^FROM node:(\S+) AS base$/m)?.[1]));
if (images.size > 1) problems.push(`Dockerfiles use different node images: ${[...images].join(', ')}`);

for (const wf of workflows) {
  const text = read(`.github/workflows/${wf}`);
  for (const m of text.matchAll(/node-version:\s*['"]?([\w.]+)/g)) check('node-version', nodeMajor, major(m[1]), `.github/workflows/${wf}`);
  // The pnpm/action-setup step (up to the next "- " step) must not pin a version: it reads packageManager.
  const step = text.match(/uses: pnpm\/action-setup@[^\n]*\n((?:(?!\s*- )[^\n]*\n)*)/)?.[1] ?? '';
  if (/^\s+version:/m.test(step)) problems.push(`.github/workflows/${wf}: pnpm/action-setup must not set "version" (it reads packageManager)`);
}

const pkgs = ['package.json', ...apps.map((a) => `apps/${a}/package.json`), ...readdirSync('packages').map((p) => `packages/${p}/package.json`)];
const specs = {};
for (const p of pkgs) {
  const d = json(p);
  for (const [name, spec] of Object.entries({ ...d.dependencies, ...d.devDependencies })) (specs[name] ??= []).push([p, spec]);
}
for (const p of specs['@types/node'] ?? []) check('@types/node major', nodeMajor, major(p[1]), p[0]);

// Packages that must move together: same spec everywhere, and grouped ones on the same version.
const lockstep = [['prisma', '@prisma/client', '@prisma/adapter-pg'], ['next'], ['react', 'react-dom'], ['@types/react', '@types/react-dom'], ['typescript'], ['vitest'], ['tailwindcss', '@tailwindcss/postcss']];
for (const group of lockstep) {
  const all = group.flatMap((n) => (specs[n] ?? []).map(([p, s]) => [`${n} in ${p}`, s]));
  const distinct = new Set(all.map(([, s]) => (group.length > 1 || group[0] !== 'vitest' ? s : major(s))));
  if (distinct.size > 1) problems.push(`${group.join(' / ')} must share one version: ${all.map(([w, s]) => `${w}=${s}`).join(', ')}`);
  else if (all.length) rows.push(['ok ', group.join(' / '), `${all.length} declarations`, all[0][1]]);
}

// Production PostgreSQL (helm/api, official image) is the source of truth; local dev must use the same major.
const prodPg = read('helm/api/values.yaml').match(/^\s*tag:\s*"?(\d+)\.\d+"?\s*$/m)?.[1];
const devPg = read('docker-compose.yml').match(/image:\s*postgres:(\d+)/)?.[1];
rows.push(['src ', 'postgres major', 'helm/api/values.yaml postgresql.image.tag', prodPg]);
check('postgres major', prodPg, devPg, 'docker-compose.yml');

const w = [4, 28, 52];
for (const r of rows) console.log(r.map((c, i) => String(c ?? '').padEnd(w[i] ?? 0)).join(' '));

if (process.argv.includes('--latest')) {
  console.log('\nLatest stable releases:');
  const nodes = await (await fetch('https://nodejs.org/dist/index.json')).json();
  const lts = nodes.find((r) => r.lts);
  console.log(`  node         ${lts.version} (LTS "${lts.lts}", ${lts.date}; repo: ${nodeMajor})`);
  // Highest stable version, not the "latest" dist-tag: some projects tag release candidates as latest.
  const cmp = (a, b) => a.split('.').map(Number).reduce((r, n, i) => r || n - Number(b.split('.')[i]), 0);
  for (const name of ['pnpm', 'next', 'prisma', 'fastify', 'typescript', 'vitest', 'react', 'tailwindcss', 'zod']) {
    const meta = await (await fetch(`https://registry.npmjs.org/${name}`, { headers: { accept: 'application/vnd.npm.install-v1+json' } })).json();
    const stable = Object.keys(meta.versions).filter((v) => /^\d+\.\d+\.\d+$/.test(v)).sort(cmp).at(-1);
    const tag = meta['dist-tags'].latest;
    // Recommended: the "latest" tag, unless it is a prerelease (then the highest stable).
    const pick = /-/.test(tag) ? stable : tag;
    const note = pick !== stable ? ` (newer ${stable} not yet tagged latest)` : /-/.test(tag) ? ` ("latest" tag is prerelease ${tag})` : '';
    const current = specs[name]?.[0]?.[1] ?? (name === 'pnpm' ? pnpm : '-');
    console.log(`  ${name.padEnd(12)} ${pick.padEnd(10)} repo: ${current.padEnd(9)}${major(pick) !== major(current) && current !== '-' ? ' <- new major' : ''}${note}`);
  }
}

if (problems.length) {
  console.error(`\n${problems.length} version problem(s):\n- ${problems.join('\n- ')}`);
  process.exit(1);
}
console.log('\nAll version pins are consistent.');
