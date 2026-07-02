import { apiClient, type ApiResponse } from '@shared/api';
import type { ProviderModelOptionListResponse } from '@shared/api/contracts';

export async function listProviderModels(): Promise<ApiResponse<ProviderModelOptionListResponse>> {
  return apiClient.get<ProviderModelOptionListResponse>('/api/provider-models');
}
