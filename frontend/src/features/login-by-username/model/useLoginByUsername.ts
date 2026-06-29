import { useMutation, useQueryClient } from '@tanstack/react-query';

import { currentUserQueryKey, type LoginResponse } from '@entities/user-account';
import { loginByUsername } from '../api/loginApi';
import type { LoginRequest } from './types';

export function useLoginByUsername() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (params: LoginRequest): Promise<LoginResponse> => {
      const response = await loginByUsername(params);
      return response.data;
    },
    onSuccess: async (): Promise<void> => {
      await queryClient.invalidateQueries({ queryKey: currentUserQueryKey });
    },
  });
}
