import { useMemo, useState } from 'react';
import { Button, Card, Space, Table, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { ProtocolConversionRow, useProtocolConversions, type ProtocolConversionListItemResponse, type ProtocolConversionResponse } from '@entities/protocol-conversion';
import { ProtocolConversionDetailDrawer } from './ProtocolConversionDetailDrawer';
import { ProtocolConversionFilterBar } from './ProtocolConversionFilterBar';
import { useProtocolConversionMutations } from '../model/useProtocolConversionMutations';
import type { ProtocolConversionFilters } from '../model/types';

interface ProtocolConversionTablePanelProps {
  /** 初始筛选 */
  initialFilters?: ProtocolConversionFilters;
}

export function ProtocolConversionTablePanel({ initialFilters = {} }: ProtocolConversionTablePanelProps) {
  const { conversions, isLoading } = useProtocolConversions();
  const { directionMutation } = useProtocolConversionMutations();
  const [filters, setFilters] = useState<ProtocolConversionFilters>(initialFilters);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [directionConversion, setDirectionConversion] = useState<ProtocolConversionResponse | null>(null);

  const protocolOptions = useMemo(() => Array.from(new Set(conversions.flatMap((item) => [item.sourceProtocol, item.targetProtocol]))).map((protocol) => ({ label: protocol, value: protocol })), [conversions]);
  const filtered = useMemo(() => conversions.filter((item) =>
    (!filters.sourceProtocol || item.sourceProtocol === filters.sourceProtocol) &&
    (!filters.targetProtocol || item.targetProtocol === filters.targetProtocol) &&
    (!filters.status || item.status === filters.status) &&
    (!filters.implementationStatus || item.implementationStatus === filters.implementationStatus)
  ), [conversions, filters]);

  async function handleDirectionSearch(sourceProtocol: string, targetProtocol: string): Promise<void> {
    try {
      const response = await directionMutation.mutateAsync({ sourceProtocol, targetProtocol });
      setDirectionConversion(response.data);
      setSelectedId(String(response.data.id));
    } catch {
      message.error('未找到匹配的转换定义');
    }
  }

  const columns: ColumnsType<ProtocolConversionListItemResponse> = [{
    title: '转换定义',
    dataIndex: 'id',
    render: (_value: number, conversion) => (
      <ProtocolConversionRow conversion={conversion} selected={String(conversion.id) === selectedId} actions={<Button size="small" onClick={() => setSelectedId(String(conversion.id))}>查看映射</Button>} />
    ),
  }];

  return (
    <Card>
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <div>
          <Typography.Title level={5} style={{ marginBottom: 4 }}>转换映射</Typography.Title>
          <Typography.Text type="secondary">选择协议方向，查看请求与响应字段的详细映射</Typography.Text>
        </div>
        <ProtocolConversionFilterBar value={filters} protocolOptions={protocolOptions} onChange={setFilters} onReset={() => setFilters({})} onDirectionSearch={(source, target) => void handleDirectionSearch(source, target)} />
        <Table rowKey="id" columns={columns} dataSource={filtered} loading={isLoading} pagination={false} onRow={(record) => ({ onClick: () => setSelectedId(String(record.id)) })} />
        <ProtocolConversionDetailDrawer open={selectedId !== null} definitionId={selectedId} conversion={directionConversion} onClose={() => { setSelectedId(null); setDirectionConversion(null); }} onStatusChanged={(conversion) => setDirectionConversion(conversion)} />
      </Space>
    </Card>
  );
}
