import { defineConfig, devices } from '@playwright/test';

/**
 * E2E contra el stack completo: Next.js (BFF) en :3000 → Spring Boot en :8080 → PostgreSQL con el seed dev
 * (FECHA_SIMULADA=2027-03-20). Levante backend y base antes (docker compose up, o ver README).
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:3000',
    trace: 'retain-on-failure',
    locale: 'es-CO',
    timezoneId: 'America/Bogota',
  },
  projects: [
    { name: 'celular', use: { ...devices['Pixel 7'] } },
    { name: 'escritorio', use: { ...devices['Desktop Chrome'] } },
  ],
  webServer: process.env.E2E_BASE_URL ? undefined : {
    command: 'npm run start',
    url: 'http://localhost:3000/login',
    reuseExistingServer: true,
    timeout: 120_000,
  },
});
