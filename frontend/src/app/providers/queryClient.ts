import { QueryClient } from '@tanstack/react-query';

export function retryTransientQuery(failureCount: number, error: unknown): boolean {
  if (failureCount >= 2) {
    return false;
  }
  if (typeof error === 'object' && error !== null && 'status' in error) {
    const status = Number((error as { status: unknown }).status);
    if (Number.isFinite(status) && status >= 400 && status < 500 && status !== 429) {
      return false;
    }
  }
  return true;
}

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 60_000,
      gcTime: 5 * 60_000,
      retry: retryTransientQuery,
      retryDelay: (attempt: number): number => Math.min(1000 * 2 ** attempt, 4000),
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: 0,
    },
  },
});
