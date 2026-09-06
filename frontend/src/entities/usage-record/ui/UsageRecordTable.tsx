import { Empty, Table, Tooltip, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useState, type ReactElement } from 'react';

import { formatDateTime, formatTokenThousands } from '@shared/lib/formatters';
import { buildTablePagination, USAGE_EMPTY_TEXT } from '@shared/lib/table';
import type { UsagePageSize } from '@shared/types/table';

import type { UsageRecordResponse, UsageScope } from '../model/types';
import { UsageRecordStatusTag } from './UsageRecordStatusTag';

interface UsageRecordTableProps {
  /** 记录列表 */
  records: UsageRecordResponse[];
  /** 前台或后台 */
  scope: UsageScope;
  /** 是否加载中 */
  loading?: boolean;
  /** 分页状态 */
  pagination: { page: number; pageSize: number; total: number };
  /** 是否允许切换每页条数 */
  showSizeChanger?: boolean;
  /** 分页变化回调 */
  onPageChange: (page: number, pageSize: UsagePageSize) => void;
}

export function UsageRecordTable({ records, scope, loading = false, pagination, showSizeChanger = true, onPageChange }: UsageRecordTableProps) {
  const [expandedRowKeys, setExpandedRowKeys] = useState<React.Key[]>([]);

  const columns: ColumnsType<UsageRecordResponse> = [
    { title: '时间', dataIndex: 'createdAt', key: 'createdAt', width: 180, render: (value: string | number): string => formatDateTime(value) },
    { title: 'API Key', dataIndex: 'apiCredentialName', key: 'apiCredentialName', width: 140, render: (_: unknown, record: UsageRecordResponse): string => record.apiCredentialName ?? record.apiCredentialId ?? '-' },
    { title: '模型', dataIndex: 'model', key: 'model', width: 180 },
    { title: '协议', dataIndex: 'protocolType', key: 'protocolType', width: 180 },
    { title: 'Token', key: 'tokens', align: 'right', width: 120, render: (_: unknown, record: UsageRecordResponse): ReactElement => (
      <Tooltip mouseEnterDelay={0.15} placement="top" title={<div className="usage-token-tooltip">输入：{formatTokenThousands(record.inputTokens ?? 0)}<br />输出：{formatTokenThousands(record.outputTokens ?? 0)}<br />缓存创建：{formatTokenThousands(record.cacheCreationInputTokens ?? 0)}<br />缓存命中：{formatTokenThousands(record.cacheReadInputTokens ?? 0)}<br />实际：{formatTokenThousands(record.tokens)}<br />总计：{formatTokenThousands(record.totalTokens)}</div>}>
        <span title="鼠标悬停查看 Token 明细" className="usage-token-value"><Typography.Text className="mono-number" underline>{formatTokenThousands(record.tokens)}</Typography.Text></span>
      </Tooltip>
    ) },
    { title: '首字耗时', dataIndex: 'firstTokenMillis', key: 'firstTokenMillis', align: 'right', width: 110, render: (value: number | undefined): string => value == null ? '-' : `${value} ms` },
    { title: '总耗时', dataIndex: 'durationMillis', key: 'durationMillis', align: 'right', width: 110, render: (value: number | undefined): string => value == null ? '-' : `${value} ms` },
    { title: '状态', dataIndex: 'status', key: 'status', width: 120, render: (value: string | undefined): ReactElement => <UsageRecordStatusTag status={value ?? 'SUCCESS'} /> },
  ];

  if (scope === 'admin') {
    columns.splice(2, 0, { title: '用户', dataIndex: 'username', key: 'username', width: 140, render: (_: unknown, record: UsageRecordResponse): string => record.username ?? record.userId ?? '-' });
    columns.push({ title: '请求 IP', dataIndex: 'clientIp', key: 'clientIp', width: 140, render: (value: string | undefined): string => value ?? '-' });
    columns.push({ title: '渠道', dataIndex: 'providerChannelName', key: 'providerChannelName', width: 160, render: (_: unknown, record: UsageRecordResponse): string => record.providerChannelName ?? record.providerChannel ?? record.providerChannelId ?? '-' });
  }

  return (
    <Table<UsageRecordResponse>
      rowKey="id"
      size="middle"
      loading={loading}
      columns={columns}
      dataSource={records}
      locale={{ emptyText: <Empty description={USAGE_EMPTY_TEXT} /> }}
      pagination={buildTablePagination(pagination, onPageChange, showSizeChanger)}
      scroll={{ x: scope === 'admin' ? 1620 : 1380 }}
      expandable={scope === 'admin' ? {
        expandedRowKeys,
        onExpandedRowsChange: (keys: readonly React.Key[]): void => setExpandedRowKeys([...keys]),
        expandedRowRender: (record: UsageRecordResponse): ReactElement => (
          <Typography.Paragraph>{record.diagnostic || '暂无诊断信息'}</Typography.Paragraph>
        ),
      } : undefined}
    />
  );
}
