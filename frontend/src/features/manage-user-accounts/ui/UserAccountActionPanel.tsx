import { useState } from 'react';
import { Button, Card, Input, Space, Table, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { UserAccountRow, UserRoleTag, UserStatusTag, useUserAccounts, type UserAccountResponse } from '@entities/user-account';
import { UserCreateModal } from './UserCreateModal';
import { UserEditDrawer } from './UserEditDrawer';

interface UserAccountActionPanelProps {
  /** 创建入口点击回调 */
  onCreateClick?: () => void;
  /** 最近操作结果 */
  lastResult?: UserAccountResponse | null;
}

export function UserAccountActionPanel({ onCreateClick, lastResult = null }: UserAccountActionPanelProps) {
  const { users, isLoading, refetch } = useUserAccounts();
  const [createOpen, setCreateOpen] = useState(false);
  const [targetUserId, setTargetUserId] = useState('');
  const [editOpen, setEditOpen] = useState(false);
  const [recent, setRecent] = useState<UserAccountResponse | null>(lastResult);

  function handleCreateClick(): void {
    onCreateClick?.();
    setCreateOpen(true);
  }

  function handleEditClick(userId?: string | number): void {
    const nextUserId = userId === undefined ? targetUserId.trim() : String(userId);
    if (!nextUserId) {
      message.warning('请输入用户 ID');
      return;
    }
    setTargetUserId(nextUserId);
    setEditOpen(true);
  }

  function handleUserChanged(user: UserAccountResponse): void {
    setRecent(user);
    void refetch();
  }

  const columns: ColumnsType<UserAccountResponse> = [{
    title: '用户',
    dataIndex: 'displayName',
    render: (_value, user) => (
      <Space direction="vertical" size={2}>
        <Space wrap>
          <Typography.Text strong>{user.displayName}</Typography.Text>
          <Typography.Text type="secondary">@{user.username}</Typography.Text>
        </Space>
        <Typography.Text type="secondary">ID：{String(user.id)}</Typography.Text>
      </Space>
    ),
  }, {
    title: '角色',
    dataIndex: 'role',
    width: 120,
    render: (_value, user) => <UserRoleTag role={user.role} />,
  }, {
    title: '状态',
    dataIndex: 'status',
    width: 120,
    render: (_value, user) => <UserStatusTag status={user.status} />,
  }, {
    title: '更新时间',
    dataIndex: 'updatedAt',
    width: 220,
    render: (value) => value ?? '-',
  }, {
    title: '操作',
    key: 'actions',
    width: 120,
    render: (_value, user) => <Button size="small" onClick={() => handleEditClick(user.id)}>编辑/启停</Button>,
  }];

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Card title="用户操作">
        <Space wrap>
          <Button type="primary" onClick={handleCreateClick}>创建用户</Button>
          <Input placeholder="输入用户 ID" value={targetUserId} onChange={(event) => setTargetUserId(event.target.value)} style={{ width: 260 }} />
          <Button onClick={() => handleEditClick()}>按 ID 编辑/启停</Button>
          <Button onClick={() => void refetch()} loading={isLoading}>刷新列表</Button>
        </Space>
      </Card>
      <Card title="用户列表">
        <Table rowKey="id" columns={columns} dataSource={users} loading={isLoading} pagination={{ pageSize: 10 }} />
      </Card>
      {recent ? <Card title="最近操作结果"><UserAccountRow user={recent} /></Card> : <Typography.Text type="secondary">暂无操作结果</Typography.Text>}
      <UserCreateModal open={createOpen} onClose={() => setCreateOpen(false)} onCreated={handleUserChanged} />
      <UserEditDrawer open={editOpen} targetUserId={targetUserId} onClose={() => setEditOpen(false)} onUpdated={handleUserChanged} />
    </Space>
  );
}
