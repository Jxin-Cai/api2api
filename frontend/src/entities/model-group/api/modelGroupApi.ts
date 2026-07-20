import { apiClient, type ApiResponse } from '@shared/api';

import type { ModelGroupListResponse, ModelGroupResponse, SaveModelGroupRequest } from '../model/types';

interface ModelGroupBackendResponse {
  id: string | number;
  name?: string;
  modelWhitelist?: string[];
  createdAt?: string | number;
  updatedAt?: string | number;
}

function normalizeGroup(group: ModelGroupBackendResponse): ModelGroupResponse {
  return {
    id: String(group.id),
    name: group.name ?? '',
    modelWhitelist: group.modelWhitelist ?? [],
    createdAt: group.createdAt,
    updatedAt: group.updatedAt,
  };
}

export async function listModelGroups(): Promise<ApiResponse<ModelGroupListResponse>> {
  const response = await apiClient.get<{ groups: ModelGroupBackendResponse[] }>('/api/model-groups');
  return { ...response, data: { groups: (response.data.groups ?? []).map(normalizeGroup) } };
}

export async function createModelGroup(params: SaveModelGroupRequest): Promise<ApiResponse<ModelGroupResponse>> {
  const response = await apiClient.post<ModelGroupBackendResponse>('/api/model-groups', params);
  return { ...response, data: normalizeGroup(response.data) };
}

export async function updateModelGroup(id: string, params: SaveModelGroupRequest): Promise<ApiResponse<ModelGroupResponse>> {
  const response = await apiClient.put<ModelGroupBackendResponse>(`/api/model-groups/${encodeURIComponent(id)}`, params);
  return { ...response, data: normalizeGroup(response.data) };
}

export async function deleteModelGroup(id: string): Promise<ApiResponse<void>> {
  return apiClient.delete<void>(`/api/model-groups/${encodeURIComponent(id)}`);
}
