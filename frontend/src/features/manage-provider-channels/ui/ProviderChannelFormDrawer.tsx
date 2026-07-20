import { useEffect, useState, type Key } from 'react';
import { Alert, AutoComplete, Button, Divider, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Switch, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { batchUpsertChannelModels, fetchProviderChannelModelPreview, fetchProviderModelPreview, type ChannelModelSupportResponse } from '@entities/channel-model-support';
import type { ProviderChannelResponse, ProtocolMappingRequest } from '@entities/provider-channel';
import type { ApiErrorShape } from '@shared/api';
import { PROTOCOL_OPTIONS, UPSTREAM_PROTOCOL_OPTIONS, formatProtocolDirection, getProtocolMeta } from '@shared/lib/protocols';
import { useProviderChannelMutations } from '../model/useProviderChannelMutations';
import type { ProviderChannelFormState } from '../model/types';

type ProviderChannelFormMode = 'create' | 'edit' | 'copy';

interface ProviderChannelFormDrawerProps {
  /** 打开状态 */
  open: boolean;
  /** 表单模式 */
  mode: ProviderChannelFormMode;
  /** 编辑渠道 */
  channel?: ProviderChannelResponse | null;
  /** 关闭回调 */
  onClose: () => void;
  /** 保存成功回调 */
  onSaved: (channel: ProviderChannelResponse) => void;
}

const DEFAULT_MODELS_PATH = '/v1/models';

const MODELS_PATH_OPTIONS = [
  { label: '/v1/models (OpenAI 兼容)', value: '/v1/models' },
  { label: '/foundation-models (AWS Bedrock)', value: '/foundation-models' },
  { label: '/models (通用)', value: '/models' },
];
const DEFAULT_FORM: ProviderChannelFormState = {
  name: '',
  host: '',
  keyRef: '',
  modelsPath: DEFAULT_MODELS_PATH,
  routePriority: 0,
  supportedProtocols: [],
  protocolMappings: [],
};

function getErrorMessage(error: unknown, fallback: string): string {
  if (typeof error === 'object' && error !== null && 'message' in error) {
    return (error as ApiErrorShape).message || fallback;
  }
  return fallback;
}

function isHttpHost(host: string): boolean {
  return /^https?:\/\//i.test(host.trim());
}

function isFormValidationError(error: unknown): boolean {
  return typeof error === 'object' && error !== null && 'errorFields' in error;
}

function normalizeProtocolMappings(protocols: string[], mappings?: ProtocolMappingRequest[]): ProtocolMappingRequest[] {
  const existing = new Map((mappings ?? []).map((mapping) => [mapping.requestProtocol, mapping.upstreamProtocol]));
  return protocols.map((protocol) => ({
    requestProtocol: protocol,
    upstreamProtocol: existing.get(protocol) ?? protocol,
  }));
}

function derivePreviewUpstreamProtocols(protocols: string[], mappings?: ProtocolMappingRequest[]): string[] {
  const normalizedMappings = normalizeProtocolMappings(protocols, mappings);
  return Array.from(new Set(normalizedMappings.map((mapping) => mapping.upstreamProtocol)));
}

function deriveSupportedProtocols(channel: ProviderChannelResponse): string[] {
  const mappings = channel.protocolMappings ?? [];
  if (mappings.length > 0) {
    return mappings.map((mapping) => mapping.requestProtocol);
  }
  return channel.supportedProtocols ?? [];
}

function modelKey(model: Pick<ChannelModelSupportResponse, 'requestedModel' | 'upstreamProtocol'>): string {
  return `${model.requestedModel}::${model.upstreamProtocol}`;
}

function isMaskedKey(value: string | undefined, keyMasked?: string): boolean {
  const trimmed = value?.trim();
  if (!trimmed) {
    return false;
  }
  return trimmed.includes('****') || Boolean(keyMasked && trimmed === keyMasked);
}

function sanitizeEditableKey(value: string | undefined, keyMasked?: string): string {
  const trimmed = value?.trim() ?? '';
  return isMaskedKey(trimmed, keyMasked) ? '' : trimmed;
}

export function ProviderChannelFormDrawer({ open, mode, channel = null, onClose, onSaved }: ProviderChannelFormDrawerProps) {
  const { createMutation, updateMutation } = useProviderChannelMutations();
  const [form] = Form.useForm<ProviderChannelFormState>();
  const [previewModels, setPreviewModels] = useState<ChannelModelSupportResponse[]>([]);
  const [selectedModelIds, setSelectedModelIds] = useState<Key[]>([]);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [modelsDirty, setModelsDirty] = useState(false);
  const [manualModelName, setManualModelName] = useState('');
  const saving = createMutation.isPending || updateMutation.isPending;
  const selectedProtocols = Form.useWatch('supportedProtocols', form) ?? [];
  const protocolMappings = Form.useWatch('protocolMappings', form) ?? [];
  const existingModels = mode === 'edit' ? channel?.supportedModels ?? [] : [];

  useEffect(() => {
    if (!open) {
      return;
    }
    setModelsDirty(false);
    if (channel && mode !== 'create') {
      const supportedProtocols = deriveSupportedProtocols(channel);
      form.setFieldsValue({
        name: mode === 'copy' ? `${channel.name} - 副本` : channel.name,
        host: channel.host,
        keyRef: '',
        modelsPath: channel.modelsPath ?? DEFAULT_MODELS_PATH,
        routePriority: channel.routePriority ?? 0,
        supportedProtocols,
        protocolMappings: normalizeProtocolMappings(supportedProtocols, channel.protocolMappings),
      });
      setPreviewModels(channel.supportedModels ?? []);
      setSelectedModelIds((channel.supportedModels ?? []).filter((model) => model.status !== 'DISABLED').map((model) => model.id));
      setModelsDirty(mode === 'copy');
      return;
    }
    form.setFieldsValue(DEFAULT_FORM);
    setPreviewModels([]);
    setSelectedModelIds([]);
  }, [channel, form, mode, open]);

  function findExistingModel(model: Pick<ChannelModelSupportResponse, 'requestedModel' | 'upstreamProtocol'>): ChannelModelSupportResponse | undefined {
    return existingModels.find((item) => modelKey(item) === modelKey(model));
  }

  function mergeWithExistingModels(models: ChannelModelSupportResponse[]): ChannelModelSupportResponse[] {
    return models.map((model) => {
      const existing = findExistingModel(model);
      return existing ? {
        ...model,
        id: existing.id,
        priority: existing.priority,
        preferred: existing.preferred,
        source: existing.source,
        status: existing.status,
      } : model;
    });
  }

  function handleSupportedProtocolsChange(protocols: string[]): void {
    form.setFieldValue('supportedProtocols', protocols);
    form.setFieldValue('protocolMappings', normalizeProtocolMappings(protocols, form.getFieldValue('protocolMappings')));
  }

  function handleUpstreamProtocolChange(requestProtocol: string, upstreamProtocol: string): void {
    form.setFieldValue(
      'protocolMappings',
      normalizeProtocolMappings(selectedProtocols, protocolMappings).map((mapping) => (
        mapping.requestProtocol === requestProtocol ? { ...mapping, upstreamProtocol } : mapping
      ))
    );
  }

  async function handlePreviewModels(): Promise<void> {
    try {
      const values = await form.validateFields(['host', 'keyRef', 'modelsPath']);
      if (!isHttpHost(values.host)) {
        form.setFields([{ name: 'host', errors: ['渠道 Host 必须以 http:// 或 https:// 开头'] }]);
        message.warning('渠道 Host 必须以 http:// 或 https:// 开头');
        return;
      }
      const keyRef = sanitizeEditableKey(values.keyRef, channel?.keyMasked);
      if (mode === 'create' && !keyRef) {
        form.setFields([{ name: 'keyRef', errors: ['预览拉取模型需要填写真实渠道 Key'] }]);
        message.warning('预览拉取模型需要填写真实渠道 Key');
        return;
      }
      if (mode === 'edit' && !keyRef && !channel?.hasKey) {
        form.setFields([{ name: 'keyRef', errors: ['当前渠道没有已保存 Key，请输入真实渠道 Key'] }]);
        message.warning('当前渠道没有已保存 Key，请输入真实渠道 Key');
        return;
      }
      setPreviewLoading(true);
      try {
        const upstreamProtocols = derivePreviewUpstreamProtocols(selectedProtocols, protocolMappings);
        if (upstreamProtocols.length === 0) {
          message.warning('请先选择至少一个上游调用协议');
          return;
        }
        const commonParams = {
          host: values.host.trim(),
          modelsPath: values.modelsPath?.trim() || DEFAULT_MODELS_PATH,
          upstreamProtocols,
          defaultPriority: 10,
        };
        const response = mode === 'edit' && channel && !keyRef
          ? await fetchProviderChannelModelPreview(channel.id, commonParams)
          : await fetchProviderModelPreview({ ...commonParams, keyRef });
        const mergedModels = mergeWithExistingModels(response.data.models);
        setPreviewModels(mergedModels);
        setSelectedModelIds(mergedModels.filter((model) => findExistingModel(model)?.status === 'ENABLED').map((model) => model.id));
        setModelsDirty(true);
        message.success(`验证成功，已获取 ${response.data.models.length} 个模型候选`);
      } catch (error) {
        message.error(`验证并获取模型失败：${getErrorMessage(error, '请检查 Host、Key、模型列表路径和模型列表权限')}`);
      } finally {
        setPreviewLoading(false);
      }
    } catch (error) {
      if (isFormValidationError(error)) {
        message.warning('请先填写渠道 Host、渠道 Key 和模型列表路径');
        return;
      }
      message.error(`验证并获取模型失败：${getErrorMessage(error, '请检查 Host、Key、模型列表路径和模型列表权限')}`);
    }
  }

  async function saveSelectedModels(providerChannelId: number): Promise<ProviderChannelResponse | null> {
    if (!modelsDirty) {
      return null;
    }
    const selectedModels = previewModels.filter((model) => selectedModelIds.includes(model.id));
    const response = await batchUpsertChannelModels(providerChannelId, {
      replaceExisting: true,
      models: selectedModels.map((model) => {
        const existing = findExistingModel(model);
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
    });
    setModelsDirty(false);
    message.success(selectedModels.length === 0 ? '已清空该渠道模型配置' : `已保存 ${selectedModels.length} 个模型配置`);
    return response.data;
  }

  async function handleSave(): Promise<void> {
    try {
      const values = await form.validateFields();
      if (!isHttpHost(values.host)) {
        form.setFields([{ name: 'host', errors: ['渠道 Host 必须以 http:// 或 https:// 开头'] }]);
        message.warning('渠道 Host 必须以 http:// 或 https:// 开头');
        return;
      }
      const body = {
        name: values.name.trim(),
        host: values.host.trim(),
        keyRef: sanitizeEditableKey(values.keyRef, channel?.keyMasked),
        modelsPath: values.modelsPath?.trim() || DEFAULT_MODELS_PATH,
        routePriority: values.routePriority ?? 0,
        supportedProtocols: values.supportedProtocols,
        protocolMappings: normalizeProtocolMappings(values.supportedProtocols, values.protocolMappings),
      };
      if (mode === 'edit' && !body.keyRef) {
        delete (body as Partial<typeof body>).keyRef;
      }
      const response = mode !== 'edit'
        ? await createMutation.mutateAsync(body as ProviderChannelFormState)
        : await updateMutation.mutateAsync({ id: channel?.id ?? 0, body });
      let latestChannel: ProviderChannelResponse | null = null;
      try {
        latestChannel = await saveSelectedModels(response.data.id);
      } catch (error) {
        message.error(`渠道已保存，但部分模型保存失败：${getErrorMessage(error, '请进入模型列表重试')}`);
      }
      onSaved(latestChannel ?? response.data);
      onClose();
    } catch (error) {
      if (isFormValidationError(error)) {
        message.warning('请完善表单必填项后再保存');
        return;
      }
      message.error(`保存渠道失败：${getErrorMessage(error, '请检查表单内容后重试')}`);
    }
  }

  function updatePreviewModel(modelId: number, patch: Partial<ChannelModelSupportResponse>): void {
    setModelsDirty(true);
    setPreviewModels((current) => current.map((item) => (item.id === modelId ? { ...item, ...patch } : item)));
  }

  function handleSelectedModelIdsChange(keys: Key[]): void {
    setModelsDirty(true);
    setSelectedModelIds(keys);
  }

  function handleRemovePreviewModel(model: ChannelModelSupportResponse): void {
    setPreviewModels((current) => current.filter((item) => item.id !== model.id));
    setSelectedModelIds((current) => current.filter((id) => id !== model.id));
    setModelsDirty(true);
  }

  function handleAddManualModel(): void {
    const modelName = manualModelName.trim();
    if (!modelName) {
      message.warning('请输入模型名称');
      return;
    }
    const upstreamProtocols = derivePreviewUpstreamProtocols(selectedProtocols, protocolMappings);
    if (upstreamProtocols.length === 0) {
      message.warning('请先选择上游调用协议');
      return;
    }
    const idBase = Date.now() * 1000;
    const newModels: ChannelModelSupportResponse[] = upstreamProtocols.map((protocol, index) => ({
      id: idBase + index + 1,
      requestedModel: modelName,
      upstreamModel: modelName,
      upstreamProtocol: protocol,
      priority: 10,
      preferred: false,
      source: 'MANUAL' as const,
      status: 'ENABLED' as const,
    }));
    const duplicates = newModels.filter((nm) => previewModels.some((pm) => modelKey(pm) === modelKey(nm)));
    if (duplicates.length > 0) {
      message.warning(`模型 ${modelName} 已存在`);
      return;
    }
    setPreviewModels((current) => [...current, ...newModels]);
    setSelectedModelIds((current) => [...current, ...newModels.map((m) => m.id)]);
    setModelsDirty(true);
    setManualModelName('');
    message.success(`已添加模型: ${modelName}`);
  }

  const normalizedMappings = normalizeProtocolMappings(selectedProtocols, protocolMappings);
  const columns: ColumnsType<ChannelModelSupportResponse> = [{
    title: '模型候选',
    dataIndex: 'requestedModel',
    render: (_value, model) => {
      const existing = findExistingModel(model);
      return (
        <Space wrap>
          <Typography.Text strong>{model.requestedModel}</Typography.Text>
          <Typography.Text type="secondary">→ {model.upstreamModel}</Typography.Text>
          <Tag>{getProtocolMeta(model.upstreamProtocol).label}</Tag>
          {existing ? <Tag color="processing">已保存</Tag> : <Tag color="success">新增候选</Tag>}
        </Space>
      );
    },
  }, {
    title: '优先模型',
    dataIndex: 'preferred',
    width: 130,
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
  }, {
    title: '操作',
    key: 'actions',
    width: 110,
    render: (_value, model) => {
      const existing = findExistingModel(model);
      const button = <Button size="small" danger onClick={() => !existing && handleRemovePreviewModel(model)}>移除</Button>;
      if (!existing) {
        return button;
      }
      return (
        <Popconfirm
          title="移除模型配置？"
          description="保存后该模型将从渠道配置中移除，不再参与路由。"
          onConfirm={() => handleRemovePreviewModel(model)}
        >
          <Button size="small" danger>移除</Button>
        </Popconfirm>
      );
    },
  }];

  return (
    <Modal
      title={mode === 'create' ? '新建渠道' : mode === 'copy' ? '复制渠道' : '编辑渠道'}
      open={open}
      onCancel={onClose}
      onOk={() => form.submit()}
      confirmLoading={saving}
      width={820}
      destroyOnHidden
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={DEFAULT_FORM}
        onFinish={() => void handleSave()}
        onFinishFailed={() => message.warning('请完善表单必填项后再保存')}
      >
        <Form.Item name="name" label="渠道名称" rules={[{ required: true, message: '请输入渠道名称' }]}>
          <Input placeholder="例如：OpenAI 主渠道" />
        </Form.Item>
        <Form.Item name="host" label="渠道 Host" rules={[{ required: true, message: '请输入渠道 Host' }]}>
          <Input placeholder="https://api.example.com" />
        </Form.Item>
        <Form.Item
          name="keyRef"
          label="渠道 Key"
          rules={mode !== 'edit' ? [{ required: true, message: '请输入渠道 Key' }] : []}
          extra={mode === 'edit'
            ? `留空则保存和预览均沿用当前 Key；输入新 Key 才会替换。当前：${channel?.keyMasked ?? channel?.keyRef ?? '已配置'}`
            : mode === 'copy'
              ? '已复制渠道配置和选用模型；出于安全考虑，渠道 Key 不会被复制，请重新输入。'
              : '真实 API Key 将由后端保存，接口响应会脱敏'}
        >
          <Input.Password placeholder={mode === 'edit' ? '留空沿用已保存 Key' : '请输入供应商 API Key'} autoComplete="new-password" />
        </Form.Item>
        <Form.Item
          name="modelsPath"
          label="模型列表路径"
          rules={[{ required: true, message: '请选择或输入模型列表路径' }]}
          extra="OpenAI 兼容用 /v1/models；AWS Bedrock 代理用 /foundation-models；也可手动输入自定义路径。"
        >
          <AutoComplete
            placeholder="选择或输入路径"
            options={MODELS_PATH_OPTIONS}
            style={{ width: '100%' }}
            filterOption={(input, option) => (option?.value ?? '').toLowerCase().includes(input.toLowerCase())}
          />
        </Form.Item>
        <Form.Item name="routePriority" label="渠道优先级" extra="数字越大越优先；同优先级命中时会负载均衡">
          <InputNumber style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item
          name="supportedProtocols"
          label="入口支持协议"
          rules={[{ required: true, message: '请选择至少一个入口协议' }]}
          extra="选择入口协议后，请在下方为每个入口协议选择供应商实际调用的上游协议。请求会按入口协议接收，转换为上游协议调用供应商，再把响应转回入口协议。"
        >
          <Select
            mode="multiple"
            placeholder="入口支持协议"
            options={PROTOCOL_OPTIONS}
            maxTagCount="responsive"
            popupMatchSelectWidth={false}
            style={{ width: '100%' }}
            onChange={handleSupportedProtocolsChange}
          />
        </Form.Item>
        <Form.Item name="protocolMappings" hidden>
          <Input type="hidden" />
        </Form.Item>
        <Form.Item label="入口协议 → 上游调用协议" required>
          <Space direction="vertical" style={{ width: '100%' }}>
            <Alert
              type="info"
              showIcon
              message="为每个入口协议选择转换后的上游调用协议"
              description="例如 Claude Messages → OpenAI Responses 表示用 Claude Messages 接收请求，转换为 OpenAI Responses 调用供应商，响应再转回 Claude Messages。"
            />
            {selectedProtocols.length === 0 ? (
              <Typography.Text type="secondary">请先选择入口支持协议。</Typography.Text>
            ) : normalizedMappings.map((mapping) => (
              <Space key={mapping.requestProtocol} wrap>
                <Tag color={getProtocolMeta(mapping.requestProtocol).color}>入口：{getProtocolMeta(mapping.requestProtocol).label}</Tag>
                <Typography.Text type="secondary">转换为</Typography.Text>
                <Select
                  value={mapping.upstreamProtocol}
                  options={UPSTREAM_PROTOCOL_OPTIONS}
                  style={{ width: 240 }}
                  onChange={(value) => handleUpstreamProtocolChange(mapping.requestProtocol, value)}
                />
                <Typography.Text type="secondary">{formatProtocolDirection(mapping.requestProtocol, mapping.upstreamProtocol)}</Typography.Text>
              </Space>
            ))}
          </Space>
        </Form.Item>
      </Form>

      <Divider orientation="left">已保存/预览模型列表</Divider>
      <Space direction="vertical" style={{ width: '100%' }}>
        <Alert
          type="info"
          showIcon
          message="验证并获取模型列表会按上游调用协议真实请求供应商模型列表；编辑渠道时可沿用已保存 Key，并保留已配置的优先模型和排序值。"
          description="获取结果仅作为上游支持模型候选；预览请求只提交上游调用协议，不提交入口请求协议。默认只勾选已保存的启用模型，新候选需要手动勾选后才会保存为选用模型。"
        />
        <Space wrap>
          <Button loading={previewLoading} onClick={() => void handlePreviewModels()}>验证并获取模型列表</Button>
          <Input
            placeholder="手动输入模型名称"
            value={manualModelName}
            onChange={(e) => setManualModelName(e.target.value)}
            onPressEnter={handleAddManualModel}
            style={{ width: 240 }}
          />
          <Button onClick={handleAddManualModel}>手动添加</Button>
        </Space>
        {previewModels.length > 0 ? (
          <Table
            rowKey="id"
            size="small"
            columns={columns}
            dataSource={previewModels}
            pagination={{ pageSize: 6 }}
            rowSelection={{ selectedRowKeys: selectedModelIds, onChange: handleSelectedModelIdsChange }}
          />
        ) : null}
      </Space>
    </Modal>
  );
}
