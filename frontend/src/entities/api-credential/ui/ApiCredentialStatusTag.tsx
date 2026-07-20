import { Tag, Tooltip } from 'antd';

interface ApiCredentialStatusTagProps {
  /** 后端 API Key 状态原始值 */
  status: string;
  /** 是否显示状态圆点 */
  showDot?: boolean;
}

function getStatusMeta(status: string): { color: string; label: string } {
  const normalized = status.toUpperCase();
  if (normalized === 'ACTIVE' || normalized === 'ENABLED') {
    return { color: 'success', label: '已激活' };
  }
  if (normalized === 'DISABLED') {
    return { color: 'default', label: '已禁用' };
  }
  return { color: 'default', label: '未知' };
}

export function ApiCredentialStatusTag({ status, showDot = true }: ApiCredentialStatusTagProps) {
  const meta = getStatusMeta(status || 'UNKNOWN');
  return (
    <Tooltip title={`原始状态：${status || 'UNKNOWN'}`}>
      <Tag color={meta.color}>{showDot ? '● ' : ''}{meta.label}</Tag>
    </Tooltip>
  );
}
