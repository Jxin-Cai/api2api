import { Alert, Button, Form, Input, Modal, Select, Space } from 'antd';
import { useEffect, useState } from 'react';

import type { ModelGroupResponse, SaveModelGroupRequest } from '@entities/model-group';

import { useModelGroupMutations } from '../model/useModelGroupMutations';

interface ModelGroupFormModalProps {
  open: boolean;
  group: ModelGroupResponse | null;
  modelOptions: Array<{ label: string; value: string }>;
  onClose: () => void;
  onSaved: (group: ModelGroupResponse) => void;
}

const EMPTY_FORM: SaveModelGroupRequest = { name: '', modelWhitelist: [] };

export function ModelGroupFormModal({ open, group, modelOptions, onClose, onSaved }: ModelGroupFormModalProps) {
  const { createMutation, updateMutation } = useModelGroupMutations();
  const [form, setForm] = useState<SaveModelGroupRequest>(EMPTY_FORM);

  useEffect((): void => {
    if (open) {
      setForm(group ? { name: group.name, modelWhitelist: group.modelWhitelist } : EMPTY_FORM);
    }
  }, [group, open]);

  const mutation = group ? updateMutation : createMutation;

  async function handleSubmit(): Promise<void> {
    const params = { ...form, name: form.name.trim() };
    if (!params.name) {
      return;
    }
    const saved = group
      ? await updateMutation.mutateAsync({ id: group.id, params })
      : await createMutation.mutateAsync(params);
    onSaved(saved);
    onClose();
  }

  return (
    <Modal title={group ? '编辑模型分组' : '创建模型分组'} open={open} onCancel={onClose} footer={null} width={600} destroyOnHidden>
      <Form layout="vertical" onFinish={handleSubmit}>
        <Form.Item label="分组名称" required validateStatus={!form.name.trim() ? 'error' : undefined} help={!form.name.trim() ? '请输入分组名称' : undefined}>
          <Input autoFocus value={form.name} maxLength={100} onChange={(event): void => setForm((current) => ({ ...current, name: event.target.value }))} placeholder="例如：生产环境标准模型" disabled={mutation.isPending} />
        </Form.Item>
        <Form.Item label="允许的大模型" extra="留空表示该分组禁止调用所有模型。修改后会立即作用于该分组下的全部 Key。">
          <Select
            mode="tags"
            value={form.modelWhitelist}
            onChange={(modelWhitelist: string[]): void => setForm((current) => ({ ...current, modelWhitelist }))}
            options={modelOptions}
            placeholder="选择或输入允许的模型名"
            disabled={mutation.isPending}
          />
          {modelOptions.length === 0 ? <Alert type="info" showIcon message="暂无已配置模型，也可以手动输入模型名。" style={{ marginTop: 8 }} /> : null}
        </Form.Item>
        {mutation.isError ? <Alert type="error" showIcon message="保存失败" description={mutation.error.message} style={{ marginBottom: 16 }} /> : null}
        <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
          <Button onClick={onClose} disabled={mutation.isPending}>取消</Button>
          <Button type="primary" htmlType="submit" loading={mutation.isPending} disabled={!form.name.trim()}>保存分组</Button>
        </Space>
      </Form>
    </Modal>
  );
}
