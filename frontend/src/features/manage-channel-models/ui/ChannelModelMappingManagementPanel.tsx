import { useMemo, useState } from 'react';
import { Button, Card, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Switch, Table, Tag, Tooltip, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { ModelPriorityBadge, ModelSourceTag, type ChannelModelSupportResponse } from '@entities/channel-model-support';
import { useProviderChannels, type ProviderChannelResponse } from '@entities/provider-channel';
import { PAGE_SIZE_OPTIONS } from '@shared/config/constants';
import { getProtocolMeta, PROTOCOL_OPTIONS } from '@shared/lib/protocols';
import { PageState } from '@shared/ui';
import { useChannelModelMutations } from '../model/useChannelModelMutations';
import { ChannelModelFetchModal } from './ChannelModelFetchModal';

interface ModelMappingRow {
  key: string;
  channel: ProviderChannelResponse;
  model: ChannelModelSupportResponse;
}

interface ModelMappingFormValues {
  channelId: number;
  requestedModel: string;
  upstreamModel: string;
  upstreamProtocol: string;
  priority: number;
  preferred?: boolean;
  source: string;
}

interface MappingModalState {
  open: boolean;
  mode: 'create' | 'edit';
  row: ModelMappingRow | null;
}

const DEFAULT_FORM_VALUES: Partial<ModelMappingFormValues> = {
  priority: 10,
  preferred: false,
  source: 'MANUAL',
};

export function ChannelModelMappingManagementPanel() {
  const { channels, channelOptions, isLoading, isError, refetch } = useProviderChannels();
  const { upsertMutation, removeMutation } = useChannelModelMutations();
  const [form] = Form.useForm<ModelMappingFormValues>();
  const selectedFormChannelId = Form.useWatch('channelId', form);
  const [localChannels, setLocalChannels] = useState<ProviderChannelResponse[]>([]);
  const [search, setSearch] = useState('');
  const [channelFilter, setChannelFilter] = useState<number | undefined>();
  const [protocolFilter, setProtocolFilter] = useState<string | undefined>();
  const [sourceFilter, setSourceFilter] = useState<string | undefined>();
  const [statusFilter, setStatusFilter] = useState<string | undefined>();
  const [preferredFilter, setPreferredFilter] = useState<string | undefined>();
  const [modal, setModal] = useState<MappingModalState>({ open: false, mode: 'create', row: null });
  const [fetchModalOpen, setFetchModalOpen] = useState(false);

  const sourceChannels = localChannels.length > 0 ? localChannels : channels;
  const rows = useMemo(() => flattenModelMappings(sourceChannels), [sourceChannels]);
  const protocolOptions = useMemo(() => {
    const protocols = Array.from(new Set(rows.map((row) => row.model.upstreamProtocol))).filter(Boolean);
    return protocols.map((protocol) => ({ label: getProtocolMeta(protocol).label, value: protocol }));
  }, [rows]);
  const filteredRows = useMemo(() => rows.filter((row) => {
    const keyword = search.trim().toLowerCase();
    const text = [
      row.channel.name,
      row.channel.host,
      row.model.requestedModel,
      row.model.upstreamModel,
      row.model.upstreamProtocol,
      row.model.source,
      row.model.status,
    ].join(' ').toLowerCase();
    if (keyword && !text.includes(keyword)) {
      return false;
    }
    if (channelFilter && row.channel.id !== channelFilter) {
      return false;
    }
    if (protocolFilter && row.model.upstreamProtocol !== protocolFilter) {
      return false;
    }
    if (sourceFilter && row.model.source !== sourceFilter) {
      return false;
    }
    if (statusFilter && row.model.status !== statusFilter) {
      return false;
    }
    if (preferredFilter === 'preferred' && !row.model.preferred) {
      return false;
    }
    if (preferredFilter === 'normal' && row.model.preferred) {
      return false;
    }
    return true;
  }), [channelFilter, preferredFilter, protocolFilter, rows, search, sourceFilter, statusFilter]);

  const selectedFormChannel = sourceChannels.find((channel) => channel.id === selectedFormChannelId);
  const selectedFetchChannel = sourceChannels.find((channel) => channel.id === channelFilter);
  const upstreamProtocolOptions = useMemo(() => getUpstreamProtocolOptions(selectedFormChannel), [selectedFormChannel]);

  function handleChannelChanged(channel: ProviderChannelResponse): void {
    setLocalChannels((current) => {
      const next = current.length > 0 ? current : channels;
      const exists = next.some((item) => item.id === channel.id);
      return exists ? next.map((item) => (item.id === channel.id ? channel : item)) : [channel, ...next];
    });
    void refetch();
  }

  function handleRefresh(): void {
    setLocalChannels([]);
    void refetch();
  }

  function openCreateModal(): void {
    const defaultChannelId = channelFilter ?? sourceChannels[0]?.id;
    setModal({ open: true, mode: 'create', row: null });
    form.setFieldsValue({
      ...DEFAULT_FORM_VALUES,
      channelId: defaultChannelId,
      requestedModel: '',
      upstreamModel: '',
      upstreamProtocol: getUpstreamProtocolOptions(sourceChannels.find((channel) => channel.id === defaultChannelId))[0]?.value ?? '',
    });
  }

  function openEditModal(row: ModelMappingRow): void {
    setModal({ open: true, mode: 'edit', row });
    form.setFieldsValue({
      channelId: row.channel.id,
      requestedModel: row.model.requestedModel,
      upstreamModel: row.model.upstreamModel,
      upstreamProtocol: row.model.upstreamProtocol,
      priority: row.model.priority,
      preferred: Boolean(row.model.preferred),
      source: row.model.source,
    });
  }

  function closeModal(): void {
    setModal((current) => ({ ...current, open: false }));
    form.resetFields();
  }

  async function handleSave(): Promise<void> {
    const values = await form.validateFields();
    const response = await upsertMutation.mutateAsync({
      channelId: values.channelId,
      modelId: modal.mode === 'create' ? 0 : modal.row?.model.id ?? 0,
      body: {
        requestedModel: values.requestedModel,
        upstreamModel: values.upstreamModel,
        upstreamProtocol: values.upstreamProtocol,
        priority: values.priority,
        preferred: Boolean(values.preferred),
        source: values.source,
      },
    });
    handleChannelChanged(response.data);
    message.success(modal.mode === 'create' ? '模型映射已新增' : '模型映射已更新');
    closeModal();
  }

  async function handleDelete(row: ModelMappingRow): Promise<void> {
    const response = await removeMutation.mutateAsync({
      channelId: row.channel.id,
      body: {
        requestedModel: row.model.requestedModel,
        upstreamProtocol: row.model.upstreamProtocol,
      },
    });
    handleChannelChanged(response.data);
    message.success('模型映射已删除');
  }

  const columns: ColumnsType<ModelMappingRow> = [{
    title: '渠道',
    dataIndex: ['channel', 'name'],
    width: 220,
    render: (_value, row) => (
      <Space direction="vertical" size={2}>
        <Typography.Text strong>{row.channel.name}</Typography.Text>
        <Space size={6} wrap>
          <Tag color={row.channel.status === 'ENABLED' ? 'success' : 'default'}>{row.channel.status}</Tag>
          <Typography.Text type="secondary">ID {row.channel.id}</Typography.Text>
        </Space>
      </Space>
    ),
  }, {
    title: '模型映射',
    dataIndex: ['model', 'requestedModel'],
    render: (_value, row) => (
      <Space wrap>
        <Typography.Text strong>{row.model.requestedModel}</Typography.Text>
        <Typography.Text type="secondary">→</Typography.Text>
        <Typography.Text>{row.model.upstreamModel}</Typography.Text>
        <Tag color={getProtocolMeta(row.model.upstreamProtocol).color}>{getProtocolMeta(row.model.upstreamProtocol).label}</Tag>
      </Space>
    ),
  }, {
    title: '排序',
    dataIndex: ['model', 'priority'],
    width: 120,
    render: (_value, row) => <ModelPriorityBadge priority={row.model.priority} />,
  }, {
    title: '优先',
    dataIndex: ['model', 'preferred'],
    width: 110,
    render: (_value, row) => row.model.preferred ? <Tag color="gold">★ 优先</Tag> : <Tag>普通</Tag>,
  }, {
    title: '来源',
    dataIndex: ['model', 'source'],
    width: 120,
    render: (_value, row) => <ModelSourceTag source={row.model.source} />,
  }, {
    title: '状态',
    dataIndex: ['model', 'status'],
    width: 110,
    render: (_value, row) => <Tag color={row.model.status === 'ENABLED' ? 'success' : 'default'}>{row.model.status}</Tag>,
  }, {
    title: '更新时间',
    dataIndex: ['model', 'updatedAt'],
    width: 180,
    render: (_value, row) => row.model.updatedAt ? formatTime(row.model.updatedAt) : '-',
  }, {
    title: '操作',
    key: 'actions',
    width: 150,
    fixed: 'right',
    render: (_value, row) => (
      <Space>
        <Button size="small" onClick={() => openEditModal(row)}>编辑</Button>
        <Popconfirm title="确认删除模型映射？" onConfirm={() => void handleDelete(row)}>
          <Button size="small" danger loading={removeMutation.isPending}>删除</Button>
        </Popconfirm>
      </Space>
    ),
  }];

  if (isError) {
    return <PageState status="error" title="模型映射加载失败" onRetry={handleRefresh} />;
  }

  return (
    <Card>
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Space wrap style={{ width: '100%', justifyContent: 'space-between' }}>
          <Space wrap>
            <Input.Search
              allowClear
              placeholder="搜索渠道、请求模型、上游模型"
              style={{ width: 280 }}
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
            <Select
              allowClear
              showSearch
              placeholder="全部渠道"
              optionFilterProp="label"
              style={{ width: 220 }}
              value={channelFilter}
              options={channelOptions}
              onChange={setChannelFilter}
            />
            <Select
              allowClear
              placeholder="上游协议"
              style={{ width: 190 }}
              value={protocolFilter}
              options={protocolOptions}
              onChange={setProtocolFilter}
            />
            <Select
              allowClear
              placeholder="来源"
              style={{ width: 130 }}
              value={sourceFilter}
              options={[{ label: 'MANUAL', value: 'MANUAL' }, { label: 'FETCHED', value: 'FETCHED' }]}
              onChange={setSourceFilter}
            />
            <Select
              allowClear
              placeholder="状态"
              style={{ width: 130 }}
              value={statusFilter}
              options={[{ label: 'ENABLED', value: 'ENABLED' }, { label: 'DISABLED', value: 'DISABLED' }]}
              onChange={setStatusFilter}
            />
            <Select
              allowClear
              placeholder="优先模型"
              style={{ width: 130 }}
              value={preferredFilter}
              options={[{ label: '仅优先', value: 'preferred' }, { label: '非优先', value: 'normal' }]}
              onChange={setPreferredFilter}
            />
          </Space>
          <Space wrap>
            <Tooltip title={selectedFetchChannel ? undefined : '请先在筛选区选择一个渠道'}>
              <Button disabled={!selectedFetchChannel} onClick={() => setFetchModalOpen(true)}>验证并获取模型列表</Button>
            </Tooltip>
            <Button onClick={handleRefresh} loading={isLoading}>刷新</Button>
            <Button type="primary" disabled={sourceChannels.length === 0} onClick={openCreateModal}>新增映射</Button>
          </Space>
        </Space>
        <Table
          rowKey="key"
          columns={columns}
          dataSource={filteredRows}
          loading={isLoading}
          scroll={{ x: 1200 }}
          pagination={{ pageSize: 20, showSizeChanger: true, pageSizeOptions: PAGE_SIZE_OPTIONS.map(String), showTotal: (total) => `共 ${total} 条` }}
        />
      </Space>
      <Modal
        title={modal.mode === 'create' ? '新增模型映射' : '编辑模型映射'}
        open={modal.open}
        confirmLoading={upsertMutation.isPending}
        onOk={() => void handleSave()}
        onCancel={closeModal}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" initialValues={DEFAULT_FORM_VALUES}>
          <Form.Item name="channelId" label="所属渠道" rules={[{ required: true, message: '请选择所属渠道' }]}>
            <Select
              disabled={modal.mode === 'edit'}
              showSearch
              optionFilterProp="label"
              options={channelOptions}
              placeholder="请选择渠道"
              onChange={() => form.setFieldValue('upstreamProtocol', undefined)}
            />
          </Form.Item>
          <Form.Item name="requestedModel" label="请求模型" rules={[{ required: true, whitespace: true, message: '请输入请求模型' }]}>
            <Input placeholder="例如 claude-sonnet-4-5" />
          </Form.Item>
          <Form.Item name="upstreamModel" label="上游模型" rules={[{ required: true, whitespace: true, message: '请输入上游模型' }]}>
            <Input placeholder="例如 claude-sonnet-4-6" />
          </Form.Item>
          <Form.Item name="upstreamProtocol" label="上游协议" rules={[{ required: true, message: '请选择上游协议' }]}>
            <Select options={upstreamProtocolOptions} placeholder="请选择上游协议" />
          </Form.Item>
          <Form.Item name="priority" label="模型排序值" rules={[{ required: true, message: '请输入排序值' }]}>
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="preferred" label="优先模型" valuePropName="checked">
            <Switch checkedChildren="★ 优先" unCheckedChildren="普通" />
          </Form.Item>
          <Form.Item name="source" label="来源" rules={[{ required: true, message: '请选择来源' }]}>
            <Select options={[{ label: 'MANUAL', value: 'MANUAL' }, { label: 'FETCHED', value: 'FETCHED' }]} />
          </Form.Item>
        </Form>
      </Modal>
      {selectedFetchChannel ? (
        <ChannelModelFetchModal
          open={fetchModalOpen}
          channelId={selectedFetchChannel.id}
          channelName={selectedFetchChannel.name}
          models={selectedFetchChannel.supportedModels}
          onClose={() => setFetchModalOpen(false)}
          onFetched={handleChannelChanged}
        />
      ) : null}
    </Card>
  );
}

function flattenModelMappings(channels: ProviderChannelResponse[]): ModelMappingRow[] {
  return channels.flatMap((channel) => channel.supportedModels.map((model) => ({
    key: `${channel.id}:${model.id}:${model.requestedModel}:${model.upstreamProtocol}`,
    channel,
    model,
  })));
}

function getUpstreamProtocolOptions(channel?: ProviderChannelResponse): Array<{ label: string; value: string }> {
  const protocols = Array.from(new Set((channel?.protocolMappings ?? []).map((mapping) => mapping.upstreamProtocol).filter(Boolean)));
  if (protocols.length === 0) {
    return PROTOCOL_OPTIONS;
  }
  return protocols.map((protocol) => ({ label: getProtocolMeta(protocol).label, value: protocol }));
}

function formatTime(value: number): string {
  return new Date(value).toLocaleString();
}
