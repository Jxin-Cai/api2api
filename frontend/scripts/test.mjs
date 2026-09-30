import { spawnSync } from 'node:child_process';
import { rm } from 'node:fs/promises';
import { resolve } from 'node:path';
import { build } from 'vite';

const outDir = resolve('node_modules/.tmp/model-availability-tests');
try {
  await build({
    mode: 'test',
    build: {
      ssr: resolve('tests/modelAvailability.test.ts'),
      outDir,
      emptyOutDir: true,
      rollupOptions: { output: { entryFileNames: 'tests.mjs' } },
    },
  });
  const result = spawnSync(process.execPath, ['--test', resolve(outDir, 'tests.mjs')], { stdio: 'inherit' });
  if (result.error) throw result.error;
  process.exitCode = result.status ?? 1;
} finally {
  await rm(outDir, { recursive: true, force: true });
}
