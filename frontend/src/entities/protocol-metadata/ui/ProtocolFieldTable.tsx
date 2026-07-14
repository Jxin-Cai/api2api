import { Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { ProtocolFieldDefinitionItemResponse } from '../model/types';

interface ProtocolFieldTableProps {
  fields: ProtocolFieldDefinitionItemResponse[];
}

const DIRECTION_META: Record<string, { label: string; color: string }> = {
  INPUT: { label: '入参', color: 'blue' },
  OUTPUT: { label: '出参', color: 'green' },
  BOTH: { label: '双向', color: 'purple' },
};

const TYPE_COLORS: Record<string, string> = {
  STRING: 'cyan',
  INTEGER: 'blue',
  FLOAT: 'geekblue',
  BOOLEAN: 'orange',
  ARRAY: 'purple',
  OBJECT: 'magenta',
  ENUM: 'gold',
};

const columns: ColumnsType<ProtocolFieldDefinitionItemResponse> = [
  {
    title: '字段名',
    dataIndex: 'fieldName',
    width: 160,
    render: (name: string) => <code>{name}</code>,
  },
  {
    title: '路径',
    dataIndex: 'fieldPath',
    width: 200,
    render: (path: string) => <code>{path}</code>,
  },
  {
    title: '类型',
    dataIndex: 'fieldType',
    width: 80,
    render: (type: string) => <Tag color={TYPE_COLORS[type] ?? 'default'}>{type}</Tag>,
  },
  {
    title: '必填',
    dataIndex: 'required',
    width: 60,
    render: (required: boolean) => required ? <Tag color="warning">必填</Tag> : <Tag>可选</Tag>,
  },
  {
    title: '方向',
    dataIndex: 'usageDirection',
    width: 70,
    render: (direction: string) => {
      const meta = DIRECTION_META[direction] ?? { label: direction, color: 'default' };
      return <Tag color={meta.color}>{meta.label}</Tag>;
    },
  },
  {
    title: '说明',
    dataIndex: 'description',
    ellipsis: { showTitle: true },
  },
  {
    title: '用途',
    dataIndex: 'purpose',
    ellipsis: { showTitle: true },
    width: 200,
  },
];

export function ProtocolFieldTable({ fields }: ProtocolFieldTableProps) {
  return (
    <Table
      rowKey="id"
      columns={columns}
      dataSource={fields}
      pagination={false}
      size="small"
      scroll={{ x: 900 }}
    />
  );
}
