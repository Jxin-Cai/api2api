import type { UserAccountResponse } from '@entities/user-account';
import type { AdminRole } from '@shared/types/admin';

export interface UserCreateFormState {
  username: string;
  displayName: string;
  password: string;
  role: AdminRole;
}

export interface UserOperationResultState {
  user: UserAccountResponse | null;
  action: string;
}
