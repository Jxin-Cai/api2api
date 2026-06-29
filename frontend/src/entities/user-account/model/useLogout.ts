import { useMutation, useQueryClient } from '@tanstack/react-query';

import { logout } from '../api/userAccountApi';
import { currentUserQueryKey } from './useCurrentUser';

export function useLogout() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (): Promise<void> => {
      await logout();
    },
    onSettled: async (): Promise<void> => {
      queryClient.setQueryData(currentUserQueryKey, undefined);
      await queryClient.invalidateQueries({ queryKey: currentUserQueryKey });
    },
  });
}
