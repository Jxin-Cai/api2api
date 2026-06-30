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
      title: '分类',
      dataIndex: 'category',
      width: 120,
      render: (value: string | undefined) => value ? <Tag>{value}</Tag> : '-',
    },
    {
      title: '类型',
      dataIndex: 'mappingType',
      width: 120,
      render: (value: string | undefined) => value ? <Tag color="blue">{value}</Tag> : '-',
    },
    {
      title: 'Source',
      dataIndex: 'sourceField',
      render: (_value: string, record) => <FieldPathText value={record.sourcePath || record.sourceField} />,
    },
    {
      title: 'Target',
      dataIndex: 'targetField',
      render: (_value: string, record) => <FieldPathText value={record.targetPath || record.targetField} />,
    },
    {
      title: '参数',
      key: 'parameter',
      render: (_value, record) => (
        <Space direction="vertical" size={2}>
          {record.sourceType || record.targetType ? <Typography.Text type="secondary">{record.sourceType ?? '-'} → {record.targetType ?? '-'}</Typography.Text> : null}
          {record.required !== undefined ? <Tag color={record.required ? 'warning' : 'default'}>{record.required ? '必填' : '可选'}</Tag> : null}
          {record.supported !== undefined ? <Tag color={record.supported ? 'success' : 'error'}>{record.supported ? '支持' : '不支持'}</Tag> : null}
          {record.defaultValue ? <Typography.Text type="secondary">默认：{record.defaultValue}</Typography.Text> : null}
        </Space>
      ),
    },
    { title: '规则', dataIndex: 'ruleDescription' },
    {
      title: '条件/说明',
      key: 'notes',
      render: (_value, record) => (
        <Space direction="vertical" size={2}>
          {record.condition ? <Typography.Text type="secondary">条件：{record.condition}</Typography.Text> : null}
          {record.notes ? <Typography.Text type="secondary">{record.notes}</Typography.Text> : null}
        </Space>
      ),
    },
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
