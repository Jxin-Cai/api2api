import { Tag, Tooltip } from 'antd';

interface ProviderChannelStatusTagProps {
  /** 渠道状态 */
  status: string;
}

export function ProviderChannelStatusTag({ status }: ProviderChannelStatusTagProps) {
  const meta = status === 'ENABLED'
    ? { color: 'success', label: '启用', description: '渠道可参与路由' }
    : status === 'DEGRADED'
      ? { color: 'warning', label: '限流隔离', description: '上游返回 429，24 小时后自动恢复；也可手动启用' }
      : { color: 'default', label: '禁用', description: '渠道不会参与路由' };
  return (
    <Tooltip title={meta.description}>
      <Tag color={meta.color}>{meta.label}</Tag>
    </Tooltip>
  );
}
