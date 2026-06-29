import type { PageResponse } from '@shared/types/table';

export interface UsageRecordBackendResponse {
  id: string | number;
  requestId?: string;
  userAccountId?: string | number;
  apiCredentialId?: string | number;
  requestedModel?: string;
  upstreamModel?: string;
  requestProtocol?: string;
  upstreamProtocol?: string;
  providerChannelId?: string | number;
  status?: string;
  inputTokens?: number;
  outputTokens?: number;
  cacheCreationInputTokens?: number;
  cacheReadInputTokens?: number;
  totalTokens?: number;
  usageKnown?: boolean;
  streaming?: boolean;
  errorType?: string;
  errorMessage?: string;
  diagnostic?: string;
  startedAt?: string | number;
  endedAt?: string | number;
  createdAt?: string | number;
}

export interface UsageRecordBackendPageResponse {
  records: UsageRecordBackendResponse[];
  page: number;
  size: 50 | 100 | 200;
  totalElements: number;
  totalPages: number;
  filteredTotalTokens: number;
  adminView: boolean;
}

export interface UsageRecordResponse {
  id: string;
  apiCredentialId?: string;
  apiCredentialName?: string;
  userId?: string;
  username?: string;
  model: string;
  protocolType: string;
  tokens: number;
  inputTokens?: number;
  outputTokens?: number;
  cacheCreationInputTokens?: number;
  cacheReadInputTokens?: number;
  usageKnown?: boolean;
  status?: string;
  providerChannelId?: string;
  providerChannel?: string;
  providerChannelName?: string;
  diagnostic?: string;
  createdAt: string | number;
}

export interface UsageTokenSummaryResponse {
  totalTokens: number;
  recordCount: number;
}

export interface UsageRecordPageResponse extends PageResponse<UsageRecordResponse> {
  totalTokens: number;
  summary?: UsageTokenSummaryResponse;
}
