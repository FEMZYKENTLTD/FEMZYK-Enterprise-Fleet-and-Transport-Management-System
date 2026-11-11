/**
 * Bundles the components under test (plus their real imports) into a single
 * CommonJS file the jsdom harness can require.
 *
 * `next/navigation` is aliased to a small stand-in because the tests need to
 * observe router pushes, not perform real navigation.
 */
import { build } from 'esbuild';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const here = dirname(fileURLToPath(import.meta.url));
const root = resolve(here, '..');

await build({
  entryPoints: [resolve(here, 'ui.entry.ts')],
  outfile: resolve(here, '.build/ui.cjs'),
  bundle: true,
  platform: 'node',
  format: 'cjs',
  jsx: 'automatic',
  target: 'node20',
  logLevel: 'warning',
  // Keep React external so the components share the test process's React
  // instance — two copies make hooks fail with 'reading useState of null'.
  // next/navigation is resolved at runtime so the bundle and the test file
  // share one module instance (otherwise navigation.pushes is never observed).
  external: ['react', 'react-dom', 'react/jsx-runtime', 'react-dom/client', 'next/navigation'],
  plugins: [{
    name: 'next-link-stub',
    setup(build) {
      build.onResolve({ filter: /^next\/link$/ }, () => ({ path: resolve(here, 'stubs/next-link.tsx') }));
    },
  }],
  define: {
    'process.env.NODE_ENV': '"test"',
  },
});

console.log('built tests/.build/ui.cjs');
