import { Alert, Button, Card, Col, Descriptions, Progress, Row, Space, Switch, Tag, Typography } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { useEffect, useState } from 'react';
import { SlowestChannelTable, TrendChart } from '@entities/dashboard-metric';
import { normalizeConcurrencyPoints } from '@shared/lib/chartData';
import type { TrendChartPoint } from '@shared/types/chart';
import { PageHeader, PageState } from '@shared/ui';
import { useOperationsTraffic, useSystemMetrics, type Resource, type SystemSnapshot } from '../api/operationsApi';
import './OperationsPanel.css';

const HEALTH = {
  HEALTHY: { color: 'success', label: '资源健康', description: '当前资源水位在观察阈值内，未发现数据库连接等待。' },
  WARNING: { color: 'warning', label: '需要关注', description: '部分资源已达预警水位，或数据库连接池存在等待。' },
  CRITICAL: { color: 'error', label: '资源压力高', description: '部分资源已达高危水位，请及时检查容量与请求负载。' },
  UNKNOWN: { color: 'default', label: '指标不完整', description: '部分指标暂不可用，不能据此判断整体资源健康。' },
} as const;

function percent(value: number | null | undefined): string {
  return value == null ? '不可用' : `${value.toFixed(1)}%`;
}

function bytes(value: number | null | undefined): string {
  if (value == null) return '—';
  return value >= 1024 ** 3 ? `${(value / 1024 ** 3).toFixed(1)} GiB` : `${(value / 1024 ** 2).toFixed(0)} MiB`;
}

function resourceDetail(resource: Resource | undefined): string {
  return `${bytes(resource?.usedBytes)} / ${bytes(resource?.totalBytes)}`;
}

function uptime(milliseconds: number | undefined): string {
  if (milliseconds == null) return '—';
  const minutes = Math.floor(milliseconds / 60_000);
  return `${Math.floor(minutes / 1440)} 天 ${Math.floor(minutes / 60) % 24} 小时 ${minutes % 60} 分钟`;
}

function ResourceCard({ title, value, detail, loading, isLoad = false }: {
  title: string; value: number | null | undefined; detail: string; loading: boolean; isLoad?: boolean;
}) {
  const danger = value != null && value >= (isLoad ? 150 : 95);
  const warning = value != null && value >= (isLoad ? 100 : 80);
  return <Card className="operations__resource" loading={loading}>
    <div className="operations__resource-title">{title}</div>
    <div className="operations__resource-value">{percent(value)}</div>
    <Progress percent={value == null ? 0 : Math.min(100, value)} showInfo={false}
      strokeColor={danger ? 'var(--error-color)' : warning ? 'var(--warning-color)' : 'var(--success-color)'}
      aria-label={`${title} ${percent(value)}`} />
    <div className="operations__resource-detail">{detail}</div>
    <Tag color={value == null ? 'default' : danger ? 'error' : warning ? 'warning' : 'success'}>
      {value == null ? '待采集' : danger ? '高危' : warning ? '预警' : '正常'}
    </Tag>
  </Card>;
}

