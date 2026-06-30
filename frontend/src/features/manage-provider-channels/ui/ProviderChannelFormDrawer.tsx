import { useEffect, useState, type Key } from 'react';
import { Button, Divider, Form, Input, InputNumber, Modal, Select, Space, Switch, Table, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { fetchProviderModelPreview, upsertChannelModel, type ChannelModelSupportResponse } from '@entities/channel-model-support';
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

const DEFAULT_FORM: ProviderChannelFormState = { name: '', host: '', keyRef: '', routePriority: 0, supportedProtocols: [] };

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
    const values = form.getFieldsValue();
    if (!values.host?.trim() || !values.keyRef?.trim() || !values.supportedProtocols?.length) {
      message.warning('请先填写渠道 Host、渠道 Key 和支持协议');
      return;
    }
    setPreviewLoading(true);
    try {
      const response = await fetchProviderModelPreview({
        host: values.host,
        keyRef: values.keyRef,
        supportedProtocols: values.supportedProtocols,
        defaultPriority: 10,
      });
      setPreviewModels(response.data.models);
      setSelectedModelIds(response.data.models.map((model) => model.id));
      message.success(`已拉取 ${response.data.models.length} 个模型`);
    } finally {
      setPreviewLoading(false);
    }
  }

  async function saveSelectedModels(providerChannelId: number): Promise<ProviderChannelResponse | null> {
    const selectedModels = previewModels.filter((model) => selectedModelIds.includes(model.id));
    let latestChannel: ProviderChannelResponse | null = null;
    for (const model of selectedModels) {
      const response = await upsertChannelModel(providerChannelId, model.id, {
        requestedModel: model.requestedModel,
        upstreamModel: model.upstreamModel,
        upstreamProtocol: model.upstreamProtocol,
        priority: model.priority,
        preferred: Boolean(model.preferred),
        source: model.source,
      });
      latestChannel = response.data;
    }
    return latestChannel;
  }

  async function handleSave(): Promise<void> {
    const values = await form.validateFields();
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
    const latestChannel = await saveSelectedModels(response.data.id);
    onSaved(latestChannel ?? response.data);
    onClose();
  }

  const columns: ColumnsType<ChannelModelSupportResponse> = [{
    title: '模型',
    dataIndex: 'requestedModel',
    render: (_value, model) => (
      <Space wrap>
        <span>{model.requestedModel}</span>
        <span style={{ color: '#8c8c8c' }}>→ {model.upstreamModel}</span>
        <span>{model.upstreamProtocol}</span>
      </Space>
    ),
  }, {
    title: '优先模型',
    dataIndex: 'preferred',
    width: 120,
    render: (_value, model) => (
      <Switch
        checked={Boolean(model.preferred)}
        checkedChildren="是"
        unCheckedChildren="否"
        onChange={(checked) => setPreviewModels((current) => current.map((item) => (
          item.id === model.id ? { ...item, preferred: checked } : item
        )))}
      />
    ),
  }];

  return (
    <Modal
      title={mode === 'create' ? '新建渠道' : '编辑渠道'}
      open={open}
      onCancel={onClose}
      onOk={() => void handleSave()}
      confirmLoading={saving}
      width={760}
      destroyOnHidden
    >
      <Form form={form} layout="vertical" initialValues={DEFAULT_FORM}>
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
          extra={mode === 'edit' ? `留空则保持当前 Key 不变，当前：${channel?.keyMasked ?? channel?.keyRef ?? '已配置'}` : '真实 API Key 将由后端保存，接口响应会脱敏'}
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
        <Button loading={previewLoading} onClick={() => void handlePreviewModels()}>拉取模型列表</Button>
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
