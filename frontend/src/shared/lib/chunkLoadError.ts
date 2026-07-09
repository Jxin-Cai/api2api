const RELOAD_KEY = 'api2api:chunk-reload-attempted';

export function isChunkLoadError(error: unknown): boolean {
  if (!(error instanceof Error)) return false;
  const msg = error.message;
  return (
    msg.includes('Failed to fetch dynamically imported module') ||
    msg.includes('Importing a module script failed') ||
    msg.includes('Loading chunk') ||
    error.name === 'ChunkLoadError'
  );
}

export function handleChunkLoadFailure(): boolean {
  const alreadyAttempted = sessionStorage.getItem(RELOAD_KEY);
  if (alreadyAttempted) return false;
  sessionStorage.setItem(RELOAD_KEY, '1');
  window.location.reload();
  return true;
}

export function clearChunkReloadFlag(): void {
  sessionStorage.removeItem(RELOAD_KEY);
}
