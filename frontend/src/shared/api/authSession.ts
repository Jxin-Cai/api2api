const AUTH_EXPIRED_EVENT = 'api2api:auth-expired';

/**
 * Broadcast a session-expired signal so route guards can redirect to login.
 * Authentication state is owned by the server-side session cookie, not the client.
 */
export function notifyAuthExpired(): void {
  if (typeof window === 'undefined') {
    return;
  }
  window.dispatchEvent(new CustomEvent(AUTH_EXPIRED_EVENT));
}

export function onAuthExpired(listener: () => void): () => void {
  if (typeof window === 'undefined') {
    return () => undefined;
  }
  window.addEventListener(AUTH_EXPIRED_EVENT, listener);
  return () => window.removeEventListener(AUTH_EXPIRED_EVENT, listener);
}
