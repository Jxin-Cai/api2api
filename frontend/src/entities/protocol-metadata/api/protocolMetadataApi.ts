import { apiClient } from '@shared/api';
import type { ApiResponse } from '@shared/api';
import type { ProtocolMetadataDetailResponse, ProtocolMetadataListResponse } from '../model/types';

export function listProtocolMetadata(): Promise<ApiResponse<ProtocolMetadataListResponse>> {
  return apiClient.get('/api/admin/protocol-metadata');
}

export function getProtocolMetadataDetail(protocolType: string): Promise<ApiResponse<ProtocolMetadataDetailResponse>> {
  return apiClient.get(`/api/admin/protocol-metadata/${encodeURIComponent(protocolType)}`);
}
