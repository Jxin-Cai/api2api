import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { Alert, App, Button, Card, Empty, Input, Space, Table, Tabs, Tag, Tooltip, Typography, theme } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { deleteQuota, QUOTA_QUERY_KEY, useTokenQuotas, type QuotaRule, type QuotaScope } from '@entities/token-quota';
import { PageState } from '@shared/ui';
import { PERIOD_LABELS, QuotaFormModal } from './QuotaFormModal';

function millions(value: number): string { return Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 6 }); }
export function TokenQuotaPanel() {
  const { token } = theme.useToken();
  const { query, rules, targets, zoneId } = useTokenQuotas();
  const { modal, message } = App.useApp();
  const client = useQueryClient();
  const [scope, setScope] = useState<QuotaScope>('MODEL_GROUP');
  const [search, setSearch] = useState('');
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<QuotaRule | null>(null);
  const deletion = useMutation({
    mutationFn: deleteQuota,
    onSuccess: async () => {
      await client.invalidateQueries({ queryKey: QUOTA_QUERY_KEY });
      await client.invalidateQueries({ queryKey: ['apiCredentials'] });
      message.success('限流规则已删除');
    },
    onError: (error: Error) => message.error(error.message),
  });
  const targetFor = (rule: QuotaRule) => targets.find((target) => target.scope === rule.scope && target.id === rule.targetId);
  const filtered = rules.filter((rule) => rule.scope === scope && `${targetFor(rule)?.name ?? ''} ${targetFor(rule)?.ownerName ?? ''} ${rule.targetId} ${rule.model}`.toLowerCase().includes(search.toLowerCase()));
  const columns: ColumnsType<QuotaRule> = [
    { title: '限流对象', key: 'target', width: 240, render: (_, rule) => <Space direction="vertical" size={0}>
      <Typography.Text strong>{targetFor(rule)?.name ?? '对象已删除'}</Typography.Text>
      <Typography.Text type="secondary">{targetFor(rule)?.ownerName ? `${targetFor(rule)?.ownerName} · ` : ''}{rule.targetId}</Typography.Text>
    </Space> },
    { title: '统计模型', dataIndex: 'model', render: (model: string) => <Tag>{model || '全部模型合计'}</Tag> },
    { title: '周期', dataIndex: 'period', width: 80, render: (period: QuotaRule['period']) => PERIOD_LABELS[period] },
    { title: '已用 / 额度（百万 token）', key: 'usage', width: 240, render: (_, rule) => <Space direction="vertical" size={0}>
      <Typography.Text type={rule.limited ? 'danger' : undefined}>{millions(rule.usedMillions)} / {millions(rule.limitMillions)}</Typography.Text>
      <Typography.Text type="secondary">剩余 {millions(rule.remainingMillions)}</Typography.Text>
    </Space> },
    { title: '状态', key: 'status', width: 100, render: (_, rule) => <Tag color={!rule.enabled ? 'default' : rule.limited ? 'red' : 'green'}>{!rule.enabled ? '已停用' : rule.limited ? '额度耗尽' : '生效中'}</Tag> },
    { title: '下次重置', dataIndex: 'resetAt', width: 180, render: (value: string | null) => value ? new Date(value).toLocaleString('zh-CN', { timeZone: zoneId, hour12: false }) : '不自动重置' },
    { title: '操作', key: 'actions', width: 100, fixed: 'right', render: (_, rule) => <Space size={0}>
      <Tooltip title="编辑规则"><Button type="text" icon={<EditOutlined />} aria-label={`编辑规则 ${rule.id}`} onClick={() => { setEditing(rule); setOpen(true); }} /></Tooltip>
      <Tooltip title="删除规则"><Button type="text" danger icon={<DeleteOutlined />} aria-label={`删除规则 ${rule.id}`} disabled={deletion.isPending} onClick={() => modal.confirm({
        title: '删除这条限流规则？', content: '删除后该规则不再限制请求，历史用量保留。其他规则继续生效。', okText: '删除', cancelText: '取消', okButtonProps: { danger: true }, onOk: () => deletion.mutateAsync(rule.id),
      })} /></Tooltip>
    </Space> },
  ];
  if (query.isError) return <PageState status="error" title="限流规则加载失败" description={query.error.message} onRetry={() => { void query.refetch(); }} />;
  return <>
    <Alert type="info" showIcon message="分组额度与 Key 额度同时生效" description={`分组内所有 Key 共享分组额度，任一规则耗尽即返回 429。周期按 ${zoneId} 计算：每天零点、每周一零点、每月 1 日零点重置。用量包含进行中请求的预留额度。`} style={{ marginBottom: 16, background: token.colorBgContainer, borderColor: token.colorBorder }} />
    <Card>
      <Tabs activeKey={scope} onChange={(key) => setScope(key as QuotaScope)} items={[{ key: 'MODEL_GROUP', label: '模型分组限流' }, { key: 'API_KEY', label: '指定 Key 限流' }]} />
      <Space wrap style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Input.Search aria-label="搜索限流规则" placeholder="搜索名称、ID 或模型" value={search} onChange={(event) => setSearch(event.target.value)} allowClear style={{ width: 260, maxWidth: '100%' }} />
        <Space><Button icon={<ReloadOutlined />} aria-label="刷新限流规则" loading={query.isFetching} onClick={() => { void query.refetch(); }} /><Button type="primary" icon={<PlusOutlined />} onClick={() => { setEditing(null); setOpen(true); }}>新增规则</Button></Space>
      </Space>
      <Table rowKey="id" columns={columns} dataSource={filtered} loading={query.isLoading} scroll={{ x: 1180 }} pagination={{ pageSize: 20, hideOnSinglePage: true }} locale={{ emptyText: <Empty description="暂无限流规则，添加规则后开始限制对应额度" /> }} />
    </Card>
    <QuotaFormModal open={open} rule={editing} initialScope={scope} targets={targets} onClose={() => setOpen(false)} />
  </>;
}
