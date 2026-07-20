import { useMemo, useState, type Key, type ReactNode } from 'react';
import { Button, Card, message, Popconfirm, Space, Table, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { ProviderChannelRow, useProviderChannels, type ProviderChannelResponse } from '@entities/provider-channel';
import { ProviderChannelFormDrawer } from './ProviderChannelFormDrawer';
import { ProviderChannelToolbar, type ProviderChannelStatusFilter } from './ProviderChannelToolbar';
import { useProviderChannelMutations } from '../model/useProviderChannelMutations';

interface ProviderChannelTablePanelProps {
  /** 模型展开区渲染函数 */
  renderModelsPanel?: (channel: ProviderChannelResponse, onChanged: (channel: ProviderChannelResponse) => void) => ReactNode;
}

function needsReenable(channel: ProviderChannelResponse): boolean {
  return channel.status !== 'ENABLED'
    || channel.supportedModels.some((model) => model.status === 'RATE_LIMITED');
}

export function ProviderChannelTablePanel({ renderModelsPanel }: ProviderChannelTablePanelProps) {
  const { channels, isLoading, refetch } = useProviderChannels();
  const { enableMutation, disableMutation, deleteMutation } = useProviderChannelMutations();
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<ProviderChannelStatusFilter>();
  const [selectedChannelIds, setSelectedChannelIds] = useState<Key[]>([]);
  const [batchChanging, setBatchChanging] = useState(false);
  const [drawer, setDrawer] = useState<{ open: boolean; mode: 'create' | 'edit' | 'copy'; channel: ProviderChannelResponse | null }>({ open: false, mode: 'create', channel: null });
  const [localChannels, setLocalChannels] = useState<ProviderChannelResponse[]>([]);
  const source = localChannels.length > 0 ? localChannels : channels;

  const filtered = useMemo(() => source.filter((channel) => {
    const mappings = (channel.protocolMappings ?? []).map((mapping) => `${mapping.requestProtocol} ${mapping.upstreamProtocol}`).join(' ');
    const models = channel.supportedModels.map((model) => `${model.requestedModel} ${model.upstreamModel}`).join(' ');
    const text = `${channel.name} ${channel.host} ${channel.supportedProtocols.join(' ')} ${mappings} ${models}`.toLowerCase();
    return text.includes(search.trim().toLowerCase()) && (!statusFilter || channel.status === statusFilter);
  }), [source, search, statusFilter]);

  const selectedChannels = useMemo(() => {
    const selectedIds = new Set(selectedChannelIds.map(String));
    return source.filter((channel) => selectedIds.has(String(channel.id)));
  }, [selectedChannelIds, source]);
  const canBatchEnable = selectedChannels.some(needsReenable);
  const canBatchDisable = selectedChannels.some((channel) => channel.status === 'ENABLED');

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

  async function handleEnable(channelId: number): Promise<void> {
    await enableMutation.mutateAsync(channelId);
    handleRefresh();
  }

  async function handleDisable(channelId: number): Promise<void> {
    await disableMutation.mutateAsync(channelId);
    handleRefresh();
  }

  async function handleDelete(channelId: number): Promise<void> {
    await deleteMutation.mutateAsync(channelId);
    handleRefresh();
  }

  async function handleBatchStatus(targetStatus: ProviderChannelStatusFilter): Promise<void> {
    const targets = selectedChannels.filter((channel) => (
      targetStatus === 'ENABLED' ? needsReenable(channel) : channel.status !== targetStatus
    ));
    if (targets.length === 0) {
      return;
    }
    setBatchChanging(true);
    try {
      const results = await Promise.allSettled(targets.map((channel) => (
        targetStatus === 'ENABLED'
          ? enableMutation.mutateAsync(channel.id)
          : disableMutation.mutateAsync(channel.id)
      )));
      const succeeded = results.filter((result) => result.status === 'fulfilled').length;
      const failed = results.length - succeeded;
      if (failed > 0) {
        message.error(`批量操作完成：成功 ${succeeded} 个，失败 ${failed} 个`);
      } else {
        message.success(targetStatus === 'ENABLED' ? `已重新启用 ${succeeded} 个渠道` : `已禁用 ${succeeded} 个渠道`);
      }
      setSelectedChannelIds([]);
      handleRefresh();
    } finally {
      setBatchChanging(false);
    }
  }

  const columns: ColumnsType<ProviderChannelResponse> = [{
    title: '渠道',
    dataIndex: 'name',
    render: (_value: string, channel) => (
      <ProviderChannelRow
        channel={channel}
        actions={
          <Space>
            <Button size="small" onClick={() => setDrawer({ open: true, mode: 'edit', channel })}>编辑</Button>
            <Button size="small" onClick={() => setDrawer({ open: true, mode: 'copy', channel })}>复制</Button>
            {channel.status === 'ENABLED' ? (
              <Popconfirm title="确认禁用渠道？" onConfirm={() => void handleDisable(channel.id)}><Button size="small" danger loading={disableMutation.isPending}>禁用</Button></Popconfirm>
            ) : null}
            {needsReenable(channel) ? (
              <Popconfirm
                title="确认重新启用渠道？"
                description="将恢复渠道及其所有限流隔离模型，人工禁用的模型不受影响。"
                onConfirm={() => void handleEnable(channel.id)}
              >
                <Button size="small" loading={enableMutation.isPending}>重新启用</Button>
              </Popconfirm>
            ) : null}
            <Popconfirm
              title="确认删除渠道？"
              description="删除后该渠道不再展示且不会参与路由，历史使用记录不受影响。"
              onConfirm={() => void handleDelete(channel.id)}
            >
              <Button size="small" danger loading={deleteMutation.isPending}>删除</Button>
            </Popconfirm>
          </Space>
        }
      />
    ),
  }];

  return (
    <Card>
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <ProviderChannelToolbar
          search={search}
          onSearchChange={setSearch}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          onCreateClick={() => setDrawer({ open: true, mode: 'create', channel: null })}
          onRefresh={handleRefresh}
          loading={isLoading}
        />
        <Space wrap>
          <Typography.Text type="secondary">已选择 {selectedChannelIds.length} 个渠道</Typography.Text>
          <Popconfirm
            title={`确认重新启用 ${selectedChannels.filter(needsReenable).length} 个渠道？`}
            description="将恢复所选渠道及其所有限流隔离模型，人工禁用的模型不受影响。"
            disabled={!canBatchEnable || batchChanging}
            onConfirm={() => void handleBatchStatus('ENABLED')}
          >
            <Button disabled={!canBatchEnable} loading={batchChanging}>重新启用</Button>
          </Popconfirm>
          <Popconfirm
            title={`确认禁用 ${selectedChannels.filter((channel) => channel.status === 'ENABLED').length} 个渠道？`}
            description="禁用后所选渠道将不再参与路由。"
            disabled={!canBatchDisable || batchChanging}
            onConfirm={() => void handleBatchStatus('DISABLED')}
          >
            <Button danger disabled={!canBatchDisable} loading={batchChanging}>禁用</Button>
          </Popconfirm>
        </Space>
        <Table
          rowKey="id"
          columns={columns}
          dataSource={filtered}
          loading={isLoading}
          rowSelection={{
            selectedRowKeys: selectedChannelIds,
            onChange: setSelectedChannelIds,
            preserveSelectedRowKeys: true,
          }}
          expandable={{ expandedRowRender: (channel) => renderModelsPanel ? renderModelsPanel(channel, handleChannelChanged) : null }}
          pagination={false}
        />
        <ProviderChannelFormDrawer open={drawer.open} mode={drawer.mode} channel={drawer.channel} onClose={() => setDrawer((current) => ({ ...current, open: false }))} onSaved={handleChannelChanged} />
      </Space>
    </Card>
  );
}
