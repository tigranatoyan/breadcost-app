// JWT-based auth storage in localStorage

export const TOKEN_KEY = 'bc_token';
export const USER_KEY  = 'bc_user';

export type Role = 'admin' | 'floor' | 'management' | 'viewer' | 'finance' | 'warehouse' | 'cashier' | 'technologist';

export interface UserInfo {
  username: string;
  displayName: string;
  roles: string[];
  tenantId: string;
  primaryRole: string;
}

// ── Token helpers ─────────────────────────────────────────────────────────────

export function getToken(): string {
  if (typeof window === 'undefined') return '';
  return localStorage.getItem(TOKEN_KEY) ?? '';
}

export function getAuthHeader(): string {
  const token = getToken();
  return token ? `Bearer ${token}` : '';
}

export function getUserInfo(): UserInfo | null {
  if (typeof window === 'undefined') return null;
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) return null;
  try { return JSON.parse(raw) as UserInfo; } catch { return null; }
}

export function setSession(token: string, user: UserInfo) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

// Keep legacy name so existing callers that pass (username, password) still compile.
// New callers should use setSession() instead.
export function setCredentials(_username: string, _password: string) {
  // no-op — replaced by setSession() from login page
}

export function clearCredentials() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export function getUsername(): string {
  return getUserInfo()?.username ?? '';
}

export function getTenantId(): string {
  return getUserInfo()?.tenantId ?? 'tenant1';
}

export function isLoggedIn(): boolean {
  if (typeof window === 'undefined') return false;
  return !!localStorage.getItem(TOKEN_KEY);
}

export function hasRole(role: string): boolean {
  return getUserInfo()?.roles.includes(role) ?? false;
}

/** Maps backend primary role to a frontend Role. */
export function getRole(): Role {
  const primary = getUserInfo()?.primaryRole ?? '';
  if (primary === 'Admin') return 'admin';
  if (['ProductionUser', 'ProductionSupervisor'].includes(primary)) return 'floor';
  if (primary === 'Technologist') return 'technologist';
  if (['FinanceUser'].includes(primary)) return 'finance';
  if (primary === 'Manager') return 'management';
  if (['Warehouse', 'WarehouseKeeper'].includes(primary)) return 'warehouse';
  if (primary === 'Cashier') return 'cashier';
  return 'viewer';
}
