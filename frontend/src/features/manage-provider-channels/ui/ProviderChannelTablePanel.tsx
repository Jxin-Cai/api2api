import { useMemo, useState, type ReactNode } from 'react';
import { Button, Card, Popconfirm, Space, Table } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { ProviderChannelRow, useProviderChannels, type ProviderChannelResponse } from '@entities/provider-channel';
import { ProviderChannelFormDrawer } from './ProviderChannelFormDrawer';
import { ProviderChannelToolbar } from './ProviderChannelToolbar';
import { useProviderChannelMutations } from '../model/useProviderChannelMutations';

interface ProviderChannelTablePanelProps {
  /** 模型展开区渲染函数 */
  renderModelsPanel?: (channel: ProviderChannelResponse, onChanged: (channel: ProviderChannelResponse) => void) => ReactNode;
}

export function ProviderChannelTablePanel({ renderModelsPanel }: ProviderChannelTablePanelProps) {
  const { channels, isLoading, refetch } = useProviderChannels();
  const { enableMutation, disableMutation } = useProviderChannelMutations();
  const [search, setSearch] = useState('');
  const [drawer, setDrawer] = useState<{ open: boolean; mode: 'create' | 'edit'; channel: ProviderChannelResponse | null }>({ open: false, mode: 'create', channel: null });
  const [localChannels, setLocalChannels] = useState<ProviderChannelResponse[]>([]);
  const source = localChannels.length > 0 ? localChannels : channels;

  const filtered = useMemo(() => source.filter((channel) => {
    const text = `${channel.name} ${channel.host} ${channel.supportedProtocols.join(' ')}`.toLowerCase();
    return text.includes(search.toLowerCase());
  }), [source, search]);

  function handleChannelChanged(channel: ProviderChannelResponse): void {
    setLocalChannels((current) => {
      const next = current.length > 0 ? current : channels;
      const exists = next.some((item) => item.id === channel.id);
      return exists ? next.map((item) => (item.id === channel.id ? channel : item)) : [channel, ...next];
    });
    void refetch();
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
            {channel.status === 'ENABLED' ? (
              <Popconfirm title="确认禁用渠道？" onConfirm={() => disableMutation.mutate(channel.id)}><Button size="small" danger>禁用</Button></Popconfirm>
            ) : (
              <Popconfirm title="确认启用渠道？" onConfirm={() => enableMutation.mutate(channel.id)}><Button size="small">启用</Button></Popconfirm>
            )}
          </Space>
        }
      />
    ),
  }];

  return (
    <Card>
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <ProviderChannelToolbar search={search} onSearchChange={setSearch} onCreateClick={() => setDrawer({ open: true, mode: 'create', channel: null })} onRefresh={() => void refetch()} loading={isLoading} />
        <Table rowKey="id" columns={columns} dataSource={filtered} loading={isLoading} expandable={{ expandedRowRender: (channel) => renderModelsPanel ? renderModelsPanel(channel, handleChannelChanged) : null }} pagination={false} />
        <ProviderChannelFormDrawer open={drawer.open} mode={drawer.mode} channel={drawer.channel} onClose={() => setDrawer((current) => ({ ...current, open: false }))} onSaved={handleChannelChanged} />
      </Space>
    </Card>
  );
}
