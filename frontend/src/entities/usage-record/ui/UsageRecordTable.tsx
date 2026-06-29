import { Empty, Table, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useState, type ReactElement } from 'react';

import { formatDateTime } from '@shared/lib/formatters';
import { buildTablePagination, USAGE_EMPTY_TEXT } from '@shared/lib/table';
import type { TablePaginationState, UsagePageSize } from '@shared/types/table';

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
  pagination: TablePaginationState;
  /** 分页变化回调 */
  onPageChange: (page: number, pageSize: UsagePageSize) => void;
}

export function UsageRecordTable({ records, scope, loading = false, pagination, onPageChange }: UsageRecordTableProps) {
  const [expandedRowKeys, setExpandedRowKeys] = useState<React.Key[]>([]);

  const columns: ColumnsType<UsageRecordResponse> = [
    { title: '时间', dataIndex: 'createdAt', key: 'createdAt', width: 180, render: (value: string | number): string => formatDateTime(value) },
    { title: 'API Key', dataIndex: 'apiCredentialName', key: 'apiCredentialName', width: 140, render: (_: unknown, record: UsageRecordResponse): string => record.apiCredentialName ?? record.apiCredentialId ?? '-' },
    { title: '模型', dataIndex: 'model', key: 'model', width: 180 },
    { title: '协议', dataIndex: 'protocolType', key: 'protocolType', width: 180 },
    { title: '输入 Token', dataIndex: 'inputTokens', key: 'inputTokens', align: 'right', width: 120, render: (value: number | undefined): number => value ?? 0 },
    { title: '输出 Token', dataIndex: 'outputTokens', key: 'outputTokens', align: 'right', width: 120, render: (value: number | undefined): number => value ?? 0 },
    { title: '缓存创建输入', dataIndex: 'cacheCreationInputTokens', key: 'cacheCreationInputTokens', align: 'right', width: 140, render: (value: number | undefined): number => value ?? 0 },
    { title: '缓存命中输入', dataIndex: 'cacheReadInputTokens', key: 'cacheReadInputTokens', align: 'right', width: 140, render: (value: number | undefined): number => value ?? 0 },
    { title: '总 Token', dataIndex: 'tokens', key: 'tokens', align: 'right', width: 120 },
    { title: '状态', dataIndex: 'status', key: 'status', width: 120, render: (value: string | undefined): ReactElement => <UsageRecordStatusTag status={value ?? 'SUCCESS'} /> },
  ];

  if (scope === 'admin') {
    columns.splice(2, 0, { title: '用户', dataIndex: 'username', key: 'username', width: 140, render: (_: unknown, record: UsageRecordResponse): string => record.username ?? record.userId ?? '-' });
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
      pagination={buildTablePagination(pagination, onPageChange)}
      scroll={{ x: scope === 'admin' ? 1500 : 1260 }}
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
