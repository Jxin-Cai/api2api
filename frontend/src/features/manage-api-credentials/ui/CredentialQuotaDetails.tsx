import { Alert, Empty, Space, Table, Tag, Typography } from 'antd';
import { useCredentialQuotas, type QuotaRule } from '@entities/token-quota';

const periods = { DAY: '每日', WEEK: '每周', MONTH: '每月', LIFETIME: '累计' };
const millions = (value: number) => Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 6 });

export function CredentialQuotaDetails({ keyId }: { keyId: string }) {
  const query = useCredentialQuotas(keyId);
  if (query.isError) return <Alert type="error" showIcon message="额度加载失败" description={query.error.message} />;
  const zoneId = query.data?.zoneId ?? 'Asia/Shanghai';
  return <Space direction="vertical" size={12} style={{ width: '100%' }}>
    <Alert type="info" showIcon message="额度由管理员配置" description={`分组共享额度与 Key 专属额度同时生效。自然周期按 ${zoneId} 重置；用量包含进行中请求的预留额度。`} />
    <Table<QuotaRule> rowKey="id" size="small" loading={query.isLoading} dataSource={query.data?.rules ?? []} pagination={false}
      locale={{ emptyText: <Empty description="暂未配置 Token 额度限制" /> }}
      columns={[
        { title: '模型 / 周期', key: 'model', render: (_, rule) => <Space direction="vertical" size={2}>
          <Typography.Text>{rule.model || '全部模型合计'}</Typography.Text>
          <Tag>{rule.scope === 'API_KEY' ? '此 Key' : '分组共享'} · {periods[rule.period]}</Tag>
          {!rule.enabled ? <Tag>已停用</Tag> : null}
        </Space> },
        { title: '已用 / 上限（百万）', key: 'usage', render: (_, rule) => <Space direction="vertical" size={2}>
          <Typography.Text type={rule.limited ? 'danger' : undefined}>{millions(rule.usedMillions)} / {millions(rule.limitMillions)}</Typography.Text>
          <Typography.Text type="secondary">{rule.resetAt ? `${new Date(rule.resetAt).toLocaleString('zh-CN', { timeZone: zoneId, hour12: false })} 重置` : '不自动重置'}</Typography.Text>
        </Space> },
      ]} />
  </Space>;
}
