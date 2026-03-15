import { request } from '@playwright/test';

const API_BASE = process.env.API_BASE ?? 'http://localhost:8080';

/**
 * Playwright global setup – runs once before all tests.
 * Assigns ENTERPRISE subscription to tenant1 so feature-gated pages
 * (suppliers, invoices, deliveries, loyalty, AI) are accessible.
 */
export default async function globalSetup() {
  const ctx = await request.newContext();

  // Login as admin to get a token
  const loginRes = await ctx.post(`${API_BASE}/v1/auth/login`, {
    data: { username: 'admin', password: 'admin' },
  });
  if (!loginRes.ok()) {
    throw new Error(`Global setup: admin login failed ${loginRes.status()}`);
  }
  const { token } = await loginRes.json();

  // Assign ENTERPRISE tier to tenant1 (enables all features)
  const assignRes = await ctx.put(`${API_BASE}/v2/subscriptions/tenants/tenant1`, {
    headers: { Authorization: `Bearer ${token}` },
    data: { tierLevel: 'ENTERPRISE', assignedBy: 'admin' },
  });
  if (!assignRes.ok()) {
    const body = await assignRes.text();
    throw new Error(`Global setup: subscription assignment failed ${assignRes.status()} ${body}`);
  }

  await ctx.dispose();
}
