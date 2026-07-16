import { CheckCircleOutlined, ExclamationCircleOutlined, InfoCircleOutlined, StopOutlined } from '@ant-design/icons';
import { Collapse, Empty, Input, Segmented, Space, Tag, Typography } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { buildMappingView, type MappingViewGroup, type MappingViewRow } from '../model/mappingView';
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
  const isRequestMapping = mapping.direction.toUpperCase() === 'REQUEST';
  const [query, setQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<MappingStatusFilter>('all');
  const filteredGroups = useMemo(() => view.groups.map((group) => ({
    ...group,
    rows: group.rows.filter((row) => matchesRow(row, query, statusFilter)),
  })).filter((group) => group.rows.length > 0).map((group) => ({
    ...group,
    count: group.rows.length,
    lossyCount: group.rows.filter((row) => row.lossinessMeta.isLossy).length,
    unmappedCount: group.rows.filter((row) => row.coverageStatus === 'unmapped').length,
    types: Array.from(new Set(group.rows.map((row) => row.type))),
  })), [query, statusFilter, view.groups]);
  const defaultActiveKeys = useMemo(() => view.groups
    .filter((group, index) => index < 2 || group.unmappedCount > 0)
    .map((group) => group.key), [view.groups]);
  const [activeKeys, setActiveKeys] = useState<string[]>(defaultActiveKeys);

  useEffect(() => setActiveKeys(defaultActiveKeys), [defaultActiveKeys]);

  if (view.totalCount === 0) {
    return <Empty description="暂无字段映射" />;
  }

  return (
    <Space className="protocol-mapping-hierarchy" direction="vertical" size={12}>
      <div className="protocol-mapping-hierarchy__header">
        <Typography.Title level={5} style={{ margin: 0 }}>{isRequestMapping ? '请求字段如何转换' : '响应字段如何转换'}</Typography.Title>
        <Typography.Text type="secondary">{mapping.summary}</Typography.Text>
        <Tag className="protocol-mapping-hierarchy__direction">{isRequestMapping ? '请求发送给目标服务之前' : '目标服务返回响应之后'}</Tag>
        <div className="protocol-mapping-hierarchy__summary" aria-label="字段映射覆盖情况">
          <SummaryMetric icon={<CheckCircleOutlined />} label="完整映射" hint="字段和值都能转换" value={view.mappedCount} tone="success" />
          <SummaryMetric icon={<ExclamationCircleOutlined />} label="部分映射" hint="转换时会损失部分信息" value={view.partialCount} tone="warning" />
          <SummaryMetric icon={<StopOutlined />} label="未映射" hint="目标协议没有对应字段" value={view.unmappedCount} tone="error" />
          <SummaryMetric label="来源字段" hint="本页列出的字段总数" value={view.totalCount} tone="neutral" />
        </div>
      </div>
      <div className="protocol-mapping-read-guide">
        <div className="protocol-mapping-read-guide__title"><InfoCircleOutlined /> 如何阅读字段映射</div>
        <div className="protocol-mapping-read-guide__steps">
          <span><b>1</b> 找到输入字段</span>
          <span><b>2</b> 查看网关如何处理</span>
          <span><b>3</b> 找到输出字段</span>
        </div>
        <Typography.Text type="secondary">
          遇到数组时会单独显示“数组中的每一项”，表示后面的字段要在数组的每个元素里查找。
        </Typography.Text>
      </div>
      <div className="protocol-mapping-hierarchy__actions">
        <Input.Search
          allowClear
          aria-label="搜索字段路径或转换规则"
          placeholder="输入字段名，例如 max_tokens"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          style={{ maxWidth: 360 }}
        />
        <div className="protocol-mapping-hierarchy__filter">
          <span>按结果查看</span>
          <Segmented
            aria-label="按映射结果筛选"
            value={statusFilter}
            onChange={(value) => setStatusFilter(value as typeof statusFilter)}
            options={[
              { label: `全部 ${view.totalCount}`, value: 'all' },
              { label: `完整 ${view.mappedCount}`, value: 'mapped' },
              { label: `部分 ${view.partialCount}`, value: 'partial' },
              { label: `未映射 ${view.unmappedCount}`, value: 'unmapped' },
            ]}
          />
        </div>
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
        {group.unmappedCount > 0 ? <Tag color="error">{group.unmappedCount} 个未映射</Tag> : null}
        {group.lossyCount > group.unmappedCount ? <Tag color="warning">{group.lossyCount - group.unmappedCount} 个部分映射</Tag> : null}
        {group.lossyCount === 0 ? <Tag color="success">全部可转换</Tag> : null}
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
  const isUnmapped = row.coverageStatus === 'unmapped';
  return (
    <div className={`protocol-mapping-row is-${row.coverageStatus}`}>
      <div className="protocol-mapping-row__endpoint">
        <span className="protocol-mapping-row__label"><b>1</b> 输入协议 · 来源字段</span>
        <FieldPathText value={row.sourceField} showDepth />
      </div>
      <div className="protocol-mapping-row__rule" aria-label={isUnmapped
        ? `字段 ${row.sourceField} 未映射，原因：${row.ruleDescription}`
        : `字段 ${row.sourceField} 映射到 ${row.targetField}，规则：${row.ruleDescription}`}>
        <span className="protocol-mapping-row__label"><b>2</b> 网关处理</span>
        <span className={`protocol-mapping-row__line ${row.typeMeta.lineClassName}`} aria-hidden="true" />
        <div className="protocol-mapping-row__tags">
          <MappingTypeTag type={row.type} />
          {row.coverageStatus === 'partial' ? <Tag color="warning">部分信息无法保留</Tag> : null}
          {row.required !== undefined ? <Tag color={row.required ? 'warning' : 'default'}>{row.required ? '来源必填' : '来源可选'}</Tag> : null}
        </div>
        <span className="protocol-mapping-row__explanation-label">{isUnmapped ? '未映射原因' : '转换说明'}</span>
        <Typography.Text className="protocol-mapping-row__description">{row.ruleDescription}</Typography.Text>
        {row.type === 'container' ? <Typography.Text type="secondary" className="protocol-mapping-row__container-hint">这是外层结构入口；同组下方会继续列出具体字段。</Typography.Text> : null}
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
        <span className="protocol-mapping-row__label"><b>3</b> 输出协议 · 结果字段</span>
        {isUnmapped ? (
          <div className="protocol-mapping-row__unmapped-target">
            <StopOutlined aria-hidden="true" />
            <span>无对应字段</span>
          </div>
        ) : <FieldPathText value={row.targetField} showDepth />}
      </div>
    </div>
  );
}

type MappingStatusFilter = 'all' | 'mapped' | 'partial' | 'unmapped';

function SummaryMetric({ icon, label, hint, value, tone }: { icon?: React.ReactNode; label: string; hint: string; value: number; tone: 'success' | 'warning' | 'error' | 'neutral' }) {
  return (
    <div className={`protocol-mapping-summary-card is-${tone}`}>
      <span>
        <span className="protocol-mapping-summary-card__label">{icon}{label}</span>
        <span className="protocol-mapping-summary-card__hint">{hint}</span>
      </span>
      <strong>{value}</strong>
    </div>
  );
}

function matchesRow(row: MappingViewRow, query: string, status: MappingStatusFilter): boolean {
  const normalizedQuery = query.trim().toLowerCase();
  const textMatches = !normalizedQuery || [row.sourceField, row.targetField, row.ruleDescription, row.notes]
    .filter(Boolean).some((value) => value?.toLowerCase().includes(normalizedQuery));
  if (!textMatches) return false;
  if (status !== 'all') return row.coverageStatus === status;
  return true;
}
