import { useEffect, useMemo, useState, type Key } from 'react';
import { Alert, Button, InputNumber, Modal, Space, Switch, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { type ChannelModelSupportResponse } from '@entities/channel-model-support';
import type { ProviderChannelResponse } from '@entities/provider-channel';
import type { ApiErrorShape } from '@shared/api';
import { useChannelModelMutations } from '../model/useChannelModelMutations';

function getErrorMessage(error: unknown): string {
  if (typeof error === 'object' && error !== null && 'message' in error) {
    return (error as ApiErrorShape).message || '请检查 Host、Key 和模型列表权限';
  }
  return '请检查 Host、Key 和模型列表权限';
}

interface ChannelModelFetchModalProps {
  /** 打开状态 */
  open: boolean;
  /** 渠道 ID */
  channelId: number;
  /** 渠道名称 */
  channelName: string;
  /** 当前模型列表 */
  models: ChannelModelSupportResponse[];
  /** 关闭回调 */
  onClose: () => void;
  /** 保存成功回调 */
  onFetched: (channel: ProviderChannelResponse) => void;
}

export function ChannelModelFetchModal({ open, channelId, channelName, models, onClose, onFetched }: ChannelModelFetchModalProps) {
  const [defaultPriority, setDefaultPriority] = useState(10);
  const [previewModels, setPreviewModels] = useState<ChannelModelSupportResponse[]>([]);
  const [selectedModelIds, setSelectedModelIds] = useState<Key[]>([]);
  const { previewMutation, batchUpsertMutation } = useChannelModelMutations();

  useEffect(() => {
    if (!open) {
      return;
    }
    setPreviewModels([]);
    setSelectedModelIds([]);
    setDefaultPriority(10);
  }, [open]);

  const existingKeys = useMemo(() => new Set(models.map(modelKey)), [models]);

  function findExisting(model: ChannelModelSupportResponse): ChannelModelSupportResponse | undefined {
    return models.find((item) => item.requestedModel === model.requestedModel && item.upstreamProtocol === model.upstreamProtocol);
  }

  async function handlePreview(): Promise<void> {
    if (defaultPriority < 1) {
      message.warning('默认模型排序值必须大于等于 1');
      return;
    }
    try {
      const response = await previewMutation.mutateAsync({ channelId, body: { defaultPriority } });
      const merged = response.data.models.map((model) => {
        const existing = findExisting(model);
        return existing ? { ...model, id: existing.id, priority: existing.priority, preferred: existing.preferred, source: existing.source } : model;
      });
      setPreviewModels(merged);
      setSelectedModelIds(merged.map((model) => model.id));
      message.success(`验证成功，已获取 ${merged.length} 个模型候选`);
    } catch (error) {
      message.error(`验证并获取模型失败：${getErrorMessage(error)}`);
    }
  }

  async function handleSave(): Promise<void> {
    const selectedModels = previewModels.filter((model) => selectedModelIds.includes(model.id));
    if (selectedModels.length === 0) {
      message.warning('请选择要保存的模型');
      return;
    }
    try {
      const response = await batchUpsertMutation.mutateAsync({
        channelId,
        body: {
          replaceExisting: false,
          models: selectedModels.map((model) => {
            const existing = findExisting(model);
            return {
              id: existing?.id,
              requestedModel: model.requestedModel,
              upstreamModel: model.upstreamModel,
              upstreamProtocol: model.upstreamProtocol,
              priority: model.priority,
              preferred: Boolean(model.preferred),
              source: model.source,
            };
          }),
        },
      });
      onFetched(response.data);
      message.success(`已保存 ${selectedModels.length} 个模型配置`);
      onClose();
    } catch (error) {
      message.error(`保存模型配置失败：${getErrorMessage(error)}`);
    }
  }

  function updatePreviewModel(modelId: number, patch: Partial<ChannelModelSupportResponse>): void {
    setPreviewModels((current) => current.map((item) => (item.id === modelId ? { ...item, ...patch } : item)));
  }

  const columns: ColumnsType<ChannelModelSupportResponse> = [{
    title: '模型候选',
    dataIndex: 'requestedModel',
    render: (_value, model) => (
      <Space wrap>
        <Typography.Text strong>{model.requestedModel}</Typography.Text>
        <Typography.Text type="secondary">→ {model.upstreamModel}</Typography.Text>
        <Tag>{model.upstreamProtocol}</Tag>
        {existingKeys.has(modelKey(model)) ? <Tag color="processing">已存在</Tag> : <Tag color="success">新增</Tag>}
      </Space>
    ),
  }, {
    title: '优先模型',
    dataIndex: 'preferred',
    width: 120,
    render: (_value, model) => (
      <Switch
        checked={Boolean(model.preferred)}
        checkedChildren="★ 优先"
        unCheckedChildren="普通"
        onChange={(checked) => updatePreviewModel(model.id, { preferred: checked })}
      />
    ),
  }, {
    title: '模型排序值',
    dataIndex: 'priority',
    width: 140,
    render: (_value, model) => (
      <InputNumber min={1} value={model.priority} onChange={(value) => updatePreviewModel(model.id, { priority: value ?? 1 })} />
    ),
  }];

  return (
    <Modal title={`验证并获取 ${channelName} 模型列表`} open={open} onCancel={onClose} footer={null} width={820}>
      <Space direction="vertical" style={{ width: '100%' }}>
        <Alert
          type="info"
          showIcon
          message="此操作会请求上游模型列表接口，用于验证当前渠道 Host/Key 是否具备模型列表权限。获取结果仅作为候选，不会自动替换当前模型配置。"
        />
        <Space wrap>
          <Typography.Text>默认模型排序值</Typography.Text>
          <InputNumber min={1} value={defaultPriority} onChange={(value) => setDefaultPriority(value ?? 1)} />
          <Button type="primary" loading={previewMutation.isPending} onClick={handlePreview}>验证并获取模型列表</Button>
        </Space>
        {previewModels.length > 0 ? (
          <>
            <Table
              rowKey="id"
              size="small"
              columns={columns}
              dataSource={previewModels}
              pagination={{ pageSize: 8 }}
              rowSelection={{ selectedRowKeys: selectedModelIds, onChange: setSelectedModelIds }}
            />
            <Space>
              <Button type="primary" loading={batchUpsertMutation.isPending} onClick={handleSave}>保存所选模型</Button>
              <Typography.Text type="secondary">优先模型会在路由时优先尝试；模型排序值数字越小越优先。</Typography.Text>
            </Space>
          </>
        ) : null}
      </Space>
    </Modal>
  );
}

function modelKey(model: ChannelModelSupportResponse): string {
  return `${model.requestedModel}::${model.upstreamProtocol}`;
}
