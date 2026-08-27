import { Button, Card, Col, Row, Select, Space, Typography } from 'antd';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { useApiCredentials } from '@entities/api-credential';
import {
  MetricCard,
  TopRankList,
  TrendChart,
  useFrontDashboardMetrics,
  useFrontKeyMetrics,
} from '@entities/dashboard-metric';
import { UsageRecordTable } from '@entities/usage-record';
import { ROUTE_PATHS } from '@shared/config/constants';
import { normalizeRankItems, normalizeTrendPoints } from '@shared/lib/chartData';
import { formatTokenMillions } from '@shared/lib/formatters';
import { buildAppUsageQuery } from '@shared/lib/usageQuery';
import { DashboardSummaryGrid, PageState } from '@shared/ui';
import type { FrontDashboardPanelProps } from '../model/types';
import './FrontDashboardPanel.css';

const TREND_DAYS = 7;

export function FrontDashboardPanel({ zoneId }: FrontDashboardPanelProps) {
  const navigate = useNavigate();
  const [selectedCredentialIds, setSelectedCredentialIds] = useState<string[]>([]);
  const query = useFrontDashboardMetrics({ zoneId, recentCallsPage: 1, recentCallsSize: 20 });
  const keyMetricsQuery = useFrontKeyMetrics({ zoneId, trendDays: TREND_DAYS, credentialIds: selectedCredentialIds });
  const { options: credentialOptions } = useApiCredentials();
  const data = query.data;
  const keyMetrics = keyMetricsQuery.data;

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

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <TopRankList title="今日 Top10 Key Token" items={normalizeRankItems(keyMetrics?.dailyTopCredentials)} loading={keyMetricsQuery.isLoading} />
        </Col>
        <Col xs={24} lg={12}>
          <TopRankList title="本月 Top10 Key Token" items={normalizeRankItems(keyMetrics?.monthlyTopCredentials)} loading={keyMetricsQuery.isLoading} />
        </Col>
      </Row>

      <Space direction="vertical" size={12} style={{ width: '100%' }}>
        <div className="front-dashboard-trend__head">
          <Typography.Title level={4} className="front-dashboard-trend__title">近 {TREND_DAYS} 日 Key Token 趋势（M）</Typography.Title>
          <Select
            mode="multiple"
            allowClear
            placeholder="全部 Key（可多选筛选）"
            className="front-dashboard-trend__filter"
            maxTagCount="responsive"
            options={credentialOptions}
            value={selectedCredentialIds}
            onChange={(values: string[]): void => { setSelectedCredentialIds(values); }}
          />
        </div>
        {keyMetricsQuery.isError ? (
          <PageState
            status="error"
            title="Key 趋势加载失败"
            description={keyMetricsQuery.error.message}
            onRetry={(): void => { keyMetricsQuery.refetch().catch(() => undefined); }}
          />
        ) : (
          <TrendChart data={normalizeTrendPoints(keyMetrics?.credentialTokenTrends)} loading={keyMetricsQuery.isLoading} />
        )}
      </Space>

      <Card
        className="surface-card"
        title="最近调用"
        extra={<Button type="link" onClick={(): void => { void navigate(`${ROUTE_PATHS.appUsage}${buildAppUsageQuery({})}`); }}>查看全部</Button>}
      >
        <UsageRecordTable
          scope="front"
          records={data?.recentCalls ?? []}
          loading={query.isLoading}
          pagination={{ page: 1, pageSize: 20, total: data?.recentCalls?.length ?? 0 }}
          showSizeChanger={false}
          onPageChange={(): void => undefined}
        />
        {data?.recentCalls?.length === 0 ? <Typography.Text type="secondary">暂无最近调用数据</Typography.Text> : null}
      </Card>
    </Space>
  );
}
