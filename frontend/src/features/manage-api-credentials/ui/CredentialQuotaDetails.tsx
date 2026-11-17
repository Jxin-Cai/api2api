import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Alert, App, Button, Empty, Space, Table, Tag, Typography } from 'antd';
import { deleteCredentialQuota, QUOTA_QUERY_KEY, useCredentialQuotas, type QuotaRule } from '@entities/token-quota';
import { CredentialQuotaFormModal } from './CredentialQuotaFormModal';

const periods = { DAY: '每日', WEEK: '每周', MONTH: '每月', LIFETIME: '累计' };
const millions = (value: number) => Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 6 });

export function CredentialQuotaDetails({ keyId, models }: { keyId: string; models: string[] }) {
  const query = useCredentialQuotas(keyId);
  const client = useQueryClient();
  const { modal, message } = App.useApp();
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<QuotaRule | null>(null);
  const deletion = useMutation({
    mutationFn: (ruleId: string) => deleteCredentialQuota(keyId, ruleId),
    onSuccess: async () => {
      await client.invalidateQueries({ queryKey: QUOTA_QUERY_KEY });
      await client.invalidateQueries({ queryKey: ['apiCredentials'] });
      message.success('自设每日上限已移除');
    },
    onError: (error: Error) => message.error(error.message),
  });
  function remove(rule: QuotaRule) {
    modal.confirm({
      title: `移除 ${rule.model} 的自设每日上限？`,
      content: '管理员和分组额度继续生效，历史用量保留。',
      okText: '移除', cancelText: '取消', okButtonProps: { danger: true },
      onOk: () => deletion.mutateAsync(rule.id),
    });
  }
  if (query.isError) return <Alert type="error" showIcon message="额度加载失败" description={query.error.message} />;
  const zoneId = query.data?.zoneId ?? 'Asia/Shanghai';
  return <Space direction="vertical" size={12} style={{ width: '100%' }}>
    <Alert type="info" showIcon message="可为自己的 Key 设置各模型每日上限" description={`自设上限、管理员额度和分组共享额度同时生效，任一耗尽即限制请求。自设上限按 ${zoneId} 每日零点重置；用量包含进行中请求的预留额度。`} />
    <Button type="primary" disabled={query.isLoading || models.length === 0} onClick={() => { setEditing(null); setOpen(true); }}>新增自设每日上限</Button>
    <Table<QuotaRule> rowKey="id" size="small" loading={query.isLoading} dataSource={query.data?.rules ?? []} pagination={false}
      locale={{ emptyText: <Empty description="暂未配置 Token 额度限制" /> }}
      columns={[
        { title: '模型 / 周期', key: 'model', render: (_, rule) => <Space direction="vertical" size={2}>
          <Typography.Text>{rule.model || '全部模型合计'}</Typography.Text>
          <Tag>{rule.source === 'USER' ? '用户自设' : rule.scope === 'API_KEY' ? '管理员设置' : '分组共享'} · {periods[rule.period]}</Tag>
          {!rule.enabled ? <Tag>已停用</Tag> : null}
        </Space> },
        { title: '已用 / 上限（百万）', key: 'usage', render: (_, rule) => <Space direction="vertical" size={2}>
          <Typography.Text type={rule.limited ? 'danger' : undefined}>{millions(rule.usedMillions)} / {millions(rule.limitMillions)}</Typography.Text>
          <Typography.Text type="secondary">{rule.resetAt ? `${new Date(rule.resetAt).toLocaleString('zh-CN', { timeZone: zoneId, hour12: false })} 重置` : '不自动重置'}</Typography.Text>
        </Space> },
        { title: '操作', key: 'actions', width: 112, render: (_, rule) => rule.source === 'USER' ? <Space size={0}>
          <Button type="text" size="small" onClick={() => { setEditing(rule); setOpen(true); }}>编辑</Button>
          <Button type="text" size="small" danger disabled={deletion.isPending} onClick={() => remove(rule)}>移除</Button>
        </Space> : <Typography.Text type="secondary">只读</Typography.Text> },
      ]} />
    <CredentialQuotaFormModal open={open} keyId={keyId} models={models} rule={editing} onClose={() => setOpen(false)} />
  </Space>;
}
