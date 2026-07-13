import type { PageResponse } from '@shared/types/table';

export interface UsageRecordBackendResponse {
  id: string | number;
  requestId?: string;
  userAccountId?: string | number;
  username?: string;
  apiCredentialId?: string | number;
  apiCredentialName?: string;
  requestedModel?: string;
  upstreamModel?: string;
  requestProtocol?: string;
  upstreamProtocol?: string;
  providerChannelId?: string | number;
  providerChannelName?: string;
  status?: string;
  inputTokens?: number;
  outputTokens?: number;
  cacheCreationInputTokens?: number;
  cacheReadInputTokens?: number;
  totalTokens?: number;
  actualTokens?: number;
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
  totalTokens: number;
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

function toStringValue(value: string | number | undefined): string | undefined {
  return value === undefined || value === null ? undefined : String(value);
}

function buildUsageDiagnostic(record: UsageRecordBackendResponse): string | undefined {
  if (record.diagnostic) {
    return record.diagnostic;
  }
  const parts = [record.errorType, record.errorMessage].filter(Boolean);
  return parts.length > 0 ? parts.join(': ') : undefined;
}

/** Normalizes usage rows shared by the usage page and dashboard recent calls. */
export function toUsageRecordResponse(record: UsageRecordBackendResponse): UsageRecordResponse {
  return {
    id: String(record.id),
    apiCredentialId: toStringValue(record.apiCredentialId),
    apiCredentialName: record.apiCredentialName,
    userId: toStringValue(record.userAccountId),
    username: record.username,
    model: record.requestedModel ?? '-',
    protocolType: record.requestProtocol ?? '-',
    tokens: record.actualTokens ?? record.totalTokens ?? 0,
    totalTokens: record.totalTokens ?? 0,
    inputTokens: record.inputTokens,
    outputTokens: record.outputTokens,
    cacheCreationInputTokens: record.cacheCreationInputTokens,
    cacheReadInputTokens: record.cacheReadInputTokens,
    usageKnown: record.usageKnown,
    status: record.status,
    providerChannelId: toStringValue(record.providerChannelId),
    providerChannelName: record.providerChannelName,
    diagnostic: buildUsageDiagnostic(record),
    createdAt: record.createdAt ?? record.startedAt ?? '-',
  };
}
