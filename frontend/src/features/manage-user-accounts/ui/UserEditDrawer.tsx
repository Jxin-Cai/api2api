import { useState } from 'react';
import { Button, Drawer, Input, Popconfirm, Select, Space, Typography, message } from 'antd';
import type { UserAccountResponse } from '@entities/user-account';
import type { AdminRole } from '@shared/types/admin';
import { useUserAccountMutations } from '../model/useUserAccountMutations';

interface UserEditDrawerProps {
  /** 打开状态 */
  open: boolean;
  /** 目标用户 ID */
  targetUserId: string;
  /** 关闭回调 */
  onClose: () => void;
  /** 更新成功回调 */
  onUpdated: (user: UserAccountResponse) => void;
}

export function UserEditDrawer({ open, targetUserId, onClose, onUpdated }: UserEditDrawerProps) {
  const { displayNameMutation, roleMutation, enableMutation, disableMutation } = useUserAccountMutations();
  const [displayNameDraft, setDisplayNameDraft] = useState('');
  const [roleDraft, setRoleDraft] = useState<AdminRole>('USER');

  async function handleDisplayNameSave(): Promise<void> {
    if (!displayNameDraft.trim()) {
      message.warning('请输入显示名');
      return;
    }
    const response = await displayNameMutation.mutateAsync({ userId: targetUserId, displayName: displayNameDraft });
    onUpdated(response.data);
  }

  async function handleRoleSave(): Promise<void> {
    const response = await roleMutation.mutateAsync({ userId: targetUserId, role: roleDraft });
    onUpdated(response.data);
  }

  async function handleStatusChange(enabled: boolean): Promise<void> {
    const response = enabled ? await enableMutation.mutateAsync(targetUserId) : await disableMutation.mutateAsync(targetUserId);
    onUpdated(response.data);
  }

  return (
    <Drawer title="按 ID 编辑用户" open={open} onClose={onClose} width={480}>
      <Space direction="vertical" style={{ width: '100%' }}>
        <Typography.Text type="secondary">目标用户 ID：{targetUserId || '-'}</Typography.Text>
        <Input placeholder="新的显示名" value={displayNameDraft} onChange={(event) => setDisplayNameDraft(event.target.value)} />
        <Button loading={displayNameMutation.isPending} onClick={handleDisplayNameSave}>保存显示名</Button>
        <Select value={roleDraft} onChange={setRoleDraft} options={[{ label: 'USER', value: 'USER' }, { label: 'ADMIN', value: 'ADMIN' }]} />
        <Button loading={roleMutation.isPending} onClick={handleRoleSave}>保存角色</Button>
        <Space>
          <Popconfirm title="确认启用该用户？" onConfirm={() => handleStatusChange(true)}>
            <Button loading={enableMutation.isPending}>启用</Button>
          </Popconfirm>
          <Popconfirm title="确认禁用该用户？" onConfirm={() => handleStatusChange(false)}>
            <Button danger loading={disableMutation.isPending}>禁用</Button>
          </Popconfirm>
        </Space>
      </Space>
    </Drawer>
  );
}
