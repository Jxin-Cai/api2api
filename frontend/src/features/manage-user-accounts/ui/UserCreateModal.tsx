import { useEffect, useState } from 'react';
import { Button, Input, Modal, Select, Space, message } from 'antd';
import { UserAccountRow, type UserAccountResponse } from '@entities/user-account';
import { useUserAccountMutations } from '../model/useUserAccountMutations';
import type { UserCreateFormState } from '../model/types';

interface UserCreateModalProps {
  /** 打开状态 */
  open: boolean;
  /** 关闭回调 */
  onClose: () => void;
  /** 创建成功回调 */
  onCreated: (user: UserAccountResponse) => void;
}

export function UserCreateModal({ open, onClose, onCreated }: UserCreateModalProps) {
  const { createMutation } = useUserAccountMutations();
  const [form, setForm] = useState<UserCreateFormState>({ username: '', displayName: '', role: 'USER' });
  const [created, setCreated] = useState<UserAccountResponse | null>(null);

  useEffect(() => {
    if (open) {
      setForm({ username: '', displayName: '', role: 'USER' });
      setCreated(null);
    }
  }, [open]);

  function updateForm<K extends keyof UserCreateFormState>(key: K, value: UserCreateFormState[K]): void {
    setForm((current) => ({ ...current, [key]: value }));
  }

  async function handleSubmit(): Promise<void> {
    if (!form.username.trim() || !form.displayName.trim()) {
      message.warning('请填写用户名和显示名');
      return;
    }
    const response = await createMutation.mutateAsync(form);
    setCreated(response.data);
    onCreated(response.data);
  }

  return (
    <Modal title="创建用户" open={open} onCancel={onClose} footer={null} width={480}>
      <Space direction="vertical" style={{ width: '100%' }}>
        <Input placeholder="username" value={form.username} onChange={(event) => updateForm('username', event.target.value)} />
        <Input placeholder="displayName" value={form.displayName} onChange={(event) => updateForm('displayName', event.target.value)} />
        <Select value={form.role} onChange={(value) => updateForm('role', value)} options={[{ label: 'USER', value: 'USER' }, { label: 'ADMIN', value: 'ADMIN' }]} />
        <Button type="primary" loading={createMutation.isPending} onClick={handleSubmit}>创建</Button>
        {created ? <UserAccountRow user={created} /> : null}
      </Space>
    </Modal>
  );
}
