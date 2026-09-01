import { apiClient, type ApiResponse, type QueryParams } from '@shared/api';
import { toUsageRecordResponse } from '@shared/api/contracts';

import type {
  AdminDashboardResponse,
  FrontDashboardBackendResponse,
  FrontDashboardResponse,
  FrontKeyMetricsResponse,
  GetAdminDashboardRequest,
  GetFrontDashboardRequest,
  GetFrontKeyMetricsRequest,
  TokenAmountResponse,
} from '../model/types';

function toTokenAmount(value: TokenAmountResponse | number | string | undefined): TokenAmountResponse {
  const raw = typeof value === 'number' || typeof value === 'string' ? value : value?.tokens;
  const parsed = typeof raw === 'number' ? raw : Number(raw);
  const safeTokens = Number.isFinite(parsed) ? Math.max(0, parsed) : 0;
  return { tokens: safeTokens, millions: safeTokens / 1_000_000 };
}

function toFiniteNumber(value: unknown): number {
  const numberValue = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(numberValue) ? numberValue : 0;
}

function toFrontDashboard(response: FrontDashboardBackendResponse): FrontDashboardResponse {
  const todayActualTokens = toTokenAmount(response?.todayActualTokens ?? response?.todayTokens);
  const monthActualTokens = toTokenAmount(response?.monthActualTokens ?? response?.monthTokens);
  return {
    todayTokens: todayActualTokens,
    todayActualTokens,
    todayTotalTokens: toTokenAmount(response?.todayTotalTokens),
    monthTokens: monthActualTokens,
    monthActualTokens,
    monthTotalTokens: toTokenAmount(response?.monthTotalTokens),
    apiKeyCount: Number.isFinite(response?.apiKeyCount) ? response.apiKeyCount : 0,
    recentCalls: Array.isArray(response?.recentCalls) ? response.recentCalls.map(toUsageRecordResponse) : [],
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

export async function getFrontKeyMetrics(
  params: GetFrontKeyMetricsRequest = {}
): Promise<ApiResponse<FrontKeyMetricsResponse>> {
  const queryParams: QueryParams = {};
  if (params.zoneId) {
    queryParams.zoneId = params.zoneId;
  }
  if (typeof params.trendDays === 'number') {
    queryParams.trendDays = params.trendDays;
  }
  if (params.credentialIds && params.credentialIds.length > 0) {
    queryParams.credentialIds = params.credentialIds.join(',');
  }
  const response = await apiClient.get<FrontKeyMetricsResponse>('/api/dashboard/key-metrics', queryParams);
  const data = response.data ?? {};
  return {
    ...response,
    data: {
      dailyTopCredentials: Array.isArray(data.dailyTopCredentials) ? data.dailyTopCredentials : [],
      monthlyTopCredentials: Array.isArray(data.monthlyTopCredentials) ? data.monthlyTopCredentials : [],
      credentialTokenTrends: Array.isArray(data.credentialTokenTrends) ? data.credentialTokenTrends : [],
    },
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
