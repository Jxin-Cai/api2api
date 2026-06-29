import { apiClient, type ApiResponse, type QueryParams } from '@shared/api';

import type {
  AdminDashboardResponse,
  FrontDashboardBackendResponse,
  FrontDashboardRecentCallBackendResponse,
  FrontDashboardResponse,
  GetAdminDashboardRequest,
  GetFrontDashboardRequest,
} from '../model/types';

function toStringValue(value: string | number | undefined): string | undefined {
  return value === undefined || value === null ? undefined : String(value);
}

function toRecentCallRecord(record: FrontDashboardRecentCallBackendResponse) {
  return {
    id: String(record.id),
    model: record.requestedModel ?? '-',
    protocolType: record.requestProtocol ?? '-',
    tokens: record.totalTokens ?? 0,
    status: record.status,
    createdAt: record.startedAt ?? record.endedAt ?? '-',
    apiCredentialId: toStringValue(undefined),
  };
}

function toFrontDashboard(response: FrontDashboardBackendResponse): FrontDashboardResponse {
  return {
    todayTokens: response.todayTokens,
    monthTokens: response.monthTokens,
    apiKeyCount: response.apiKeyCount,
    recentCalls: response.recentCalls?.map(toRecentCallRecord) ?? [],
  };
}

export async function getFrontDashboard(
  params: GetFrontDashboardRequest = {}
): Promise<ApiResponse<FrontDashboardResponse>> {
  const response = await apiClient.get<FrontDashboardBackendResponse>('/api/dashboard', params as unknown as QueryParams);
  return {
    ...response,
    data: toFrontDashboard(response.data),
  };
}

export async function getAdminDashboard(
  params: GetAdminDashboardRequest = {}
): Promise<ApiResponse<AdminDashboardResponse>> {
  return apiClient.get('/api/admin/dashboard', params as unknown as QueryParams);
}
