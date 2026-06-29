import type {
  UsageFilterOption,
  UsagePageSize,
  UsageQueryParams,
  UsageQueryValue,
  UsageRecordFilters,
  UsageScope,
} from '@entities/usage-record';

export type { UsageFilterOption, UsagePageSize, UsageQueryParams, UsageQueryValue, UsageRecordFilters, UsageScope };

export interface UseUsageFiltersResult {
  filters: UsageRecordFilters;
  setFilter: <TKey extends keyof UsageRecordFilters>(key: TKey, value: UsageRecordFilters[TKey]) => void;
  setFilters: (partial: Partial<UsageRecordFilters>) => void;
  resetFilters: () => void;
  setPage: (page: number, pageSize?: UsagePageSize) => void;
}
