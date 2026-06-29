import type { TablePaginationConfig } from 'antd';

import type { TablePaginationState, UsagePageSize } from '@shared/types/table';

export const USAGE_PAGE_SIZE_OPTIONS: UsagePageSize[] = [50, 100, 200];

export const USAGE_EMPTY_TEXT = '当前筛选下暂无调用记录';

export function isUsagePageSize(value: number): value is UsagePageSize {
  return value === 50 || value === 100 || value === 200;
}

export function normalizeUsagePageSize(value: string | number | null | undefined): UsagePageSize {
  const numericValue = typeof value === 'string' ? Number(value) : value;
  return numericValue !== undefined && numericValue !== null && isUsagePageSize(numericValue) ? numericValue : 50;
}

export function buildTablePagination(
  pagination: TablePaginationState,
  onChange: (page: number, pageSize: UsagePageSize) => void
): TablePaginationConfig {
  return {
    current: pagination.page,
    pageSize: pagination.pageSize,
    total: pagination.total,
    showSizeChanger: true,
    pageSizeOptions: USAGE_PAGE_SIZE_OPTIONS.map(String),
    showTotal: (total: number): string => `共 ${total} 条`,
    onChange: (page: number, pageSize: number): void => {
      onChange(page, normalizeUsagePageSize(pageSize));
    },
  };
}
