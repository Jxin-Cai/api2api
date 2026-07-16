import { Collapse, Empty, Input, Segmented, Space, Tag, Typography } from 'antd';
import { useEffect, useMemo, useState } from 'react';
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
  const [query, setQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<'all' | 'supported' | 'lossy' | 'unsupported'>('all');
  const filteredGroups = useMemo(() => view.groups.map((group) => ({
    ...group,
    rows: group.rows.filter((row) => matchesRow(row, query, statusFilter)),
  })).filter((group) => group.rows.length > 0).map((group) => ({
    ...group,
    count: group.rows.length,
    lossyCount: group.rows.filter((row) => row.lossinessMeta.isLossy).length,
    types: Array.from(new Set(group.rows.map((row) => row.type))),
  })), [query, statusFilter, view.groups]);
  const defaultActiveKeys = useMemo(() => view.groups
    .filter((group, index) => index < 2 || group.lossyCount > 0)
    .map((group) => group.key), [view.groups]);
  const [activeKeys, setActiveKeys] = useState<string[]>(defaultActiveKeys);

  useEffect(() => setActiveKeys(defaultActiveKeys), [defaultActiveKeys]);

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
      <div className="protocol-mapping-hierarchy__actions">
        <Input.Search
          allowClear
          aria-label="搜索字段路径或转换规则"
          placeholder="搜索字段路径、规则或备注"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          style={{ maxWidth: 360 }}
        />
        <Segmented
          aria-label="筛选转换状态"
          value={statusFilter}
          onChange={(value) => setStatusFilter(value as typeof statusFilter)}
          options={[
            { label: '全部', value: 'all' },
            { label: '支持', value: 'supported' },
            { label: '有损', value: 'lossy' },
            { label: '不支持', value: 'unsupported' },
          ]}
        />
        <span className="protocol-mapping-hierarchy__expand-actions">
          <Typography.Link onClick={() => setActiveKeys(filteredGroups.map((group) => group.key))}>展开全部</Typography.Link>
          <Typography.Text type="secondary"> / </Typography.Text>
          <Typography.Link onClick={() => setActiveKeys([])}>收起全部</Typography.Link>
        </span>
      </div>
      {filteredGroups.length > 0 ? <Collapse
        activeKey={activeKeys}
        onChange={(keys) => setActiveKeys(Array.isArray(keys) ? keys.map(String) : [String(keys)])}
        items={filteredGroups.map((group) => ({
          key: group.key,
          label: <GroupLabel group={group} />,
          children: <GroupRows rows={group.rows} />,
        }))}
      /> : <Empty description="没有符合筛选条件的字段映射" />}
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
        <FieldPathText value={row.sourceField} showDepth />
      </div>
      <div className="protocol-mapping-row__rule" aria-label={`字段 ${row.sourceField} 映射到 ${row.targetField}，规则：${row.ruleDescription}`}>
        <span className={`protocol-mapping-row__line ${row.typeMeta.lineClassName}`} aria-hidden="true" />
        <div className="protocol-mapping-row__tags">
          {row.category ? <Tag>{row.category}</Tag> : null}
          <MappingTypeTag type={row.type} />
          {row.mappingType ? <Tag color="blue">{row.mappingType}</Tag> : null}
          <Tag color={row.lossinessMeta.color}>{row.lossinessMeta.label}</Tag>
          {row.required !== undefined ? <Tag color={row.required ? 'warning' : 'default'}>{row.required ? '必填' : '可选'}</Tag> : null}
          {row.supported !== undefined ? <Tag color={row.supported ? 'success' : 'error'}>{row.supported ? '支持' : '不支持'}</Tag> : null}
        </div>
        <Typography.Text className="protocol-mapping-row__description">{row.ruleDescription}</Typography.Text>
        {row.sourceType || row.targetType || row.defaultValue || row.condition || row.notes ? (
          <Typography.Text type="secondary" className="protocol-mapping-row__description">
            {[row.sourceType || row.targetType ? `类型：${row.sourceType ?? '-'} → ${row.targetType ?? '-'}` : null,
              row.defaultValue ? `默认：${row.defaultValue}` : null,
              row.condition ? `条件：${row.condition}` : null,
              row.notes].filter(Boolean).join('；')}
          </Typography.Text>
        ) : null}
      </div>
      <div className="protocol-mapping-row__endpoint">
        <span className="protocol-mapping-row__label">Target</span>
        <FieldPathText value={row.targetField} showDepth />
      </div>
    </div>
  );
}

function matchesRow(row: MappingViewRow, query: string, status: 'all' | 'supported' | 'lossy' | 'unsupported'): boolean {
  const normalizedQuery = query.trim().toLowerCase();
  const textMatches = !normalizedQuery || [row.sourceField, row.targetField, row.ruleDescription, row.notes]
    .filter(Boolean).some((value) => value?.toLowerCase().includes(normalizedQuery));
  if (!textMatches) return false;
  if (status === 'unsupported') return row.supported === false;
  if (status === 'lossy') return row.lossinessMeta.isLossy;
  if (status === 'supported') return row.supported !== false;
  return true;
}
