import type { AdminId, AdminRole, AdminStatus } from '@shared/types/admin';

export interface UserAccountResponse {
  id: AdminId;
  username: string;
  displayName: string;
  role: AdminRole;
  status: AdminStatus;
  createdAt?: string | number;
  updatedAt?: string | number;
}

export interface LoginResponse extends UserAccountResponse {
  currentUserId?: AdminId;
}

export type CurrentUserResponse = UserAccountResponse;

export interface UpdateCurrentUserProfileRequest {
  displayName: string;
}

export interface AdminCreateUserRequest {
  username: string;
  displayName: string;
  password: string;
  role: AdminRole;
}

export interface AdminChangeUserDisplayNameRequest {
  displayName: string;
}

export interface AdminChangeUserRoleRequest {
  role?: AdminRole;
  newRole?: AdminRole;
}
