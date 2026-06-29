import { apiClient } from '@shared/api';
import type { ApiResponse } from '@shared/api';
import type {
  ProtocolConversionDirectionRequest,
  ProtocolConversionListResponse,
  ProtocolConversionResponse,
} from '../model/types';

function encodeId(id: string | number): string {
  return encodeURIComponent(String(id));
}

export function listProtocolConversions(): Promise<ApiResponse<ProtocolConversionListResponse>> {
  return apiClient.get('/api/admin/protocol-conversions');
}

export function getProtocolConversion(
  definitionId: string | number
): Promise<ApiResponse<ProtocolConversionResponse>> {
  return apiClient.get(`/api/admin/protocol-conversions/${encodeId(definitionId)}`);
}

export function getProtocolConversionByDirection(
  params: ProtocolConversionDirectionRequest
): Promise<ApiResponse<ProtocolConversionResponse>> {
  return apiClient.get('/api/admin/protocol-conversions/by-direction', {
    'source-protocol': params.sourceProtocol,
    'target-protocol': params.targetProtocol,
  });
}

export function enableProtocolConversion(
  definitionId: string | number
): Promise<ApiResponse<ProtocolConversionResponse>> {
  return apiClient.patch(`/api/admin/protocol-conversions/${encodeId(definitionId)}/enable`);
}

export function disableProtocolConversion(
  definitionId: string | number
): Promise<ApiResponse<ProtocolConversionResponse>> {
  return apiClient.patch(`/api/admin/protocol-conversions/${encodeId(definitionId)}/disable`);
}
