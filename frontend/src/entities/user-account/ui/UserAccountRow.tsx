import type { ReactNode } from 'react';
import { Card, Descriptions, Space, Typography } from 'antd';
import type { UserAccountResponse } from '../model/types';
import { UserRoleTag } from './UserRoleTag';
import { UserStatusTag } from './UserStatusTag';

interface UserAccountRowProps {
  /** 用户账户 */
  user: UserAccountResponse;
  /** 操作区 */
  actions?: ReactNode;
}

export function UserAccountRow({ user, actions }: UserAccountRowProps) {
  return (
    <Card size="small">
      <Space direction="vertical" style={{ width: '100%' }}>
        <Space wrap>
          <Typography.Text strong>{user.displayName}</Typography.Text>
          <Typography.Text type="secondary">@{user.username}</Typography.Text>
          <UserRoleTag role={user.role} />
          <UserStatusTag status={user.status} />
        </Space>
        <Descriptions size="small" column={2}>
          <Descriptions.Item label="ID">{String(user.id)}</Descriptions.Item>
          <Descriptions.Item label="更新时间">{user.updatedAt ?? '-'}</Descriptions.Item>
        </Descriptions>
        {actions}
      </Space>
    </Card>
  );
}
