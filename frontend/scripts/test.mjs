import { spawnSync } from 'node:child_process';
import { readdir, rm } from 'node:fs/promises';
import { resolve } from 'node:path';
import { build } from 'vite';

const outDir = resolve('node_modules/.tmp/frontend-tests');
const testFiles = (await readdir('tests')).filter((name) => name.endsWith('.test.ts')).sort();
if (testFiles.length === 0) throw new Error('No test files found');
const entries = Object.fromEntries(testFiles.map((name) => [name.slice(0, -3), resolve('tests', name)]));
try {
  await build({
    mode: 'test',
    build: {
      ssr: true,
      outDir,
      emptyOutDir: true,
      rollupOptions: { input: entries, output: { entryFileNames: '[name].mjs' } },
    },
  });
  const result = spawnSync(process.execPath, ['--test', ...Object.keys(entries).map((name) => resolve(outDir, `${name}.mjs`))], { stdio: 'inherit' });
  if (result.error) throw result.error;
  process.exitCode = result.status ?? 1;
} finally {
  await rm(outDir, { recursive: true, force: true });
}
