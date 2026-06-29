import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import {
  changeUserDisplayName,
  changeUserRole,
  createUser,
  disableUser,
  enableUser,
  type UserAccountResponse,
} from '@entities/user-account';
import type { AdminRole } from '@shared/types/admin';
import type { UserOperationResultState } from './types';

export function useUserAccountMutations() {
  const [lastResult, setLastResult] = useState<UserOperationResultState | null>(null);

  const createMutation = useMutation({
    mutationFn: createUser,
    onSuccess: (response) => setLastResult({ user: response.data, action: '创建用户' }),
  });

  const displayNameMutation = useMutation({
    mutationFn: (params: { userId: string; displayName: string }) =>
      changeUserDisplayName(params.userId, { displayName: params.displayName }),
    onSuccess: (response) => setLastResult({ user: response.data, action: '修改显示名' }),
  });

  const roleMutation = useMutation({
    mutationFn: (params: { userId: string; role: AdminRole }) => changeUserRole(params.userId, { newRole: params.role }),
    onSuccess: (response) => setLastResult({ user: response.data, action: '修改角色' }),
  });

  const enableMutation = useMutation({
    mutationFn: enableUser,
    onSuccess: (response) => setLastResult({ user: response.data, action: '启用用户' }),
  });

  const disableMutation = useMutation({
    mutationFn: disableUser,
    onSuccess: (response) => setLastResult({ user: response.data, action: '禁用用户' }),
  });

  function setResult(user: UserAccountResponse, action: string): void {
    setLastResult({ user, action });
  }

  return { createMutation, displayNameMutation, roleMutation, enableMutation, disableMutation, lastResult, setResult };
}
