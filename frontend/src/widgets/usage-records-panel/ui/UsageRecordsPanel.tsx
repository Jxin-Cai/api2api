import { Button, Card, Space } from 'antd';

import { useUsageRecords, UsageRecordTable, UsageTokenSummary, type UsageScope } from '@entities/usage-record';
import { UsagePageSizeSelector, UsageRecordFilterBar, useUsageFilters } from '@features/filter-usage-records';
import { PageState } from '@shared/ui';

interface UsageRecordsPanelProps {
  scope: UsageScope;
}

export function UsageRecordsPanel({ scope }: UsageRecordsPanelProps) {
  const { filters, setFilter, setFilters, resetFilters, setPage } = useUsageFilters();
  const query = useUsageRecords({ scope, filters });
  const data = query.data;

  const retry = (): void => { query.refetch().catch(() => undefined); };

  return (
    <Card>
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <UsageRecordFilterBar
          scope={scope}
          filters={filters}
          onFilterChange={setFilter}
          onFiltersChange={setFilters}
          onReset={resetFilters}
          disabled={query.isFetching}
        />
        <UsagePageSizeSelector value={filters.pageSize} onChange={(pageSize): void => setPage(1, pageSize)} />
        {query.isError ? (
          <PageState
            status="error"
            title="使用记录加载失败"
            description={query.error.message}
            onRetry={retry}
          />
        ) : (
          <>
            <UsageTokenSummary
              scope={scope}
              totalTokens={data?.summary?.totalTokens ?? data?.totalTokens ?? 0}
              recordCount={data?.summary?.recordCount ?? data?.total ?? 0}
              loading={query.isLoading}
            />
            <UsageRecordTable
              scope={scope}
              records={data?.records ?? []}
              loading={query.isLoading || query.isFetching}
              pagination={{ page: filters.page, pageSize: filters.pageSize, total: data?.total ?? 0 }}
              onPageChange={setPage}
            />
          </>
        )}
        {query.isError ? (
          <Button onClick={resetFilters}>重置筛选</Button>
        ) : null}
      </Space>
    </Card>
  );
}
