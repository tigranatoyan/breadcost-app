import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  globalSetup: require.resolve('./global-setup'),
  testDir: './tests',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: 1,
  reporter: [['html', { open: 'never' }], ['list']],
  timeout: 30_000,
  expect: { timeout: 10_000 },

  use: {
    baseURL: 'http://localhost:3000',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  /* Web servers — only used when they are not already running.
     Set SKIP_WEB_SERVER=1 to suppress auto-start entirely. */
  ...(!process.env.SKIP_WEB_SERVER ? {
    webServer: [
      {
        command: process.platform === 'win32'
          ? 'cd .. && .\\gradlew.bat bootRun'
          : 'cd .. && ./gradlew bootRun',
        url: process.env.API_BASE ? `${process.env.API_BASE}/` : 'http://localhost:8080/',
        ignoreHTTPSErrors: true,
        reuseExistingServer: !process.env.CI,
        timeout: 120_000,
      },
      {
        command: 'npm run dev',
        url: 'http://localhost:3000',
        reuseExistingServer: !process.env.CI,
        timeout: 30_000,
      },
    ],
  } : {}),
});
