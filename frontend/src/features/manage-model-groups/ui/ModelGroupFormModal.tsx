import { Alert, Button, Form, Input, Modal, Select, Space } from 'antd';
import { useEffect, useState } from 'react';
import type { ModelGroupResponse, SaveModelGroupRequest } from '@entities/model-group';
import { useModelGroupMutations } from '../model/useModelGroupMutations';

interface Props {
  open: boolean;
  group: ModelGroupResponse | null;
  modelOptions: Array<{ label: string; value: string }>;
  onClose: () => void;
  onSaved: (group: ModelGroupResponse) => void;
}
const EMPTY: SaveModelGroupRequest = { name: '', modelWhitelist: [] };

export function ModelGroupFormModal({ open, group, modelOptions, onClose, onSaved }: Props) {
  const { createMutation, updateMutation } = useModelGroupMutations();
  const [form, setForm] = useState<SaveModelGroupRequest>(EMPTY);
  useEffect(() => {
    if (open) setForm(group ? { name: group.name, modelWhitelist: group.modelWhitelist } : EMPTY);
  }, [group, open]);
  const mutation = group ? updateMutation : createMutation;
  async function submit(): Promise<void> {
    const params = { ...form, name: form.name.trim() };
    if (!params.name) return;
    const saved = group ? await updateMutation.mutateAsync({ id: group.id, params }) : await createMutation.mutateAsync(params);
    onSaved(saved);
    onClose();
  }
  return (
    <Modal title={group ? '编辑模型分组' : '创建模型分组'} open={open} onCancel={onClose} footer={null} width={600} destroyOnHidden>
      <Form layout="vertical" onFinish={submit}>
        <Form.Item label="分组名称" htmlFor="model-group-name" required>
          <Input id="model-group-name" autoFocus value={form.name} maxLength={100} onChange={(event) => setForm({ ...form, name: event.target.value })} placeholder="例如：标准模型" disabled={mutation.isPending} />
        </Form.Item>
        <Form.Item label="允许的模型" htmlFor="model-group-models" extra="留空表示禁止调用所有模型。渠道暂不可用时，保留分组配置。">
          <Select id="model-group-models" mode="tags" value={form.modelWhitelist} options={modelOptions} onChange={(modelWhitelist: string[]) => setForm({ ...form, modelWhitelist })} placeholder="选择或输入模型名" disabled={mutation.isPending} />
        </Form.Item>
        <Alert type="info" showIcon message="额度请在后台“限流”页配置。分组内所有 Key 共享分组额度。" style={{ marginBottom: 16 }} />
        {mutation.isError ? <Alert type="error" showIcon message="保存失败" description={mutation.error.message} style={{ marginBottom: 16 }} /> : null}
        <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
          <Button onClick={onClose} disabled={mutation.isPending}>取消</Button>
          <Button type="primary" htmlType="submit" loading={mutation.isPending} disabled={!form.name.trim()}>保存分组</Button>
        </Space>
      </Form>
    </Modal>
  );
}
