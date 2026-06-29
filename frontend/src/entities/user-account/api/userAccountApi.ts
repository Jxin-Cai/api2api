import { apiClient, encodePathParam } from '@shared/api';
import type { ApiResponse } from '@shared/api';
import type {
  AdminChangeUserDisplayNameRequest,
  AdminChangeUserRoleRequest,
  AdminCreateUserRequest,
  CurrentUserResponse,
  UpdateCurrentUserProfileRequest,
  UserAccountResponse,
} from '../model/types';

export function getCurrentUser(): Promise<ApiResponse<CurrentUserResponse>> {
  return apiClient.get('/api/auth/current-user');
}

export function logout(): Promise<ApiResponse<void>> {
  return apiClient.post('/api/auth/logout');
}

export function updateCurrentUserProfile(
  params: UpdateCurrentUserProfileRequest
): Promise<ApiResponse<CurrentUserResponse>> {
  return apiClient.patch('/api/auth/current-user/profile', params);
}

export function createUser(params: AdminCreateUserRequest): Promise<ApiResponse<UserAccountResponse>> {
  return apiClient.post('/api/admin/users', params);
}

export function changeUserDisplayName(
  userId: string | number,
  params: AdminChangeUserDisplayNameRequest
): Promise<ApiResponse<UserAccountResponse>> {
  return apiClient.patch(`/api/admin/users/${encodePathParam(userId)}/display-name`, params);
}

export function changeUserRole(
  userId: string | number,
  params: AdminChangeUserRoleRequest
): Promise<ApiResponse<UserAccountResponse>> {
  return apiClient.patch(`/api/admin/users/${encodePathParam(userId)}/role`, params);
}

export function enableUser(userId: string | number): Promise<ApiResponse<UserAccountResponse>> {
  return apiClient.patch(`/api/admin/users/${encodePathParam(userId)}/enable`);
}

export function disableUser(userId: string | number): Promise<ApiResponse<UserAccountResponse>> {
  return apiClient.patch(`/api/admin/users/${encodePathParam(userId)}/disable`);
}
