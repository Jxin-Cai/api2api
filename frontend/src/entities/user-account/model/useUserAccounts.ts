import { useQuery } from '@tanstack/react-query';
import { getUser, listUsers } from '../api/userAccountApi';

export const userAccountQueryKeys = {
  all: ['user-accounts'] as const,
  detail: (userId: string | number) => [...userAccountQueryKeys.all, 'detail', String(userId)] as const,
};

export function useUserAccounts() {
  const query = useQuery({ queryKey: userAccountQueryKeys.all, queryFn: listUsers });

  return {
    ...query,
    users: query.data?.data.users ?? [],
  };
}

export function useUserAccountDetail(userId: string | number, enabled = true) {
  const normalizedUserId = String(userId).trim();
  const query = useQuery({
    queryKey: userAccountQueryKeys.detail(normalizedUserId),
    queryFn: () => getUser(normalizedUserId),
    enabled: enabled && normalizedUserId.length > 0,
  });

  return {
    ...query,
    user: query.data?.data ?? null,
  };
}
