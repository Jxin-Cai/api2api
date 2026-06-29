import { Table, Typography, Space, Tag, Skeleton } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { getLossinessMeta } from '../model/mappingView';
import type { ProtocolConversionFieldMappingResponse, ProtocolConversionMappingResponse } from '../model/types';
import { FieldPathText } from './FieldPathText';

interface FieldMappingTableProps {
  /** 字段映射 */
  mapping: ProtocolConversionMappingResponse;
  /** 是否加载中 */
  loading?: boolean;
}

export function FieldMappingTable({ mapping, loading = false }: FieldMappingTableProps) {
  if (loading) {
    return <Skeleton active paragraph={{ rows: 4 }} />;
  }

  const columns: ColumnsType<ProtocolConversionFieldMappingResponse> = [
    {
      title: 'Source',
      dataIndex: 'sourceField',
      render: (value: string) => <FieldPathText value={value} />,
    },
    {
      title: 'Target',
      dataIndex: 'targetField',
      render: (value: string) => <FieldPathText value={value} />,
    },
    { title: '规则', dataIndex: 'ruleDescription' },
    {
      title: '损耗',
      dataIndex: 'lossiness',
      render: (value: string) => {
        const meta = getLossinessMeta(value);
        return <Tag color={meta.color}>{meta.label}</Tag>;
      },
    },
  ];

  return (
    <Space direction="vertical" style={{ width: '100%' }}>
      <div>
        <Typography.Title level={5} style={{ marginBottom: 4 }}>{mapping.title}</Typography.Title>
        <Typography.Text type="secondary">{mapping.summary}</Typography.Text>
      </div>
      <Table
        size="small"
        rowKey={(record) => `${record.sourceField}-${record.targetField}`}
        columns={columns}
        dataSource={mapping.fieldMappings}
        pagination={false}
        scroll={{ x: true }}
      />
    </Space>
  );
}
