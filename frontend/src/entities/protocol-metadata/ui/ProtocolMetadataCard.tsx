import { Card, Space, Statistic, Tag, Typography } from 'antd';
import { getProtocolMeta } from '@shared/lib/protocols';
import type { ProtocolMetadataListItemResponse } from '../model/types';

interface ProtocolMetadataCardProps {
  protocol: ProtocolMetadataListItemResponse;
  selected?: boolean;
  onClick?: () => void;
}

export function ProtocolMetadataCard({ protocol, selected = false, onClick }: ProtocolMetadataCardProps) {
  const meta = getProtocolMeta(protocol.protocolType);

  return (
    <Card
      hoverable
      size="small"
      onClick={onClick}
      style={{ borderColor: selected ? meta.color : undefined, cursor: onClick ? 'pointer' : undefined }}
    >
      <Space direction="vertical" size={8} style={{ width: '100%' }}>
        <Space wrap style={{ justifyContent: 'space-between', width: '100%' }}>
          <Typography.Text strong>{protocol.displayName}</Typography.Text>
          <Tag color={meta.color}>{protocol.apiSpecVersion}</Tag>
        </Space>
        <Typography.Text type="secondary" ellipsis={{ tooltip: true }}>
          {protocol.description}
        </Typography.Text>
        <Typography.Text code>{protocol.defaultEndpointPath}</Typography.Text>
        <Space wrap size={16}>
          <Statistic title="总字段" value={protocol.fieldCount} valueStyle={{ fontSize: 16 }} />
          <Statistic title="入参" value={protocol.inputFieldCount} valueStyle={{ fontSize: 16 }} />
          <Statistic title="出参" value={protocol.outputFieldCount} valueStyle={{ fontSize: 16 }} />
        </Space>
      </Space>
    </Card>
  );
}
