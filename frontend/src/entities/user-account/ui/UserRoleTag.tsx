import { Tag } from 'antd';
import type { AdminRole } from '@shared/types/admin';
import { USER_ROLE_LABELS } from '@shared/config/constants';

interface UserRoleTagProps {
  /** 用户角色 */
  role: AdminRole | string;
}

export function UserRoleTag({ role }: UserRoleTagProps) {
  const color = role === 'ADMIN' ? 'gold' : role === 'USER' ? 'blue' : 'default';
  return <Tag color={color}>{USER_ROLE_LABELS[role] ?? 'UNKNOWN'}</Tag>;
}
