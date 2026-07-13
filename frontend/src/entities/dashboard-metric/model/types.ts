import type { UsageRecordBackendResponse, UsageRecordResponse } from '@shared/api/contracts';
import type { UsagePageSize } from '@shared/types/table';

export interface TokenAmountResponse {
  tokens: number;
  millions: number;
}

export interface DashboardTrendPointResponse {
  date?: string;
  bucket?: string;
  bucketStart?: string;
  bucketEnd?: string;
  protocol?: string;
  protocolType?: string;
  providerChannelId?: string | number;
  providerChannel?: string;
  providerChannelName?: string;
  model?: string;
  tokens?: number;
  totalTokens?: number;
  value?: number;
}

export interface DashboardRankItemResponse {
  id?: string | number;
  userId?: string | number;
  userAccountId?: string | number;
  rank?: number;
  username?: string;
  displayName?: string;
  model?: string;
  label?: string;
  tokens?: number;
  totalTokens?: number;
  value?: number;
}

export interface DashboardProtocolRequestRateResponse {
  protocol: string;
  requestCount: number;
  requestsPerMinute: number;
}

export type FrontDashboardRecentCallBackendResponse = UsageRecordBackendResponse;

export interface GetFrontDashboardRequest {
  zoneId?: string;
  recentCallsPage?: number;
  recentCallsSize?: 20 | UsagePageSize;
}

export interface GetAdminDashboardRequest {
  zoneId?: string;
  recentRateMinutes?: number;
  trendDays?: number;
}

export interface FrontDashboardBackendResponse {
  todayTokens?: TokenAmountResponse | number;
  monthTokens?: TokenAmountResponse | number;
  apiKeyCount?: number;
  recentCalls?: FrontDashboardRecentCallBackendResponse[];
}

export interface FrontDashboardResponse {
  todayTokens?: TokenAmountResponse;
  monthTokens?: TokenAmountResponse;
  apiKeyCount?: number;
  recentCalls?: UsageRecordResponse[];
}

export interface AdminDashboardResponse {
  protocolRequestRates?: DashboardProtocolRequestRateResponse[];
  todayTokens?: TokenAmountResponse;
  monthTokens?: TokenAmountResponse;
  dailyTopUsers?: DashboardRankItemResponse[];
  monthlyTopUsers?: DashboardRankItemResponse[];
  protocolTokenTrends?: DashboardTrendPointResponse[];
  channelTokenTrends?: DashboardTrendPointResponse[];
}
