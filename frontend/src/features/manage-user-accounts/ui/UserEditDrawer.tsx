import { useEffect, useState } from 'react';
import { Alert, Button, Drawer, Input, Popconfirm, Select, Space, Spin, Typography, message } from 'antd';
import { UserAccountRow, useUserAccountDetail, type UserAccountResponse } from '@entities/user-account';
import type { ApiErrorShape } from '@shared/api';
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

function getErrorMessage(error: unknown): string {
  if (typeof error === 'object' && error !== null && 'message' in error) {
    return (error as ApiErrorShape).message || '操作失败';
  }
  return '操作失败';
}

export function UserEditDrawer({ open, targetUserId, onClose, onUpdated }: UserEditDrawerProps) {
  const { displayNameMutation, roleMutation, enableMutation, disableMutation } = useUserAccountMutations();
  const { user, isLoading, error } = useUserAccountDetail(targetUserId, open);
  const [displayNameDraft, setDisplayNameDraft] = useState('');
  const [roleDraft, setRoleDraft] = useState<AdminRole>('USER');

  useEffect(() => {
    if (!user) {
      return;
    }
    setDisplayNameDraft(user.displayName);
    setRoleDraft(user.role);
  }, [user]);

  async function handleDisplayNameSave(): Promise<void> {
    if (!displayNameDraft.trim()) {
      message.warning('请输入显示名');
      return;
    }
    try {
      const response = await displayNameMutation.mutateAsync({ userId: targetUserId, displayName: displayNameDraft.trim() });
      onUpdated(response.data);
      message.success('显示名已更新');
    } catch (mutationError) {
      message.error(`保存显示名失败：${getErrorMessage(mutationError)}`);
    }
  }

  async function handleRoleSave(): Promise<void> {
    try {
      const response = await roleMutation.mutateAsync({ userId: targetUserId, role: roleDraft });
      onUpdated(response.data);
      message.success('角色已更新');
    } catch (mutationError) {
      message.error(`保存角色失败：${getErrorMessage(mutationError)}`);
    }
  }

  async function handleStatusChange(enabled: boolean): Promise<void> {
    try {
      const response = enabled ? await enableMutation.mutateAsync(targetUserId) : await disableMutation.mutateAsync(targetUserId);
      onUpdated(response.data);
      message.success(enabled ? '用户已启用' : '用户已禁用');
    } catch (mutationError) {
      message.error(`更新状态失败：${getErrorMessage(mutationError)}`);
    }
  }

  return (
    <Drawer title="编辑用户" open={open} onClose={onClose} width={480}>
      <Space direction="vertical" style={{ width: '100%' }}>
        <Typography.Text type="secondary">目标用户 ID：{targetUserId || '-'}</Typography.Text>
        {isLoading ? <Spin /> : null}
        {error ? <Alert type="error" showIcon message={`加载用户详情失败：${getErrorMessage(error)}`} /> : null}
        {user ? <UserAccountRow user={user} /> : null}
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
