import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Alert, App, Button, Card, Drawer, Empty, Input, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useState } from 'react';
import { useModelGroups } from '@entities/model-group';
import { deleteQuota, QUOTA_QUERY_KEY, useTokenQuotas, type QuotaRule, type QuotaTarget } from '@entities/token-quota';
import { PageState } from '@shared/ui';
import { QuotaFormModal } from './QuotaFormModal';

const millions = (value: number) => Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 6 });

export function AdminKeyQuotaPanel() {
  const { query, rules, targets, zoneId } = useTokenQuotas();
  const { groups } = useModelGroups();
  const { modal, message } = App.useApp();
  const client = useQueryClient();
  const [search, setSearch] = useState('');
  const [keyId, setKeyId] = useState<string | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<QuotaRule | null>(null);
  const selectedKey = targets.find(target => target.scope === 'API_KEY' && target.id === keyId);
  const dailyRules = (id: string) => rules.filter(rule => rule.scope === 'API_KEY' && rule.targetId === id && rule.period === 'DAY');
  const deletion = useMutation({
    mutationFn: deleteQuota,
    onSuccess: async () => {
      await client.invalidateQueries({ queryKey: QUOTA_QUERY_KEY });
      await client.invalidateQueries({ queryKey: ['apiCredentials'] });
      message.success('每日上限已移除');
    },
    onError: (error: Error) => message.error(error.message),
  });
  function remove(rule: QuotaRule) {
    modal.confirm({
      title: `移除 ${rule.model || '全部模型合计'} 的每日上限？`,
      content: '历史用量保留，分组额度和其他周期规则继续生效。',
      okText: '移除', cancelText: '取消', okButtonProps: { danger: true },
      onOk: () => deletion.mutateAsync(rule.id),
    });
  }
  const columns: ColumnsType<QuotaTarget> = [
    { title: 'API Key', key: 'key', render: (_, key) => <Space direction="vertical" size={0}>
      <Typography.Text strong>{key.name}</Typography.Text><Typography.Text type="secondary" copyable>{key.id}</Typography.Text>
    </Space> },
    { title: '所属用户', dataIndex: 'ownerName' },
    { title: '模型分组', key: 'group', render: (_, key) => groups.find(group => group.id === key.modelGroupId)?.name ?? key.modelGroupId },
    { title: '模型每日上限', key: 'limits', render: (_, key) => {
      const active = dailyRules(key.id).filter(rule => rule.enabled);
      return active.length ? <Space wrap>{active.map(rule => <Tag key={rule.id} color={rule.limited ? 'red' : 'blue'}>
        {rule.model || '全部合计'} · {millions(rule.limitMillions)} 百万{rule.limited ? ' · 已耗尽' : ''}
      </Tag>)}</Space> : <Typography.Text type="secondary">未设置 Key 每日上限</Typography.Text>;
    } },
    { title: '操作', key: 'actions', width: 150, render: (_, key) => <Button onClick={() => setKeyId(key.id)}>配置每日上限</Button> },
  ];
  const quotaColumns: ColumnsType<QuotaRule> = [
    { title: '模型', dataIndex: 'model', render: model => model || '全部模型合计' },
    { title: '已用 / 上限（百万）', key: 'usage', render: (_, rule) => <Space direction="vertical" size={0}>
      <Typography.Text type={rule.limited ? 'danger' : undefined}>{millions(rule.usedMillions)} / {millions(rule.limitMillions)}</Typography.Text>
      <Typography.Text type="secondary">剩余 {millions(rule.remainingMillions)}</Typography.Text>
    </Space> },
    { title: '状态', key: 'status', render: (_, rule) => <Tag color={!rule.enabled ? 'default' : rule.limited ? 'red' : 'green'}>{!rule.enabled ? '已停用' : rule.limited ? '已耗尽' : '生效中'}</Tag> },
    { title: '操作', key: 'actions', render: (_, rule) => <Space>
      <Button size="small" onClick={() => { setEditing(rule); setFormOpen(true); }}>编辑</Button>
      <Button size="small" danger disabled={deletion.isPending} onClick={() => remove(rule)}>移除</Button>
    </Space> },
  ];
  if (query.isError) return <PageState status="error" title="用户 Key 加载失败" description={query.error.message} onRetry={() => void query.refetch()} />;
  return <>
    <Alert type="info" showIcon message="每个 Key、每个模型独立设置每日上限" description={`按 ${zoneId} 每日零点重置。分组共享额度、Key 的每日及其他周期额度同时生效；用量包含进行中请求的预留额度。`} style={{ marginBottom: 16 }} />
    <Card>
      <Space wrap style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Input.Search aria-label="搜索用户 API Key" placeholder="搜索用户、Key 名称或 ID" allowClear value={search} onChange={event => setSearch(event.target.value)} style={{ width: 300, maxWidth: '100%' }} />
        <Button icon={<ReloadOutlined />} aria-label="刷新用户 API Key" loading={query.isFetching} onClick={() => void query.refetch()} />
      </Space>
      <Table rowKey="id" columns={columns} loading={query.isLoading} scroll={{ x: 900 }} pagination={{ pageSize: 20, hideOnSinglePage: true }}
        dataSource={targets.filter(target => target.scope === 'API_KEY' && `${target.name} ${target.ownerName} ${target.id}`.toLowerCase().includes(search.trim().toLowerCase()))}
        locale={{ emptyText: <Empty description="没有符合条件的 API Key" /> }} />
    </Card>
    <Drawer title={selectedKey ? `${selectedKey.name} · 模型每日上限` : '模型每日上限'} width={760} open={Boolean(selectedKey)} onClose={() => { setKeyId(null); setFormOpen(false); }} destroyOnHidden>
      {selectedKey ? <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Typography.Text type="secondary">{selectedKey.ownerName} · {selectedKey.id}</Typography.Text>
        <Alert type="info" showIcon message={`每日零点（${zoneId}）重置，额度单位为百万加权 Token。`} description="未设置上限的模型仍受分组和其他限流规则约束。" />
        <Button type="primary" icon={<PlusOutlined />} onClick={() => { setEditing(null); setFormOpen(true); }}>新增模型每日上限</Button>
        <Table rowKey="id" columns={quotaColumns} dataSource={dailyRules(selectedKey.id)} pagination={false} scroll={{ x: 620 }} locale={{ emptyText: <Empty description="此 Key 尚未设置模型每日上限" /> }} />
      </Space> : null}
    </Drawer>
    {selectedKey ? <QuotaFormModal open={formOpen} rule={editing} initialScope="API_KEY" targets={targets} fixedKey={selectedKey} onClose={() => setFormOpen(false)} /> : null}
  </>;
}
