import { apiClient } from '@shared/api';
import type { ApiResponse, QueryParams } from '@shared/api';
import type { ProviderChannelResponse } from '@shared/api/contracts';
import type {
  AdminFetchProviderModelsRequest,
  AdminRemoveChannelModelRequest,
  AdminUpsertChannelModelRequest,
} from '../model/types';

function encodeId(id: number): string {
  return encodeURIComponent(String(id));
}

export function fetchProviderModels(
  providerChannelId: number,
  params: AdminFetchProviderModelsRequest
): Promise<ApiResponse<ProviderChannelResponse>> {
  return apiClient.post(`/api/admin/provider-channels/${encodeId(providerChannelId)}/model-fetches`, params);
}

export function upsertChannelModel(
  providerChannelId: number,
  channelModelSupportId: number,
  params: AdminUpsertChannelModelRequest
): Promise<ApiResponse<ProviderChannelResponse>> {
  return apiClient.put(
    `/api/admin/provider-channels/${encodeId(providerChannelId)}/models/${encodeId(channelModelSupportId)}`,
    params
  );
}

export function removeChannelModel(
  providerChannelId: number,
  params: AdminRemoveChannelModelRequest
): Promise<ApiResponse<ProviderChannelResponse>> {
  return apiClient.delete(`/api/admin/provider-channels/${encodeId(providerChannelId)}/models`, params as unknown as QueryParams);
}
