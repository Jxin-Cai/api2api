import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  changeUserDisplayName,
  changeUserRole,
  createUser,
  disableUser,
  enableUser,
  userAccountQueryKeys,
  type UserAccountResponse,
} from '@entities/user-account';
import type { AdminRole } from '@shared/types/admin';
import type { UserOperationResultState } from './types';

export function useUserAccountMutations() {
  const queryClient = useQueryClient();
  const [lastResult, setLastResult] = useState<UserOperationResultState | null>(null);

  function invalidateUsers(): Promise<void> {
    return queryClient.invalidateQueries({ queryKey: userAccountQueryKeys.all });
  }

  const createMutation = useMutation({
    mutationFn: createUser,
    onSuccess: (response) => {
      setLastResult({ user: response.data, action: '创建用户' });
      void invalidateUsers();
    },
  });

  const displayNameMutation = useMutation({
    mutationFn: (params: { userId: string; displayName: string }) =>
      changeUserDisplayName(params.userId, { displayName: params.displayName }),
    onSuccess: (response) => {
      setLastResult({ user: response.data, action: '修改显示名' });
      void invalidateUsers();
    },
  });

  const roleMutation = useMutation({
    mutationFn: (params: { userId: string; role: AdminRole }) => changeUserRole(params.userId, { newRole: params.role }),
    onSuccess: (response) => {
      setLastResult({ user: response.data, action: '修改角色' });
      void invalidateUsers();
    },
  });

  const enableMutation = useMutation({
    mutationFn: enableUser,
    onSuccess: (response) => {
      setLastResult({ user: response.data, action: '启用用户' });
      void invalidateUsers();
    },
  });

  const disableMutation = useMutation({
    mutationFn: disableUser,
    onSuccess: (response) => {
      setLastResult({ user: response.data, action: '禁用用户' });
      void invalidateUsers();
    },
  });

  function setResult(user: UserAccountResponse, action: string): void {
    setLastResult({ user, action });
  }

  return { createMutation, displayNameMutation, roleMutation, enableMutation, disableMutation, lastResult, setResult };
}
