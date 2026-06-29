import type { UsageRecordPageResponse, UsageRecordResponse, UsageTokenSummaryResponse } from '@shared/api/contracts';
import type { UsagePageSize } from '@shared/types/table';

export type { UsagePageSize, UsageRecordPageResponse, UsageRecordResponse, UsageTokenSummaryResponse };

export type UsageScope = 'front' | 'admin';

export interface UsageRecordFilters {
  apiCredentialId?: string;
  model?: string;
  protocolType?: string;
  startTime?: string;
  endTime?: string;
  userId?: string;
  providerChannelId?: string;
  providerChannel?: string;
  page: number;
  pageSize: UsagePageSize;
}

export interface UsageFilterOption {
  label: string;
  value: string;
}

export type UsageQueryValue = string | number | undefined;
export type UsageQueryParams = Partial<Record<keyof UsageRecordFilters, UsageQueryValue>>;

export interface QueryMyUsageRecordsRequest {
  apiCredentialId?: string;
  model?: string;
  protocolType?: string;
  startTime?: string;
  endTime?: string;
  page: number;
  pageSize: UsagePageSize;
}

export interface QueryAdminUsageRecordsRequest extends QueryMyUsageRecordsRequest {
  userId?: string;
  providerChannelId?: string;
  providerChannel?: string;
}

export type QueryUsageRecordsRequest = QueryMyUsageRecordsRequest | QueryAdminUsageRecordsRequest;
