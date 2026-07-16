import { Segmented, Space, Tabs } from 'antd';
import { useState } from 'react';
import { FieldMappingHierarchy, FieldMappingTable, type ProtocolConversionMappingResponse, type ProtocolConversionResponse } from '@entities/protocol-conversion';
import type { ProtocolConversionActiveTab } from '@features/view-protocol-conversions';

type MappingViewMode = 'hierarchy' | 'table';

interface ProtocolMappingPanelProps {
  /** 转换详情 */
  conversion: ProtocolConversionResponse;
  /** 激活 Tab */
  activeTab: ProtocolConversionActiveTab;
  /** Tab 变化回调 */
  onTabChange: (tab: ProtocolConversionActiveTab) => void;
}

export function ProtocolMappingPanel({ conversion, activeTab, onTabChange }: ProtocolMappingPanelProps) {
  const [viewMode, setViewMode] = useState<MappingViewMode>('hierarchy');

  return (
    <Space direction="vertical" size={12} style={{ width: '100%' }}>
      <Segmented
        value={viewMode}
        onChange={(value) => setViewMode(value as MappingViewMode)}
        options={[
          { label: '图解视图', value: 'hierarchy' },
          { label: '紧凑表格', value: 'table' },
        ]}
      />
      <Tabs
        activeKey={activeTab}
        onChange={(key) => onTabChange(key as ProtocolConversionActiveTab)}
        items={[
          {
            key: 'request',
            label: buildTabLabel('请求转换', conversion.requestMapping),
            children: renderMapping(conversion.requestMapping, viewMode),
          },
          {
            key: 'response',
            label: buildTabLabel('响应转换', conversion.responseMapping),
            children: renderMapping(conversion.responseMapping, viewMode),
          },
        ]}
      />
    </Space>
  );
}

function buildTabLabel(label: string, mapping: ProtocolConversionMappingResponse): string {
  return `${label} (${mapping.fieldMappings.length})`;
}

function renderMapping(mapping: ProtocolConversionMappingResponse, viewMode: MappingViewMode) {
  if (viewMode === 'table') {
    return <FieldMappingTable mapping={mapping} />;
  }
  return <FieldMappingHierarchy mapping={mapping} />;
}
