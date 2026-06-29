import { Button, Card, Space, Typography } from 'antd';
import { useNavigate } from 'react-router-dom';

import { MetricCard, useFrontDashboardMetrics } from '@entities/dashboard-metric';
import { UsageRecordTable } from '@entities/usage-record';
import { ROUTE_PATHS } from '@shared/config/constants';
import { formatTokenMillions } from '@shared/lib/formatters';
import { buildAppUsageQuery } from '@shared/lib/usageQuery';
import { DashboardSummaryGrid, PageState } from '@shared/ui';
import type { FrontDashboardPanelProps } from '../model/types';

export function FrontDashboardPanel({ zoneId }: FrontDashboardPanelProps) {
  const navigate = useNavigate();
  const query = useFrontDashboardMetrics({ zoneId, recentCallsPage: 1, recentCallsSize: 50 });
  const data = query.data;

  if (query.isError) {
    return <PageState status="error" title="前台仪表盘加载失败" description={query.error.message} onRetry={(): void => { query.refetch().catch(() => undefined); }} />;
  }

  return (
    <Space direction="vertical" size={20} style={{ width: '100%' }}>
      <DashboardSummaryGrid>
        <MetricCard title="今日 Token" value={formatTokenMillions(data?.todayTokens?.tokens)} rawValue={data?.todayTokens?.tokens} loading={query.isLoading} />
        <MetricCard title="近 30 日 Token" value={formatTokenMillions(data?.monthTokens?.tokens)} rawValue={data?.monthTokens?.tokens} loading={query.isLoading} />
        <MetricCard title="API Key 数量" value={data?.apiKeyCount ?? 0} loading={query.isLoading} />
      </DashboardSummaryGrid>
      <Card
        className="surface-card"
        title="最近调用"
        extra={<Button type="link" onClick={(): void => { void navigate(`${ROUTE_PATHS.appUsage}${buildAppUsageQuery({})}`); }}>查看全部</Button>}
      >
        <UsageRecordTable
          scope="front"
          records={data?.recentCalls ?? []}
          loading={query.isLoading}
          pagination={{ page: 1, pageSize: 50, total: data?.recentCalls?.length ?? 0 }}
          onPageChange={(): void => undefined}
        />
        {data?.recentCalls?.length === 0 ? <Typography.Text type="secondary">暂无最近调用数据</Typography.Text> : null}
      </Card>
    </Space>
  );
}
