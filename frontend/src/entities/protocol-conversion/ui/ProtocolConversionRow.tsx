import type { ReactNode } from 'react';
import { Space, Tag, Typography } from 'antd';
import { formatProtocolDirection } from '@shared/lib/protocols';
import type { ProtocolConversionListItemResponse } from '../model/types';
import { CapabilityTags } from './CapabilityTags';

interface ProtocolConversionRowProps {
  /** 转换列表项 */
  conversion: ProtocolConversionListItemResponse;
  /** 操作区 */
  actions?: ReactNode;
  /** 是否选中 */
  selected?: boolean;
}

export function ProtocolConversionRow({ conversion, actions, selected = false }: ProtocolConversionRowProps) {
  return (
    <Space wrap style={{ width: '100%', justifyContent: 'space-between', background: selected ? '#f0f7ff' : undefined }}>
      <Space direction="vertical" size={2}>
        <Typography.Text strong>
          {formatProtocolDirection(conversion.sourceProtocol, conversion.targetProtocol)}
        </Typography.Text>
        <Space wrap>
          <Tag>{conversion.kind}</Tag>
          <Tag color={conversion.status === 'ENABLED' ? 'success' : 'default'}>{conversion.status}</Tag>
          <Tag color={conversion.implementationStatus === 'IMPLEMENTED' ? 'green' : 'orange'}>
            {conversion.implementationStatus}
          </Tag>
        </Space>
      </Space>
      <CapabilityTags capability={conversion} compact />
      {actions}
    </Space>
  );
}
