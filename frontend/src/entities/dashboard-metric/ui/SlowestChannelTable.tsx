import { Empty, Skeleton, Table, Tag, Tooltip, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { ReactElement } from 'react';

import { SpotlightCard } from '@shared/ui';

import type { ChannelLatencyRankingResponse } from '../model/types';
import './TopRankList.css';

interface SlowestChannelTableProps {
  /** 排行数据（已按最长单次耗时降序） */
  items: ChannelLatencyRankingResponse[];
  /** 标题 */
  title: string;
  /** 是否加载中 */
  loading?: boolean;
}

/** 将毫秒格式化为可读耗时：<1s 显示 ms，否则显示秒（保留 1 位小数） */
export function formatDurationMillis(millis: number | undefined): string {
  const value = Number(millis ?? 0);
  if (!Number.isFinite(value) || value < 0) {
    return '-';
  }
  if (value < 1000) {
    return `${Math.round(value)} ms`;
  }
  return `${(value / 1000).toFixed(1)} s`;
}

export function SlowestChannelTable({ items, title, loading = false }: SlowestChannelTableProps) {
  const columns: ColumnsType<ChannelLatencyRankingResponse> = [
    {
      title: '#',
      key: 'rank',
      width: 48,
      render: (_: unknown, item, index): ReactElement => <span className="top-rank-list__rank">{item.rank ?? index + 1}</span>,
    },
    {
      title: '渠道',
      key: 'channel',
      ellipsis: true,
      render: (_: unknown, item): ReactElement => (
        <Tooltip title={item.providerChannelId !== undefined ? `渠道 ID：${item.providerChannelId}` : undefined}>
          <Typography.Text strong>{item.providerChannelName ?? String(item.providerChannelId ?? '未命名渠道')}</Typography.Text>
        </Tooltip>
      ),
    },
    {
      title: '最长总耗时',
      key: 'max-total',
      width: 130,
      align: 'right',
      render: (_: unknown, item): ReactElement => <Tag color="volcano" className="mono-number">{formatDurationMillis(item.maxDurationMillis)}</Tag>,
    },
    {
      title: '最长首字耗时',
      key: 'max-first-token',
      width: 130,
      align: 'right',
      render: (_: unknown, item): ReactElement => <Tag color="gold" className="mono-number">{formatDurationMillis(item.maxFirstTokenMillis)}</Tag>,
    },
    {
      title: '平均总耗时',
      key: 'avg-total',
      width: 110,
      align: 'right',
      render: (_: unknown, item): ReactElement => <span className="mono-number">{formatDurationMillis(item.avgDurationMillis)}</span>,
    },
    {
      title: '今日请求',
      key: 'count',
      width: 100,
      align: 'right',
      render: (_: unknown, item): ReactElement => <span className="mono-number">{(item.requestCount ?? 0).toLocaleString('zh-CN')}</span>,
    },
  ];

  return (
    <SpotlightCard className="top-rank-list">
      <div className="top-rank-list__head">{title}</div>
      {loading ? (
        <Skeleton active paragraph={{ rows: 5 }} />
      ) : items.length === 0 ? (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="今日暂无已完成的渠道调用" />
      ) : (
        <Table<ChannelLatencyRankingResponse>
          size="small"
          rowKey={(item): string => String(item.providerChannelId ?? item.rank ?? item.providerChannelName)}
          columns={columns}
          dataSource={items.slice(0, 5)}
          pagination={false}
        />
      )}
    </SpotlightCard>
  );
}
