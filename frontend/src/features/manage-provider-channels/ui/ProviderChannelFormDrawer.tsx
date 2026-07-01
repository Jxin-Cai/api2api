import { useEffect, useState, type Key } from 'react';
import { Alert, Button, Divider, Form, Input, InputNumber, Modal, Select, Space, Switch, Table, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { batchUpsertChannelModels, fetchProviderModelPreview, type ChannelModelSupportResponse } from '@entities/channel-model-support';
import type { ProviderChannelResponse } from '@entities/provider-channel';
import type { ApiErrorShape } from '@shared/api';
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

const DEFAULT_FORM: ProviderChannelFormState = { name: '', host: '', keyRef: '', routePriority: 0, supportedProtocols: [] };

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

export function ProviderChannelFormDrawer({ open, mode, channel = null, onClose, onSaved }: ProviderChannelFormDrawerProps) {
  const { createMutation, updateMutation } = useProviderChannelMutations();
  const [form] = Form.useForm<ProviderChannelFormState>();
  const [previewModels, setPreviewModels] = useState<ChannelModelSupportResponse[]>([]);
  const [selectedModelIds, setSelectedModelIds] = useState<Key[]>([]);
  const [previewLoading, setPreviewLoading] = useState(false);
  const saving = createMutation.isPending || updateMutation.isPending;

  useEffect(() => {
    if (!open) {
      return;
    }
    setPreviewModels([]);
    setSelectedModelIds([]);
    if (channel && mode === 'edit') {
      form.setFieldsValue({
        name: channel.name,
        host: channel.host,
        keyRef: '',
        routePriority: channel.routePriority ?? 0,
        supportedProtocols: channel.supportedProtocols,
      });
      return;
    }
    form.setFieldsValue(DEFAULT_FORM);
  }, [channel, form, mode, open]);

  async function handlePreviewModels(): Promise<void> {
    try {
      const values = await form.validateFields(['host', 'keyRef', 'supportedProtocols']);
      if (!isHttpHost(values.host)) {
        form.setFields([{ name: 'host', errors: ['渠道 Host 必须以 http:// 或 https:// 开头'] }]);
        message.warning('渠道 Host 必须以 http:// 或 https:// 开头');
        return;
      }
      setPreviewLoading(true);
      try {
        const response = await fetchProviderModelPreview({
          host: values.host.trim(),
          keyRef: values.keyRef.trim(),
          supportedProtocols: values.supportedProtocols,
          defaultPriority: 10,
        });
        setPreviewModels(response.data.models);
        setSelectedModelIds(response.data.models.map((model) => model.id));
        message.success(`验证成功，已获取 ${response.data.models.length} 个模型候选`);
      } catch (error) {
        message.error(`验证并获取模型失败：${getErrorMessage(error, '请检查 Host、Key 和模型列表权限')}`);
      } finally {
        setPreviewLoading(false);
      }
    } catch (error) {
      if (isFormValidationError(error)) {
        message.warning('请先填写渠道 Host、渠道 Key 和支持协议');
        return;
      }
      message.error(`验证并获取模型失败：${getErrorMessage(error, '请检查 Host、Key 和模型列表权限')}`);
    }
  }

  async function saveSelectedModels(providerChannelId: number): Promise<ProviderChannelResponse | null> {
    const selectedModels = previewModels.filter((model) => selectedModelIds.includes(model.id));
    if (selectedModels.length === 0) {
      return null;
    }
    const response = await batchUpsertChannelModels(providerChannelId, {
      replaceExisting: false,
      models: selectedModels.map((model) => ({
        requestedModel: model.requestedModel,
        upstreamModel: model.upstreamModel,
        upstreamProtocol: model.upstreamProtocol,
        priority: model.priority,
        preferred: Boolean(model.preferred),
        source: model.source,
      })),
    });
    return response.data;
  }

  async function handleSave(): Promise<void> {
    try {
      const values = await form.validateFields();
      if (!isHttpHost(values.host)) {
        message.warning('渠道 Host 必须以 http:// 或 https:// 开头');
        return;
      }
      const body = {
        name: values.name.trim(),
        host: values.host.trim(),
        keyRef: values.keyRef?.trim(),
        routePriority: values.routePriority ?? 0,
        supportedProtocols: values.supportedProtocols,
      };
      if (mode === 'edit' && !body.keyRef) {
        delete (body as Partial<typeof body>).keyRef;
      }
      const response = mode === 'create'
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
    setPreviewModels((current) => current.map((item) => (item.id === modelId ? { ...item, ...patch } : item)));
  }

  const columns: ColumnsType<ChannelModelSupportResponse> = [{
    title: '模型候选',
    dataIndex: 'requestedModel',
    render: (_value, model) => (
      <Space wrap>
        <Typography.Text strong>{model.requestedModel}</Typography.Text>
        <Typography.Text type="secondary">→ {model.upstreamModel}</Typography.Text>
        <span>{model.upstreamProtocol}</span>
      </Space>
    ),
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
  }];

  return (
    <Modal
      title={mode === 'create' ? '新建渠道' : '编辑渠道'}
      open={open}
      onCancel={onClose}
      onOk={() => form.submit()}
      confirmLoading={saving}
      width={760}
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
          rules={mode === 'create' ? [{ required: true, message: '请输入渠道 Key' }] : []}
          extra={mode === 'edit' ? `留空则保存时保持当前 Key 不变；如需预览拉取模型，请重新输入真实 Key。当前：${channel?.keyMasked ?? channel?.keyRef ?? '已配置'}` : '真实 API Key 将由后端保存，接口响应会脱敏'}
        >
          <Input.Password placeholder={mode === 'edit' ? '留空不修改' : '请输入供应商 API Key'} autoComplete="new-password" />
        </Form.Item>
        <Form.Item name="routePriority" label="渠道优先级" extra="数字越大越优先；同优先级命中时会负载均衡">
          <InputNumber style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="supportedProtocols" label="支持协议" rules={[{ required: true, message: '请选择至少一个协议' }]}>
          <Select
            mode="multiple"
            placeholder="支持协议"
            options={PROTOCOL_OPTIONS}
            maxTagCount="responsive"
            popupMatchSelectWidth={false}
            style={{ width: '100%' }}
          />
        </Form.Item>
      </Form>

      <Divider orientation="left">模型列表</Divider>
      <Space direction="vertical" style={{ width: '100%' }}>
        <Alert type="info" showIcon message="验证并获取模型列表会真实请求上游模型列表接口，用于试探 Host/Key 是否具备模型列表权限；结果仅作为候选，保存前不会改变配置。" />
        <Button loading={previewLoading} onClick={() => void handlePreviewModels()}>验证并获取模型列表</Button>
        {previewModels.length > 0 ? (
          <Table
            rowKey="id"
            size="small"
            columns={columns}
            dataSource={previewModels}
            pagination={{ pageSize: 6 }}
            rowSelection={{ selectedRowKeys: selectedModelIds, onChange: setSelectedModelIds }}
          />
        ) : null}
      </Space>
    </Modal>
  );
}
