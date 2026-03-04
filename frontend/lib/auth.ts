// Credential storage in localStorage

export const AUTH_KEY = 'bc_auth';   // stores "Basic base64..."
export const USER_KEY = 'bc_user';

export type Role = 'admin' | 'floor' | 'management' | 'viewer';

export function getAuthHeader(): string {
  if (typeof window === 'undefined') return 'Basic YWRtaW46YWRtaW4='; // SSR fallback
  return localStorage.getItem(AUTH_KEY) ?? '';
}

export function setCredentials(username: string, password: string) {
  const encoded = btoa(`${username}:${password}`);
  localStorage.setItem(AUTH_KEY, `Basic ${encoded}`);
  localStorage.setItem(USER_KEY, username);
}

export function clearCredentials() {
  localStorage.removeItem(AUTH_KEY);
  localStorage.removeItem(USER_KEY);
}

export function getUsername(): string {
  if (typeof window === 'undefined') return '';
  return localStorage.getItem(USER_KEY) ?? '';
}

export function isLoggedIn(): boolean {
  if (typeof window === 'undefined') return false;
  return !!localStorage.getItem(AUTH_KEY);
}

/** Maps username to a frontend display role.
 *  admin       → admin
 *  production  → floor (baker / floor staff)
 *  finance     → management
 *  anything else → viewer
 */
export function getRole(): Role {
  const u = getUsername().toLowerCase();
  if (u === 'admin') return 'admin';
  if (u === 'production') return 'floor';
  if (u === 'finance') return 'management';
  return 'viewer';
}
