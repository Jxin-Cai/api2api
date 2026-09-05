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

export interface GetFrontKeyMetricsRequest {
  zoneId?: string;
  trendDays?: number;
  credentialIds?: Array<string | number>;
}

export interface CredentialTokenRankingResponse {
  rank?: number;
  credentialId?: string | number;
  credentialName?: string;
  totalTokens?: number;
}

export interface CredentialTokenTrendPointResponse {
  bucketStart?: string;
  bucketEnd?: string;
  credentialId?: string | number;
  credentialName?: string;
  totalTokens?: number;
}

export interface ConcurrencyTrendPointResponse {
  bucketStart?: string;
  bucketEnd?: string;
  peakConcurrency?: number;
}

export interface CredentialConcurrencyTrendPointResponse extends ConcurrencyTrendPointResponse {
  credentialId?: string | number;
  credentialName?: string;
}

export interface ChannelLatencyRankingResponse {
  rank?: number;
  providerChannelId?: string | number;
  providerChannelName?: string;
  maxDurationMillis?: number;
  maxFirstTokenMillis?: number;
  avgFirstTokenMillis?: number;
  avgDurationMillis?: number;
  requestCount?: number;
}

export interface FrontKeyMetricsResponse {
  dailyTopCredentials?: CredentialTokenRankingResponse[];
  monthlyTopCredentials?: CredentialTokenRankingResponse[];
  credentialTokenTrends?: CredentialTokenTrendPointResponse[];
  /** 当天各 Key 的峰值并发曲线（5 分钟桶） */
  credentialConcurrencyTrends?: CredentialConcurrencyTrendPointResponse[];
}

export interface FrontDashboardBackendResponse {
  todayTokens?: TokenAmountResponse | number;
  todayActualTokens?: TokenAmountResponse | number;
  todayTotalTokens?: TokenAmountResponse | number;
  monthTokens?: TokenAmountResponse | number;
  monthActualTokens?: TokenAmountResponse | number;
  monthTotalTokens?: TokenAmountResponse | number;
  apiKeyCount?: number;
  recentCalls?: FrontDashboardRecentCallBackendResponse[];
}

export interface FrontDashboardResponse {
  todayTokens?: TokenAmountResponse;
  todayActualTokens?: TokenAmountResponse;
  todayTotalTokens?: TokenAmountResponse;
  monthTokens?: TokenAmountResponse;
  monthActualTokens?: TokenAmountResponse;
  monthTotalTokens?: TokenAmountResponse;
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
  /** 当天全平台峰值并发曲线（5 分钟桶） */
  todayConcurrencyTrends?: ConcurrencyTrendPointResponse[];
  /** 当天单次响应耗时最长的渠道 Top5 */
  dailySlowestChannels?: ChannelLatencyRankingResponse[];
}
