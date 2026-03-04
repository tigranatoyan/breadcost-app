import { getAuthHeader, clearCredentials } from './auth';

export const API_BASE = 'http://localhost:8080';
export const TENANT_ID = 'tenant1';

export async function apiFetch<T = unknown>(
  path: string,
  options?: RequestInit
): Promise<T> {
  const auth = getAuthHeader();
  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(auth ? { Authorization: auth } : {}),
      ...(options?.headers ?? {}),
    },
  });

  if (res.status === 401) {
    clearCredentials();
    if (typeof window !== 'undefined') window.location.href = '/login';
    throw new Error('Unauthorized');
  }

  if (!res.ok) {
    const text = await res.text().catch(() => res.statusText);
    let msg: string;
    try {
      const json = JSON.parse(text);
      msg = json.message ?? json.error ?? text;
    } catch {
      msg = text;
    }
    throw new Error(`${res.status}: ${msg}`);
  }

  const text = await res.text();
  return (text ? JSON.parse(text) : {}) as T;
}
