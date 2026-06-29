import { Tag, Tooltip } from 'antd';

interface ProviderChannelStatusTagProps {
  /** 渠道状态 */
  status: string;
}

export function ProviderChannelStatusTag({ status }: ProviderChannelStatusTagProps) {
  const enabled = status === 'ENABLED';
  return (
    <Tooltip title={status}>
      <Tag color={enabled ? 'success' : 'default'}>{enabled ? '启用' : '禁用'}</Tag>
    </Tooltip>
  );
}
