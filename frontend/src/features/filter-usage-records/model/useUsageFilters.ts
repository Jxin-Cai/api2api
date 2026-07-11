import { useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';

import { PROTOCOL_OPTIONS, UPSTREAM_PROTOCOL_OPTIONS } from '@shared/lib/protocols';
import { normalizeUsagePageSize } from '@shared/lib/table';
import type { UsagePageSize } from '@shared/types/table';

import type { UsageRecordFilters, UseUsageFiltersResult } from './types';

const DEFAULT_FILTERS: UsageRecordFilters = {
  page: 1,
  pageSize: 50,
};

const VALID_PROTOCOLS = new Set<string>([
  ...PROTOCOL_OPTIONS.map((option) => option.value),
  ...UPSTREAM_PROTOCOL_OPTIONS.map((option) => option.value),
]);

function getOptionalString(searchParams: URLSearchParams, key: string): string | undefined {
  const value = searchParams.get(key);
  return value && value.trim() ? value.trim() : undefined;
}

function getOptionalPositiveInteger(searchParams: URLSearchParams, key: string): string | undefined {
  const value = getOptionalString(searchParams, key);
  if (!value || !/^\d+$/.test(value)) {
    return undefined;
  }
  return Number(value) > 0 ? value : undefined;
}

function getOptionalProtocol(searchParams: URLSearchParams, key: string): string | undefined {
  const value = getOptionalString(searchParams, key);
  return value && VALID_PROTOCOLS.has(value) ? value : undefined;
}

function getOptionalIsoDate(searchParams: URLSearchParams, key: string): string | undefined {
  const value = getOptionalString(searchParams, key);
  if (!value) {
    return undefined;
  }
  const time = Date.parse(value);
  return Number.isNaN(time) ? undefined : new Date(time).toISOString();
}

function parsePositivePage(value: string | null): number {
  const page = value ? Number(value) : DEFAULT_FILTERS.page;
  return Number.isFinite(page) && page > 0 ? Math.floor(page) : DEFAULT_FILTERS.page;
}

function serializeFilters(filters: UsageRecordFilters): URLSearchParams {
  const nextParams = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]: [string, string | number | undefined]): void => {
    if (value !== undefined && value !== '') {
      nextParams.set(key, String(value));
    }
  });
  return nextParams;
}

export function useUsageFilters(): UseUsageFiltersResult {
  const [searchParams, setSearchParams] = useSearchParams();

  const filters = useMemo((): UsageRecordFilters => {
    const startTime = getOptionalIsoDate(searchParams, 'startTime');
    const endTime = getOptionalIsoDate(searchParams, 'endTime');
    const hasValidTimeRange = !startTime || !endTime || Date.parse(startTime) < Date.parse(endTime);
    return {
      apiCredentialId: getOptionalPositiveInteger(searchParams, 'apiCredentialId'),
      model: getOptionalString(searchParams, 'model'),
      protocolType: getOptionalProtocol(searchParams, 'protocolType'),
      startTime: hasValidTimeRange ? startTime : undefined,
      endTime: hasValidTimeRange ? endTime : undefined,
      userId: getOptionalPositiveInteger(searchParams, 'userId'),
      providerChannelId: getOptionalPositiveInteger(searchParams, 'providerChannelId'),
      providerChannel: getOptionalString(searchParams, 'providerChannel'),
      page: parsePositivePage(searchParams.get('page')),
      pageSize: normalizeUsagePageSize(searchParams.get('pageSize')),
    };
  }, [searchParams]);

  function updateFilters(nextFilters: UsageRecordFilters): void {
    setSearchParams(serializeFilters(nextFilters), { replace: true });
  }

  function setFilter<TKey extends keyof UsageRecordFilters>(key: TKey, value: UsageRecordFilters[TKey]): void {
    updateFilters({ ...filters, [key]: value, page: 1 });
  }

  function setFilters(partial: Partial<UsageRecordFilters>): void {
    updateFilters({ ...filters, ...partial, page: 1 });
  }

  function resetFilters(): void {
    updateFilters(DEFAULT_FILTERS);
  }

  function setPage(page: number, pageSize: UsagePageSize = filters.pageSize): void {
    const normalizedPage = page > 0 ? Math.floor(page) : DEFAULT_FILTERS.page;
    updateFilters({ ...filters, page: normalizedPage, pageSize });
  }

  return { filters, setFilter, setFilters, resetFilters, setPage };
}
