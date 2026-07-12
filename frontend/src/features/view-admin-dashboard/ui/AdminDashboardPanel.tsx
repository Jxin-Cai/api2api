import { Col, Row, Space, Typography } from 'antd';
import { useMemo } from 'react';

import { MetricCard, TopRankList, TrendChart, useAdminDashboardMetrics } from '@entities/dashboard-metric';
import { normalizeRankItems, normalizeTrendPoints } from '@shared/lib/chartData';
import { formatTokenThousands } from '@shared/lib/formatters';
import { getProtocolMeta } from '@shared/lib/protocols';
import { DashboardSummaryGrid, PageState } from '@shared/ui';

const RECENT_RATE_MINUTES = 5;
const TREND_DAYS = 7;
const PROTOCOLS = ['CLAUDE_MESSAGES', 'OPENAI_RESPONSES', 'OPENAI_CHAT_COMPLETIONS'];

export function AdminDashboardPanel() {
  const query = useAdminDashboardMetrics({ recentRateMinutes: RECENT_RATE_MINUTES, trendDays: TREND_DAYS });
  const data = query.data;

  const rateByProtocol = useMemo((): Map<string, { requestCount: number; requestsPerMinute: number }> => {
    return new Map(
      (data?.protocolRequestRates ?? []).map((item) => [
        item.protocol,
        { requestCount: item.requestCount, requestsPerMinute: item.requestsPerMinute },
      ])
    );
  }, [data?.protocolRequestRates]);

  if (query.isError) {
    return (
      <PageState
        status="error"
        title="后台仪表盘加载失败"
        description={query.error.message}
        onRetry={(): void => { query.refetch().catch(() => undefined); }}
      />
    );
  }

  return (
    <Space direction="vertical" size={20} style={{ width: '100%' }}>
      <DashboardSummaryGrid colProps={{ xs: 24, sm: 12, xl: 6 }}>
        <MetricCard title="全平台今日 Token" value={formatTokenThousands(data?.todayTokens?.tokens)} rawValue={data?.todayTokens?.tokens} loading={query.isLoading} />
        <MetricCard title="全平台本月 Token" value={formatTokenThousands(data?.monthTokens?.tokens)} rawValue={data?.monthTokens?.tokens} loading={query.isLoading} />
        {PROTOCOLS.map((protocol) => {
          const meta = getProtocolMeta(protocol);
          const rate = rateByProtocol.get(protocol);
          return (
            <MetricCard
              key={protocol}
              title={`${meta.label} 请求速率`}
              value={rate?.requestsPerMinute?.toFixed(2) ?? 0}
              unit="rpm"
              trend={{ value: rate?.requestCount ?? 0, direction: 'flat', label: `近 ${RECENT_RATE_MINUTES} 分钟请求` }}
              loading={query.isLoading}
            />
          );
        })}
      </DashboardSummaryGrid>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <TopRankList title="今日 Top10 用户 Token" items={normalizeRankItems(data?.dailyTopUsers)} loading={query.isLoading} />
        </Col>
        <Col xs={24} lg={12}>
          <TopRankList title="本月 Top10 用户 Token" items={normalizeRankItems(data?.monthlyTopUsers)} loading={query.isLoading} />
        </Col>
      </Row>

      <Space direction="vertical" size={12} style={{ width: '100%' }}>
        <Typography.Title level={4} style={{ margin: 0 }}>近 {TREND_DAYS} 日协议 Token 趋势（k）</Typography.Title>
        <TrendChart data={normalizeTrendPoints(data?.protocolTokenTrends)} loading={query.isLoading} />
      </Space>

      <Space direction="vertical" size={12} style={{ width: '100%' }}>
        <Typography.Title level={4} style={{ margin: 0 }}>近 {TREND_DAYS} 日供应商渠道 Token 趋势（k）</Typography.Title>
        <TrendChart data={normalizeTrendPoints(data?.channelTokenTrends)} loading={query.isLoading} />
      </Space>
    </Space>
  );
}
