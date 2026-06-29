import { Tag, Tooltip } from 'antd';

interface UsageRecordStatusTagProps {
  /** 调用状态原始值 */
  status: string;
}

function statusMeta(status: string): { color: string; label: string } {
  const normalized = status.toUpperCase();
  if (normalized === 'SUCCESS' || normalized === 'SUCCEEDED') {
    return { color: 'success', label: '成功' };
  }
  if (normalized === 'FAILED' || normalized === 'ERROR') {
    return { color: 'error', label: '失败' };
  }
  if (normalized === 'PROCESSING' || normalized === 'PENDING') {
    return { color: 'processing', label: '处理中' };
  }
  return { color: 'default', label: '未知' };
}

export function UsageRecordStatusTag({ status }: UsageRecordStatusTagProps) {
  const meta = statusMeta(status || 'UNKNOWN');
  return (
    <Tooltip title={`原始状态：${status || 'UNKNOWN'}`}>
      <Tag color={meta.color}>{meta.label}</Tag>
    </Tooltip>
  );
}