export function OperationsPanel() {
  const [live, setLive] = useState(true);
  const system = useSystemMetrics(live);
  const traffic = useOperationsTraffic(live);
  const [history, setHistory] = useState<SystemSnapshot[]>([]);
  const data = system.data;

  useEffect(() => {
    if (!data) return;
    setHistory(previous => {
      if (previous.at(-1)?.sampledAt === data.sampledAt) return previous;
      return [...previous, data].slice(-60);
    });
  }, [data]);

  const stale = system.isError;
  const health = HEALTH[stale ? 'UNKNOWN' : data?.health ?? 'UNKNOWN'];
  const pool = data?.databasePool;
  const resourceTrends: TrendChartPoint[] = history.flatMap(sample =>
    ([['CPU', sample.cpuPercent], ['内存', sample.memory.percent], ['JVM 堆', sample.runtime.heap.percent]] as const)
      .filter((item): item is readonly ['CPU' | '内存' | 'JVM 堆', number] => item[1] != null)
      .map(([category, value]) => ({ date: new Date(sample.sampledAt).toLocaleTimeString('zh-CN', { hour12: false }), value, category }))
  );

  return <div className="app-page operations">
    <PageHeader title="运维监控" description="集中观测服务资源、负载压力与请求表现。"
      extra={<Space wrap>
        <Switch checked={live} onChange={setLive} aria-label="自动刷新运维监控" />
        <span>{live ? '自动刷新' : '已暂停自动刷新'}</span>
        <Button icon={<ReloadOutlined />} loading={system.isFetching || traffic.isFetching}
          onClick={() => { void system.refetch(); void traffic.refetch(); }}>刷新</Button>
      </Space>} />

    <section className="operations__health" aria-label="服务健康概览" aria-live="polite">
      <div><Space wrap><Typography.Title level={4}>服务健康概览</Typography.Title>
        <Tag color={health.color}>{system.isLoading ? '正在采集' : stale ? '数据已过期' : health.label}</Tag>
      </Space><p>{stale ? '采集请求失败，下方保留上次数据，不代表当前状态。' : health.description}</p></div>
      <div className="operations__sample"><span>最近资源采样</span>
        <strong>{data ? new Date(data.sampledAt).toLocaleString('zh-CN', { hour12: false }) : '等待采集'}</strong>
        <span>资源 10 秒 · 请求统计 30 秒</span></div>
    </section>

    {system.isError && <Alert type="error" showIcon message="资源监控加载失败" description={system.error.message} />}
    {data?.notices.length ? <Alert type="info" showIcon message="采集说明" description={data.notices.join('；')} /> : null}

    <section aria-labelledby="operations-resources">
      <div className="operations__section-head"><Typography.Title level={4} id="operations-resources">宿主机与资源水位</Typography.Title>
        <Tag>{!data ? '等待采集' : data.scope === 'HOST_KERNEL' ? 'Linux 宿主机内核视图' : 'JVM 可见系统'}</Tag></div>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} xl={6}><ResourceCard title="CPU 使用率" value={data?.cpuPercent}
          detail={`${data?.cpuCores ?? '—'} 个逻辑核心 · 相邻采样区间`} loading={system.isLoading} /></Col>
        <Col xs={24} sm={12} xl={6}><ResourceCard title="内存水位" value={data?.memory.percent}
          detail={`${resourceDetail(data?.memory)} · 已用 / 总量`} loading={system.isLoading} /></Col>
        <Col xs={24} sm={12} xl={6}><ResourceCard title="文件系统水位" value={data?.disk.percent}
          detail={`${resourceDetail(data?.disk)} · 已用 / 总量`} loading={system.isLoading} /></Col>
        <Col xs={24} sm={12} xl={6}><ResourceCard title="负载 / 核心数" value={data?.loadPercent}
          detail={`1 分钟负载 ${data?.loadAverage[0]?.toFixed(2) ?? '—'} · 100% 为每核负载 1`} loading={system.isLoading} isLoad /></Col>
      </Row>
      <p className="operations__note">资源 ≥80% 预警、≥95% 高危；每核负载 ≥1 预警、≥1.5 高危。磁盘仅为目录 {data?.diskScope ?? '/'} 所在文件系统，并非全部磁盘。容器部署时，宿主机资源不代表容器配额余量。</p>
    </section>

    <Row gutter={[16, 16]}>
      <Col xs={24} xl={14}><Card title="资源变化趋势" className="operations__full-height">
        <TrendChart data={resourceTrends} valueUnit="%" height={250} emptyText="等待资源采样" loading={system.isLoading} />
        <p className="operations__note">仅保留本页面最近 60 个采样点，刷新页面后重置；未提供长期历史存储。</p>
      </Card></Col>
      <Col xs={24} xl={10}><Card title="运行时与依赖" loading={system.isLoading} className="operations__full-height">
        <Descriptions column={1} size="small" items={[
          { key: 'os', label: '操作系统', children: data?.operatingSystem ?? '—' },
          { key: 'uptime', label: '服务运行时间', children: uptime(data?.runtime.uptimeMillis) },
          { key: 'load', label: '负载 1 / 5 / 15 分钟', children: data?.loadAverage.length ? data.loadAverage.map(value => value.toFixed(2)).join(' / ') : '不可用' },
          { key: 'heap', label: 'JVM 堆', children: `${resourceDetail(data?.runtime.heap)}（${percent(data?.runtime.heap.percent)}）` },
          { key: 'cpu', label: '进程 CPU', children: percent(data?.runtime.processCpuPercent) },
          { key: 'threads', label: 'JVM 线程', children: data?.runtime.threads ?? '—' },
          { key: 'pool', label: '数据库连接', children: pool ? `${pool.active} 活跃 / ${pool.idle} 空闲 / ${pool.maximum} 上限` : '不可用' },
          { key: 'waiting', label: '等待数据库连接', children: pool ? <Tag color={pool.waiting > 0 ? 'warning' : 'success'}>{pool.waiting} 个线程</Tag> : '不可用' },
        ]} />
      </Card></Col>
    </Row>

    <section aria-labelledby="operations-traffic">
      <div className="operations__section-head"><Typography.Title level={4} id="operations-traffic">并发与响应耗时</Typography.Title>
        <Typography.Text type="secondary">{traffic.data ? `${traffic.data.zoneId} · ${new Date(traffic.data.sampledAt).toLocaleTimeString('zh-CN', { hour12: false })} 更新` : '今日请求统计'}</Typography.Text></div>
      {traffic.isError && <Alert type="error" showIcon message="请求监控加载失败" description={`${traffic.error.message}。已有数据如仍显示，均为上次成功结果。`} />}
      {!traffic.data && traffic.isError ? <PageState status="error" title="暂无可展示的请求监控数据" onRetry={() => { void traffic.refetch(); }} /> : <>
        <Card title="今日并发曲线（每 5 分钟峰值）">
          <TrendChart data={normalizeConcurrencyPoints(traffic.data?.todayConcurrencyTrends)} loading={traffic.isLoading}
            valueUnit="" valuePrecision={0} emptyText="今日暂无请求" />
        </Card>
        <div className="operations__latency"><SlowestChannelTable title="今日单次响应最慢渠道 Top5"
          items={traffic.data?.dailySlowestChannels ?? []} loading={traffic.isLoading} /></div>
      </>}
      <p className="operations__note">并发统计沿用使用记录中的在途区间；响应耗时展示最长总耗时、首字耗时及平均总耗时。资源健康为容量参考，不等同于上游可用性或端到端 SLA。</p>
    </section>
  </div>;
}
