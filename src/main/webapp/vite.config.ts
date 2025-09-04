import { defineConfig } from 'vite';
import { resolve } from 'path';

export default defineConfig({
  build: {
    outDir: '../dist',
    emptyOutDir: true,
    minify: false,
    rollupOptions: {
      input: {
        main: resolve(__dirname, 'src/main.ts'),
        timesheet: resolve(__dirname, 'src/timesheet.html'),
        index: resolve(__dirname, 'src/index.html')
      },
      output: {
        entryFileNames: 'js/[name].js',
        chunkFileNames: 'js/[name]-[hash].js',
        assetFileNames: (assetInfo) => {
          if (assetInfo.name?.endsWith('.html')) {
            return '[name].[ext]';
          }
          return 'assets/[name]-[hash].[ext]';
        }
      }
    },
    copyPublicDir: false
  },
  publicDir: false,
  root: 'src'
});