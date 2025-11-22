import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    globals: true,
    testTimeout: 60000, // 60 seconds for E2E tests
    hookTimeout: 30000,
    teardownTimeout: 30000,
    reporters: ['verbose', 'html'],
    outputFile: {
      html: './reports/index.html'
    }
  },
});
