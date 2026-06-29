import { Collapse, Empty, Space, Tag, Typography } from 'antd';
import { useMemo } from 'react';
import { buildMappingView, getMappingTypeMeta, type MappingViewGroup, type MappingViewRow } from '../model/mappingView';
import type { ProtocolConversionMappingResponse } from '../model/types';
import { FieldPathText } from './FieldPathText';
import { MappingTypeTag } from './MappingTypeTag';
import './FieldMappingHierarchy.css';

interface FieldMappingHierarchyProps {
  /** 字段映射 */
  mapping: ProtocolConversionMappingResponse;
}

export function FieldMappingHierarchy({ mapping }: FieldMappingHierarchyProps) {
  const view = useMemo(() => buildMappingView(mapping), [mapping]);
  const defaultActiveKeys = useMemo(() => view.groups
    .filter((group, index) => index < 2 || group.lossyCount > 0)
    .map((group) => group.key), [view.groups]);

  if (view.totalCount === 0) {
    return <Empty description="暂无字段映射" />;
  }

  return (
    <Space className="protocol-mapping-hierarchy" direction="vertical" size={12}>
      <div className="protocol-mapping-hierarchy__header">
        <Typography.Title level={5} style={{ margin: 0 }}>{mapping.title}</Typography.Title>
        <Typography.Text type="secondary">{mapping.summary}</Typography.Text>
        <div className="protocol-mapping-hierarchy__summary">
          <Tag>{mapping.direction}</Tag>
          <Tag>{view.totalCount} 个字段</Tag>
          <Tag color={view.lossyCount > 0 ? 'warning' : 'success'}>{view.lossyCount} 个有损</Tag>
          <Tag>{view.groups.length} 个分组</Tag>
        </div>
      </div>
      <Collapse
        defaultActiveKey={defaultActiveKeys}
        items={view.groups.map((group) => ({
          key: group.key,
          label: <GroupLabel group={group} />,
          children: <GroupRows rows={group.rows} />,
        }))}
      />
    </Space>
  );
}

function GroupLabel({ group }: { group: MappingViewGroup }) {
  return (
    <div className="protocol-mapping-hierarchy__group-label">
      <div className="protocol-mapping-hierarchy__group-title">
        <Typography.Text strong>{group.title}</Typography.Text>
        <Typography.Text type="secondary">{group.description}</Typography.Text>
      </div>
      <Space size={[4, 4]} wrap>
        <Tag>{group.count} 条</Tag>
        {group.lossyCount > 0 ? <Tag color="warning">{group.lossyCount} 条有损</Tag> : <Tag color="success">无损</Tag>}
        {group.types.slice(0, 3).map((type) => {
          const meta = getMappingTypeMeta(type);
          return <Tag key={type} color={meta.color}>{meta.label}</Tag>;
        })}
      </Space>
    </div>
  );
}

function GroupRows({ rows }: { rows: MappingViewRow[] }) {
  return (
    <div className="protocol-mapping-hierarchy__rows">
      {rows.map((row) => <MappingRow key={row.id} row={row} />)}
    </div>
  );
}

function MappingRow({ row }: { row: MappingViewRow }) {
  return (
    <div className="protocol-mapping-row">
      <div className="protocol-mapping-row__endpoint">
        <span className="protocol-mapping-row__label">Source</span>
        <FieldPathText value={row.sourceField} />
      </div>
      <div className="protocol-mapping-row__rule" aria-label={`字段 ${row.sourceField} 映射到 ${row.targetField}，规则：${row.ruleDescription}`}>
        <span className={`protocol-mapping-row__line ${row.typeMeta.lineClassName}`} aria-hidden="true" />
        <div className="protocol-mapping-row__tags">
          <MappingTypeTag type={row.type} />
          <Tag color={row.lossinessMeta.color}>{row.lossinessMeta.label}</Tag>
        </div>
        <Typography.Text className="protocol-mapping-row__description">{row.ruleDescription}</Typography.Text>
      </div>
      <div className="protocol-mapping-row__endpoint">
        <span className="protocol-mapping-row__label">Target</span>
        <FieldPathText value={row.targetField} />
      </div>
    </div>
  );
}
