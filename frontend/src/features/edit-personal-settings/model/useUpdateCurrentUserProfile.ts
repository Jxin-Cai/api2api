import { useMutation, useQueryClient } from '@tanstack/react-query';

import { currentUserQueryKey, updateCurrentUserProfile } from '@entities/user-account';
import type { CurrentUserResponse, UpdateCurrentUserProfileRequest } from '@entities/user-account';
import type { ApiResponse } from '@shared/api';

export function useUpdateCurrentUserProfile() {
  const queryClient = useQueryClient();

  return useMutation<ApiResponse<CurrentUserResponse>, Error, UpdateCurrentUserProfileRequest>({
    mutationFn: updateCurrentUserProfile,
    onSuccess: async (response): Promise<void> => {
      queryClient.setQueryData(currentUserQueryKey, response);
      await queryClient.invalidateQueries({ queryKey: currentUserQueryKey });
    },
  });
}
