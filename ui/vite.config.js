import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const polarionUrl = env.VITE_BASE_URL || 'http://localhost';

  // @grigoriev/react-sbb-polarion is linked via a `file:` dependency, which npm
  // symlinks into node_modules together with its own dev copy of React. Dedupe so the app and the
  // linked library resolve to this app's single React instance (avoids the dual-React "invalid hook
  // call"). Harmless once the package is consumed from a registry instead of a symlink.
  const resolve = { dedupe: ['react', 'react-dom'] };

  if (command === 'serve') {
    return {
      plugins: [react()],
      resolve,
      server: {
        proxy: {
          // Generic UI toolkit (SearchableDropdown JS + its CSS) served by GenericUiServlet. Served
          // unauthenticated in Polarion (see the mailworkflow-app web.xml), so the dev proxy can fetch
          // it without a session.
          '/polarion/mailworkflow-app/ui/generic': {
            target: polarionUrl,
            changeOrigin: true,
          },
          // The extension's own webapp context: its REST API, which the About page reads.
          '/polarion/mailworkflow/rest': {
            target: polarionUrl,
            changeOrigin: true,
          },
          '/polarion/rest': {
            target: polarionUrl,
            changeOrigin: true,
          },
          '/polarion/ria': {
            target: polarionUrl,
            changeOrigin: true,
          },
          '/polarion/icons': {
            target: polarionUrl,
            changeOrigin: true,
          },
        },
      },
    };
  }

  return {
    plugins: [react()],
    resolve,
    base: '/polarion/mailworkflow-app/ui/app/',
    build: {
      outDir: './dist/app',
      emptyOutDir: true,
    },
  };
});
