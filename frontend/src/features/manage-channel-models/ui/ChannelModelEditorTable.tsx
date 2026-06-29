import { useState } from 'react';
import { Button, Input, InputNumber, Popconfirm, Select, Space, Table, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { ChannelModelSupportRow, type ChannelModelSupportResponse } from '@entities/channel-model-support';
import type { ProviderChannelResponse } from '@entities/provider-channel';
import { useChannelModelMutations } from '../model/useChannelModelMutations';
import type { ChannelModelDraft, ChannelModelEditingId } from '../model/types';

interface ChannelModelEditorTableProps {
  /** 渠道 ID */
  channelId: number;
  /** 当前模型列表 */
  models: ChannelModelSupportResponse[];
  /** 变更成功回调 */
  onChanged: (channel: ProviderChannelResponse) => void;
}

const EMPTY_DRAFT: ChannelModelDraft = { requestedModel: '', upstreamModel: '', upstreamProtocol: '', priority: 10, source: 'MANUAL', status: 'ENABLED' };

export function ChannelModelEditorTable({ channelId, models, onChanged }: ChannelModelEditorTableProps) {
  const { upsertMutation, removeMutation } = useChannelModelMutations();
  const [editingId, setEditingId] = useState<ChannelModelEditingId>(null);
  const [draft, setDraft] = useState<ChannelModelDraft>(EMPTY_DRAFT);

  function updateDraft<K extends keyof ChannelModelDraft>(key: K, value: ChannelModelDraft[K]): void {
    setDraft((current) => ({ ...current, [key]: value }));
  }

  async function handleSave(): Promise<void> {
    if (!draft.requestedModel || !draft.upstreamModel || !draft.upstreamProtocol || draft.priority < 1 || !draft.source) {
      message.warning('请完整填写模型映射信息');
      return;
    }
    const response = await upsertMutation.mutateAsync({ channelId, modelId: editingId === 'new' ? 0 : Number(editingId), body: draft });
    onChanged(response.data);
    setEditingId(null);
    setDraft(EMPTY_DRAFT);
  }

  async function handleDelete(model: ChannelModelSupportResponse): Promise<void> {
    const response = await removeMutation.mutateAsync({ channelId, body: { requestedModel: model.requestedModel, upstreamProtocol: model.upstreamProtocol } });
    onChanged(response.data);
  }

  const data = editingId === 'new' ? [{ id: 0, ...draft } as ChannelModelSupportResponse, ...models] : models;
  const columns: ColumnsType<ChannelModelSupportResponse> = [{
    title: '模型',
    dataIndex: 'requestedModel',
    render: (_value: string, model) => model.id === 0 || editingId === model.id ? (
      <Space wrap>
        <Input placeholder="请求模型" value={draft.requestedModel} onChange={(event) => updateDraft('requestedModel', event.target.value)} />
        <Input placeholder="上游模型" value={draft.upstreamModel} onChange={(event) => updateDraft('upstreamModel', event.target.value)} />
        <Input placeholder="协议" value={draft.upstreamProtocol} onChange={(event) => updateDraft('upstreamProtocol', event.target.value)} />
        <InputNumber min={1} value={draft.priority} onChange={(value) => updateDraft('priority', value ?? 1)} />
        <Select value={draft.source} onChange={(value) => updateDraft('source', value)} options={[{ label: 'MANUAL', value: 'MANUAL' }, { label: 'FETCHED', value: 'FETCHED' }]} style={{ width: 130 }} />
        <Button type="primary" loading={upsertMutation.isPending} onClick={handleSave}>保存</Button>
        <Button onClick={() => setEditingId(null)}>取消</Button>
      </Space>
    ) : (
      <ChannelModelSupportRow model={model} actions={<Space><Button size="small" onClick={() => { setEditingId(model.id); setDraft({ requestedModel: model.requestedModel, upstreamModel: model.upstreamModel, upstreamProtocol: model.upstreamProtocol, priority: model.priority, source: model.source }); }}>编辑</Button><Popconfirm title="确认删除模型？" onConfirm={() => handleDelete(model)}><Button size="small" danger>删除</Button></Popconfirm></Space>} />
    ),
  }];

  return (
    <Space direction="vertical" style={{ width: '100%' }}>
      <Button disabled={editingId !== null} onClick={() => { setEditingId('new'); setDraft(EMPTY_DRAFT); }}>手动新增模型</Button>
      <Table rowKey="id" size="small" columns={columns} dataSource={data} pagination={false} scroll={{ x: true }} />
    </Space>
  );
}
