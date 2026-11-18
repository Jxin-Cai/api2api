import { Alert, Button, Form, Input, Modal, Select, Space } from 'antd';
import { useEffect, useState } from 'react';
import type { ModelGroupResponse, SaveModelGroupRequest } from '@entities/model-group';
import { useProviderChannels } from '@entities/provider-channel';
import { useModelGroupMutations } from '../model/useModelGroupMutations';
import type { ModelGroupFormState } from '../model/modelGroupForm';

interface Props {
  state: ModelGroupFormState | null;
  modelOptions: Array<{ label: string; value: string }>;
  onClose: () => void;
  onSaved: (group: ModelGroupResponse) => void;
}
const EMPTY: SaveModelGroupRequest = { name: '', modelWhitelist: [], allowedChannelIds: [] };

export function ModelGroupFormModal({ state, modelOptions, onClose, onSaved }: Props) {
  const { createMutation, updateMutation } = useModelGroupMutations();
  const open = state !== null;
  const channels = useProviderChannels({ enabled: open });
  const [form, setForm] = useState<SaveModelGroupRequest>(EMPTY);
  const { reset: resetCreate } = createMutation;
  const { reset: resetUpdate } = updateMutation;
  useEffect(() => {
    if (!state) return;
    resetCreate();
    resetUpdate();
    setForm(state.mode === 'create' ? EMPTY : {
      name: state.mode === 'copy' ? state.name : state.group.name,
      modelWhitelist: [...state.group.modelWhitelist],
      allowedChannelIds: [...state.group.allowedChannelIds],
    });
  }, [state, resetCreate, resetUpdate]);
  const mutation = state?.mode === 'edit' ? updateMutation : createMutation;
  async function submit(): Promise<void> {
    if (!state || mutation.isPending) return;
    const params = { ...form, name: form.name.trim() };
    if (!params.name) return;
    try {
      const saved = state.mode === 'edit'
        ? await updateMutation.mutateAsync({ id: state.group.id, params })
        : await createMutation.mutateAsync(params);
      onSaved(saved);
      onClose();
    } catch {
      // The mutation error is displayed below; keep the form open for correction.
    }
  }
  return (
    <Modal title={state?.mode === 'edit' ? '编辑模型分组' : state?.mode === 'copy' ? '复制模型分组' : '创建模型分组'} open={open} onCancel={() => { if (!mutation.isPending) onClose(); }} closable={!mutation.isPending} maskClosable={!mutation.isPending} keyboard={!mutation.isPending} footer={null} width={600} destroyOnHidden>
      <Form layout="vertical" onFinish={submit}>
        {state?.mode === 'copy' ? <Alert type="info" showIcon message={`已复制“${state.group.name}”的模型和渠道配置，可修改后保存为新分组。`} style={{ marginBottom: 16 }} /> : null}
        <Form.Item label="分组名称" htmlFor="model-group-name" required>
          <Input id="model-group-name" autoFocus value={form.name} maxLength={100} onChange={(event) => setForm({ ...form, name: event.target.value })} placeholder="例如：标准模型" disabled={mutation.isPending} />
        </Form.Item>
        <Form.Item label="允许的模型" htmlFor="model-group-models" extra="留空表示禁止调用所有模型。渠道暂不可用时，保留分组配置。">
          <Select id="model-group-models" mode="tags" value={form.modelWhitelist} options={modelOptions} onChange={(modelWhitelist: string[]) => setForm({ ...form, modelWhitelist })} placeholder="选择或输入模型名" disabled={mutation.isPending} />
        </Form.Item>
        <Form.Item label="可用渠道" htmlFor="model-group-channels" extra="仅允许使用选中的渠道，未选中的渠道禁止使用。留空表示全部禁止；新建渠道需在此手动加入。">
          <Select id="model-group-channels" mode="multiple" allowClear showSearch optionFilterProp="label"
            value={form.allowedChannelIds}
            options={[
              ...channels.channels.map(channel => ({ value: String(channel.id), label: `${channel.name}${channel.status === 'DISABLED' ? '（已停用）' : ''}` })),
              ...form.allowedChannelIds.filter(id => !channels.channels.some(channel => String(channel.id) === id)).map(id => ({ value: id, label: `已删除渠道 · ${id}` })),
            ]}
            loading={channels.isLoading} disabled={mutation.isPending || channels.isLoading || channels.isError}
            onChange={(allowedChannelIds: string[]) => setForm({ ...form, allowedChannelIds })} placeholder="选择该分组可用的渠道" />
          {channels.isError ? <Alert type="error" showIcon message="渠道列表加载失败" action={<Button size="small" onClick={() => void channels.refetch()}>重试</Button>} /> : null}
          <Space style={{ marginTop: 8 }}>
            <Button size="small" disabled={channels.isLoading || channels.isError || mutation.isPending} onClick={() => setForm({ ...form, allowedChannelIds: channels.channels.map(channel => String(channel.id)) })}>选择全部现有渠道</Button>
            <Button size="small" disabled={mutation.isPending} onClick={() => setForm({ ...form, allowedChannelIds: [] })}>清空选择</Button>
          </Space>
        </Form.Item>
        {form.allowedChannelIds.length === 0 ? <Alert type="warning" showIcon message="尚未选择渠道：此分组下所有 Key 都将无法调用模型。" style={{ marginBottom: 16 }} /> : null}
        <Alert type="info" showIcon message={state?.mode === 'copy' ? '新分组不继承原分组的 Key 绑定和限流额度，请在后台“限流”页单独配置额度。' : '额度请在后台“限流”页配置。分组内所有 Key 共享分组额度。'} style={{ marginBottom: 16 }} />
        {mutation.isError ? <Alert type="error" showIcon message="保存失败" description={mutation.error.message} style={{ marginBottom: 16 }} /> : null}
        <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
          <Button onClick={onClose} disabled={mutation.isPending}>取消</Button>
          <Button type="primary" htmlType="submit" loading={mutation.isPending} disabled={!form.name.trim()}>{state?.mode === 'copy' ? '创建副本' : '保存分组'}</Button>
        </Space>
      </Form>
    </Modal>
  );
}
