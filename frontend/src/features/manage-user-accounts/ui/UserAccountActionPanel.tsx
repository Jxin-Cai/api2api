import { useState } from 'react';
import { Button, Card, Input, Space, Typography, message } from 'antd';
import { UserAccountRow, type UserAccountResponse } from '@entities/user-account';
import { ContractNotice } from '@shared/ui';
import { UserCreateModal } from './UserCreateModal';
import { UserEditDrawer } from './UserEditDrawer';

interface UserAccountActionPanelProps {
  /** 创建入口点击回调 */
  onCreateClick?: () => void;
  /** 最近操作结果 */
  lastResult?: UserAccountResponse | null;
}

export function UserAccountActionPanel({ onCreateClick, lastResult = null }: UserAccountActionPanelProps) {
  const [createOpen, setCreateOpen] = useState(false);
  const [targetUserId, setTargetUserId] = useState('');
  const [editOpen, setEditOpen] = useState(false);
  const [recent, setRecent] = useState<UserAccountResponse | null>(lastResult);

  function handleCreateClick(): void {
    onCreateClick?.();
    setCreateOpen(true);
  }

  function handleEditClick(): void {
    if (!targetUserId.trim()) {
      message.warning('请输入用户 ID');
      return;
    }
    setEditOpen(true);
  }

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <ContractNotice title="用户列表契约未开放" description="当前 OHS 仅提供创建、按 ID 修改和启停用户接口，页面不展示假用户列表。" missingApis={['GET /api/admin/users', 'GET /api/admin/users/{user-id}']} />
      <Card title="用户操作">
        <Space wrap>
          <Button type="primary" onClick={handleCreateClick}>创建用户</Button>
          <Input placeholder="输入用户 ID" value={targetUserId} onChange={(event) => setTargetUserId(event.target.value)} style={{ width: 260 }} />
          <Button onClick={handleEditClick}>按 ID 编辑/启停</Button>
        </Space>
      </Card>
      {recent ? <Card title="最近操作结果"><UserAccountRow user={recent} /></Card> : <Typography.Text type="secondary">暂无操作结果</Typography.Text>}
      <UserCreateModal open={createOpen} onClose={() => setCreateOpen(false)} onCreated={(user) => setRecent(user)} />
      <UserEditDrawer open={editOpen} targetUserId={targetUserId} onClose={() => setEditOpen(false)} onUpdated={(user) => setRecent(user)} />
    </Space>
  );
}
