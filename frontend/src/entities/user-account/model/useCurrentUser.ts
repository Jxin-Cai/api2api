import { useQuery, useQueryClient } from '@tanstack/react-query';
import { getCurrentUser } from '../api/userAccountApi';

export const currentUserQueryKey = ['current-user'] as const;

export function useCurrentUser() {
  const queryClient = useQueryClient();
  const query = useQuery({
    queryKey: currentUserQueryKey,
    queryFn: getCurrentUser,
    retry: false,
  });

  const user = query.data?.data ?? null;

  return {
    ...query,
    user,
    isAuthenticated: Boolean(user),
    refresh: (): Promise<void> => queryClient.invalidateQueries({ queryKey: currentUserQueryKey }),
  };
}
