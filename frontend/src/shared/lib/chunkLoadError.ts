const RELOAD_KEY = 'api2api:chunk-reload-attempted';

const CHUNK_LOAD_PATTERNS = [
  'failed to fetch dynamically imported module',
  'error loading dynamically imported module',
  'importing a module script failed',
  'loading chunk',
  'unable to preload',
  'error loading dynamically imported',
];

function collectErrorText(error: unknown, seen: Set<unknown> = new Set()): string {
  if (error == null || seen.has(error)) {
    return '';
  }
  if (typeof error === 'string') {
    return error;
  }
  if (typeof error !== 'object') {
    return '';
  }
  seen.add(error);
  if (error instanceof Error) {
    return [error.name, error.message, collectErrorText(error.cause, seen)].filter(Boolean).join(' ');
  }
  const record = error as Record<string, unknown>;
  return [record.message, record.statusText, record.data, record.error]
    .map((value) => collectErrorText(value, seen))
    .filter(Boolean)
    .join(' ');
}

export function isChunkLoadError(error: unknown): boolean {
  if (error instanceof Error && error.name === 'ChunkLoadError') {
    return true;
  }
  const text = collectErrorText(error).toLowerCase();
  return CHUNK_LOAD_PATTERNS.some((pattern) => text.includes(pattern));
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
