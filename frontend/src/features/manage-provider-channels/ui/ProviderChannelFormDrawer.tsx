import { useEffect, useState } from 'react';
import { Button, Drawer, Input, Select, Space, message } from 'antd';
import type { ProviderChannelResponse } from '@entities/provider-channel';
import { PROTOCOL_OPTIONS } from '@shared/lib/protocols';
import type { AdminFormMode } from '@shared/types/admin';
import { useProviderChannelMutations } from '../model/useProviderChannelMutations';
import type { ProviderChannelFormState } from '../model/types';

interface ProviderChannelFormDrawerProps {
  /** 打开状态 */
  open: boolean;
  /** 表单模式 */
  mode: AdminFormMode;
  /** 编辑渠道 */
  channel?: ProviderChannelResponse | null;
  /** 关闭回调 */
  onClose: () => void;
  /** 保存成功回调 */
  onSaved: (channel: ProviderChannelResponse) => void;
}

export function ProviderChannelFormDrawer({ open, mode, channel = null, onClose, onSaved }: ProviderChannelFormDrawerProps) {
  const { createMutation, updateMutation } = useProviderChannelMutations();
  const [form, setForm] = useState<ProviderChannelFormState>({ name: '', host: '', keyRef: '', supportedProtocols: [] });

  useEffect(() => {
    if (!open) {
      return;
    }
    if (channel && mode === 'edit') {
      setForm({ name: channel.name, host: channel.host, keyRef: channel.keyRef, supportedProtocols: channel.supportedProtocols });
      return;
    }
    setForm({ name: '', host: '', keyRef: '', supportedProtocols: [] });
  }, [channel, mode, open]);

  function updateForm<K extends keyof ProviderChannelFormState>(key: K, value: ProviderChannelFormState[K]): void {
    setForm((current) => ({ ...current, [key]: value }));
  }

  async function handleSave(): Promise<void> {
    if (!form.name.trim() || !form.host.trim() || !form.keyRef.trim() || form.supportedProtocols.length === 0) {
      message.warning('请完整填写渠道信息，协议至少选择一个');
      return;
    }
    const response = mode === 'create' ? await createMutation.mutateAsync(form) : await updateMutation.mutateAsync({ id: channel?.id ?? 0, body: form });
    onSaved(response.data);
    onClose();
  }

  return (
    <Drawer title={mode === 'create' ? '新建渠道' : '编辑渠道'} open={open} onClose={onClose} width={560}>
      <Space direction="vertical" style={{ width: '100%' }}>
        <Input placeholder="渠道名称" value={form.name} onChange={(event) => updateForm('name', event.target.value)} />
        <Input placeholder="Provider Host" value={form.host} onChange={(event) => updateForm('host', event.target.value)} />
        <Input.Password placeholder="Key 引用名，不填写真实 key" value={form.keyRef} onChange={(event) => updateForm('keyRef', event.target.value)} />
        <Select mode="multiple" placeholder="支持协议" value={form.supportedProtocols} onChange={(value) => updateForm('supportedProtocols', value)} options={PROTOCOL_OPTIONS} />
        <Button type="primary" loading={createMutation.isPending || updateMutation.isPending} onClick={handleSave}>保存</Button>
      </Space>
    </Drawer>
  );
}
