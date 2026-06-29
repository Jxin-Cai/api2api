import { Avatar, Space, Tooltip, Typography } from 'antd';
import { UserOutlined } from '@ant-design/icons';
import { formatDateTime, formatId } from '@shared/lib/formatters';
import type { UserAccountResponse } from '../model/types';
import { UserRoleTag } from './UserRoleTag';
import { UserStatusTag } from './UserStatusTag';

interface UserAccountBadgeProps {
  /** 当前用户，空值表示未登录 */
  user?: UserAccountResponse | null;
}

export function UserAccountBadge({ user }: UserAccountBadgeProps) {
  if (!user) {
    return (
      <Space>
        <Avatar icon={<UserOutlined />} />
        <Typography.Text type="secondary">未登录</Typography.Text>
      </Space>
    );
  }

  const title = (
    <Space direction="vertical" size={2}>
      <span>ID：{formatId(user.id)}</span>
      <span>创建：{formatDateTime(user.createdAt)}</span>
      <span>更新：{formatDateTime(user.updatedAt)}</span>
    </Space>
  );

  return (
    <Tooltip title={title}>
      <Space size={8}>
        <Avatar icon={<UserOutlined />}>{user.displayName?.slice(0, 1)}</Avatar>
        <Space direction="vertical" size={0}>
          <Typography.Text strong>{user.displayName || user.username}</Typography.Text>
          <Typography.Text type="secondary">@{user.username}</Typography.Text>
        </Space>
        <UserRoleTag role={user.role} />
        <UserStatusTag status={user.status} />
      </Space>
    </Tooltip>
  );
}
