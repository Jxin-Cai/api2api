import { Tag, Tooltip } from 'antd';
import { USER_STATUS_LABELS } from '@shared/config/constants';

interface UserStatusTagProps {
  /** 用户状态 */
  status: string;
}

export function UserStatusTag({ status }: UserStatusTagProps) {
  const color = status === 'ENABLED' ? 'success' : status === 'DISABLED' ? 'default' : 'warning';
  return (
    <Tooltip title={status}>
      <Tag color={color}>{USER_STATUS_LABELS[status] ?? 'UNKNOWN'}</Tag>
    </Tooltip>
  );
}
