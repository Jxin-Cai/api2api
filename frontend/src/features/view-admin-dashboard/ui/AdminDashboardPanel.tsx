import { Col, Row, Typography } from 'antd';
import { useMemo } from 'react';

import { MetricCard, SlowestChannelTable, TopRankList, TrendChart, useAdminDashboardMetrics } from '@entities/dashboard-metric';
import { normalizeConcurrencyPoints, normalizeRankItems, normalizeTrendPoints } from '@shared/lib/chartData';
import { formatTokenMillions } from '@shared/lib/formatters';
import { getProtocolMeta } from '@shared/lib/protocols';
import { resolveTimeZone } from '@shared/lib/timeZone';
import { DashboardSummaryGrid, PageState } from '@shared/ui';
import './AdminDashboardPanel.css';

const RECENT_RATE_MINUTES = 5;
const TREND_DAYS = 7;
const PROTOCOLS = ['CLAUDE_MESSAGES', 'OPENAI_RESPONSES', 'OPENAI_CHAT_COMPLETIONS', 'OPENAI_IMAGES', 'AWS_BEDROCK_CLAUDE_MESSAGES'];

export function AdminDashboardPanel() {
  const query = useAdminDashboardMetrics({
    zoneId: resolveTimeZone(),
    recentRateMinutes: RECENT_RATE_MINUTES,
    trendDays: TREND_DAYS,
  });
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
    <div className="admin-dashboard">
      <section className="admin-dashboard__section" aria-labelledby="admin-dashboard-overview">
        <Typography.Title id="admin-dashboard-overview" level={4} className="admin-dashboard__section-title">
          平台概览
        </Typography.Title>
        <DashboardSummaryGrid colProps={{ xs: 24, sm: 12 }}>
          <MetricCard title="全平台今日 Token" value={formatTokenMillions(data?.todayTokens?.tokens)} rawValue={data?.todayTokens?.tokens} loading={query.isLoading} />
          <MetricCard title="全平台本月 Token" value={formatTokenMillions(data?.monthTokens?.tokens)} rawValue={data?.monthTokens?.tokens} loading={query.isLoading} />
        </DashboardSummaryGrid>
      </section>

      <section className="admin-dashboard__section" aria-labelledby="admin-dashboard-protocol-rates">
        <Typography.Title id="admin-dashboard-protocol-rates" level={4} className="admin-dashboard__section-title">
          协议请求速率
        </Typography.Title>
        <div className="admin-dashboard__protocol-grid">
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
        </div>
      </section>

      <section className="admin-dashboard__section" aria-labelledby="admin-dashboard-rankings">
        <Typography.Title id="admin-dashboard-rankings" level={4} className="admin-dashboard__section-title">
          用户 Token 排行
        </Typography.Title>
        <Row gutter={[16, 16]}>
          <Col xs={24} lg={12}>
            <TopRankList title="今日 Top10 用户总 Token" items={normalizeRankItems(data?.dailyTopUsers)} loading={query.isLoading} />
          </Col>
          <Col xs={24} lg={12}>
            <TopRankList title="本月 Top10 用户总 Token" items={normalizeRankItems(data?.monthlyTopUsers)} loading={query.isLoading} />
          </Col>
        </Row>
      </section>

      <section className="admin-dashboard__section" aria-labelledby="admin-dashboard-monitoring">
        <Typography.Title id="admin-dashboard-monitoring" level={4} className="admin-dashboard__section-title">
          实时监控
        </Typography.Title>
        <Row className="admin-dashboard__monitoring-row" gutter={[16, 16]}>
          <Col xs={24} xxl={16} className="admin-dashboard__column">
            <div className="admin-dashboard__chart-block">
              <Typography.Title level={5} className="admin-dashboard__chart-title">
                今日并发曲线（每 5 分钟峰值）
              </Typography.Title>
              <TrendChart
                data={normalizeConcurrencyPoints(data?.todayConcurrencyTrends)}
                loading={query.isLoading}
                valueUnit=""
                valuePrecision={0}
                emptyText="今日暂无请求"
              />
            </div>
          </Col>
          <Col xs={24} xxl={8} className="admin-dashboard__column">
            <SlowestChannelTable title="今日单次响应最慢渠道 Top5" items={data?.dailySlowestChannels ?? []} loading={query.isLoading} />
          </Col>
        </Row>
      </section>

      <section className="admin-dashboard__section" aria-labelledby="admin-dashboard-trends">
        <Typography.Title id="admin-dashboard-trends" level={4} className="admin-dashboard__section-title">
          Token 趋势
        </Typography.Title>
        <Row gutter={[16, 16]}>
          <Col xs={24} xl={12} className="admin-dashboard__column">
            <div className="admin-dashboard__chart-block">
              <Typography.Title level={5} className="admin-dashboard__chart-title">
                近 {TREND_DAYS} 日协议 Token 趋势（M）
              </Typography.Title>
              <TrendChart data={normalizeTrendPoints(data?.protocolTokenTrends)} loading={query.isLoading} />
            </div>
          </Col>
          <Col xs={24} xl={12} className="admin-dashboard__column">
            <div className="admin-dashboard__chart-block">
              <Typography.Title level={5} className="admin-dashboard__chart-title">
                近 {TREND_DAYS} 日供应商渠道 Token 趋势（M）
              </Typography.Title>
              <TrendChart data={normalizeTrendPoints(data?.channelTokenTrends)} loading={query.isLoading} />
            </div>
          </Col>
        </Row>
      </section>
    </div>
  );
}
