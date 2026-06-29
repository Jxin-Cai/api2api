import { apiClient } from '@shared/api';
import type { ApiResponse } from '@shared/api';
import type { LoginResponse } from '@entities/user-account';

import type { LoginRequest } from '../model/types';

export function loginByUsername(params: LoginRequest): Promise<ApiResponse<LoginResponse>> {
  return apiClient.post('/api/auth/login', params);
}
