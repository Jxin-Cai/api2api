import { apiClient, type ApiResponse, type QueryParams } from '@shared/api';

import type {
  AdminDashboardResponse,
  FrontDashboardBackendResponse,
  FrontDashboardRecentCallBackendResponse,
  FrontDashboardResponse,
  GetAdminDashboardRequest,
  GetFrontDashboardRequest,
  TokenAmountResponse,
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

function toTokenAmount(value: TokenAmountResponse | number | undefined): TokenAmountResponse {
  const tokens = typeof value === 'number' ? value : value?.tokens;
  const safeTokens = typeof tokens === 'number' && Number.isFinite(tokens) ? tokens : 0;
  return { tokens: safeTokens, millions: safeTokens / 1_000_000 };
}

function toFiniteNumber(value: unknown): number {
  const numberValue = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(numberValue) ? numberValue : 0;
}

function toFrontDashboard(response: FrontDashboardBackendResponse): FrontDashboardResponse {
  return {
    todayTokens: toTokenAmount(response?.todayTokens),
    monthTokens: toTokenAmount(response?.monthTokens),
    apiKeyCount: Number.isFinite(response?.apiKeyCount) ? response.apiKeyCount : 0,
    recentCalls: Array.isArray(response?.recentCalls) ? response.recentCalls.map(toRecentCallRecord) : [],
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
  const response = await apiClient.get<AdminDashboardResponse>('/api/admin/dashboard', params as unknown as QueryParams);
  const data = response.data ?? {};
  return {
    ...response,
    data: {
      protocolRequestRates: Array.isArray(data.protocolRequestRates)
        ? data.protocolRequestRates.map((rate) => ({
            protocol: String(rate?.protocol ?? ''),
            requestCount: toFiniteNumber(rate?.requestCount),
            requestsPerMinute: toFiniteNumber(rate?.requestsPerMinute),
          })).filter((rate) => rate.protocol.length > 0)
        : [],
      todayTokens: toTokenAmount(data.todayTokens),
      monthTokens: toTokenAmount(data.monthTokens),
      dailyTopUsers: Array.isArray(data.dailyTopUsers) ? data.dailyTopUsers : [],
      monthlyTopUsers: Array.isArray(data.monthlyTopUsers) ? data.monthlyTopUsers : [],
      protocolTokenTrends: Array.isArray(data.protocolTokenTrends) ? data.protocolTokenTrends : [],
      channelTokenTrends: Array.isArray(data.channelTokenTrends) ? data.channelTokenTrends : [],
    },
  };
}
