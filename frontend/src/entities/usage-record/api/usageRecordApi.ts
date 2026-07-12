import { apiClient, type ApiResponse, type QueryParams } from '@shared/api';
import type { UsageRecordBackendPageResponse, UsageRecordBackendResponse } from '@shared/api/contracts';
import type { UsagePageSize } from '@shared/types/table';

import type {
  QueryAdminUsageRecordsRequest,
  QueryMyUsageRecordsRequest,
  QueryUsageRecordsRequest,
  UsageRecordPageResponse,
  UsageRecordResponse,
} from '../model/types';

type BackendUsageQueryParams = QueryParams & {
  apiCredentialId?: string;
  requestedModel?: string;
  requestProtocol?: string;
  startInclusive?: string;
  endExclusive?: string;
  userAccountId?: string;
  providerChannelId?: string;
  page: number;
  size: UsagePageSize;
};

function cleanOptionalString(value: string | undefined): string | undefined {
  const trimmed = value?.trim();
  return trimmed ? trimmed : undefined;
}

function cleanPositiveInteger(value: string | undefined): string | undefined {
  const trimmed = cleanOptionalString(value);
  if (!trimmed || !/^\d+$/.test(trimmed)) {
    return undefined;
  }
  return Number(trimmed) > 0 ? trimmed : undefined;
}

function cleanIsoDate(value: string | undefined): string | undefined {
  const trimmed = cleanOptionalString(value);
  if (!trimmed) {
    return undefined;
  }
  const time = Date.parse(trimmed);
  return Number.isNaN(time) ? undefined : new Date(time).toISOString();
}

function toBackendParams(params: QueryUsageRecordsRequest): BackendUsageQueryParams {
  const startInclusive = cleanIsoDate(params.startTime);
  const endExclusive = cleanIsoDate(params.endTime);
  const hasValidTimeRange = !startInclusive || !endExclusive || Date.parse(startInclusive) < Date.parse(endExclusive);
  return {
    apiCredentialId: cleanPositiveInteger(params.apiCredentialId),
    requestedModel: cleanOptionalString(params.model),
    requestProtocol: cleanOptionalString(params.protocolType),
    startInclusive: hasValidTimeRange ? startInclusive : undefined,
    endExclusive: hasValidTimeRange ? endExclusive : undefined,
    userAccountId: 'userId' in params ? cleanPositiveInteger(params.userId) : undefined,
    providerChannelId: 'providerChannelId' in params ? cleanPositiveInteger(params.providerChannelId) : undefined,
    page: params.page,
    size: params.pageSize,
  };
}

function toStringValue(value: string | number | undefined): string | undefined {
  return value === undefined || value === null ? undefined : String(value);
}

function buildDiagnostic(record: UsageRecordBackendResponse): string | undefined {
  if (record.diagnostic) {
    return record.diagnostic;
  }
  const parts = [record.errorType, record.errorMessage].filter(Boolean);
  return parts.length > 0 ? parts.join(': ') : undefined;
}

function toFrontendRecord(record: UsageRecordBackendResponse): UsageRecordResponse {
  return {
    id: String(record.id),
    apiCredentialId: toStringValue(record.apiCredentialId),
    apiCredentialName: record.apiCredentialName,
    userId: toStringValue(record.userAccountId),
    username: record.username,
    model: record.requestedModel ?? '-',
    protocolType: record.requestProtocol ?? '-',
    tokens: record.totalTokens ?? 0,
    inputTokens: record.inputTokens,
    outputTokens: record.outputTokens,
    cacheCreationInputTokens: record.cacheCreationInputTokens,
    cacheReadInputTokens: record.cacheReadInputTokens,
    usageKnown: record.usageKnown,
    status: record.status,
    providerChannelId: toStringValue(record.providerChannelId),
    providerChannelName: record.providerChannelName,
    diagnostic: buildDiagnostic(record),
    createdAt: record.createdAt ?? record.startedAt ?? '-',
  };
}

function toFrontendPage(response: UsageRecordBackendPageResponse): UsageRecordPageResponse {
  const records = Array.isArray(response?.records) ? response.records : [];
  const page = Number.isFinite(response?.page) ? response.page : 1;
  const size = response?.size === 100 || response?.size === 200 ? response.size : 50;
  const totalElements = Number.isFinite(response?.totalElements) ? response.totalElements : records.length;
  const filteredTotalTokens = Number.isFinite(response?.filteredTotalTokens) ? response.filteredTotalTokens : 0;
  return {
    records: records.map(toFrontendRecord),
    page,
    pageSize: size,
    total: totalElements,
    totalTokens: filteredTotalTokens,
    summary: {
      totalTokens: filteredTotalTokens,
      recordCount: totalElements,
    },
  };
}

async function queryUsageRecords(
  path: string,
  params: QueryUsageRecordsRequest
): Promise<ApiResponse<UsageRecordPageResponse>> {
  const response = await apiClient.get<UsageRecordBackendPageResponse>(path, toBackendParams(params));
  return {
    ...response,
    data: toFrontendPage(response.data),
  };
}

export async function queryMyUsageRecords(
  params: QueryMyUsageRecordsRequest
): Promise<ApiResponse<UsageRecordPageResponse>> {
  return queryUsageRecords('/api/usage-records', params);
}

export async function queryAdminUsageRecords(
  params: QueryAdminUsageRecordsRequest
): Promise<ApiResponse<UsageRecordPageResponse>> {
  return queryUsageRecords('/api/admin/usage-records', params);
}
