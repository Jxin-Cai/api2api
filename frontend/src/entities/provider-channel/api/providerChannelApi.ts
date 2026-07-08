import { apiClient } from '@shared/api';
import type { ApiResponse } from '@shared/api';
import type {
  AdminCreateProviderChannelRequest,
  AdminUpdateProviderChannelRequest,
  ProviderChannelListResponse,
  ProviderChannelResponse,
} from '../model/types';

function encodeId(id: number): string {
  return encodeURIComponent(String(id));
}

export function listProviderChannels(): Promise<ApiResponse<ProviderChannelListResponse>> {
  return apiClient.get('/api/admin/provider-channels');
}

export function createProviderChannel(
  params: AdminCreateProviderChannelRequest
): Promise<ApiResponse<ProviderChannelResponse>> {
  return apiClient.post('/api/admin/provider-channels', params);
}

export function updateProviderChannel(
  providerChannelId: number,
  params: AdminUpdateProviderChannelRequest
): Promise<ApiResponse<ProviderChannelResponse>> {
  return apiClient.put(`/api/admin/provider-channels/${encodeId(providerChannelId)}`, params);
}

export function enableProviderChannel(providerChannelId: number): Promise<ApiResponse<ProviderChannelResponse>> {
  return apiClient.patch(`/api/admin/provider-channels/${encodeId(providerChannelId)}/enable`);
}

export function disableProviderChannel(providerChannelId: number): Promise<ApiResponse<ProviderChannelResponse>> {
  return apiClient.patch(`/api/admin/provider-channels/${encodeId(providerChannelId)}/disable`);
}

export function deleteProviderChannel(providerChannelId: number): Promise<ApiResponse<void>> {
  return apiClient.delete(`/api/admin/provider-channels/${encodeId(providerChannelId)}`);
}
