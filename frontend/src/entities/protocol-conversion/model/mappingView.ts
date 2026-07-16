import type { ProtocolConversionFieldMappingResponse, ProtocolConversionMappingResponse } from './types';

export type FieldPathSegmentKind = 'property' | 'array' | 'index' | 'wildcard' | 'selector';

export interface FieldPathSegment {
  value: string;
  kind: FieldPathSegmentKind;
}

export type MappingViewType = 'direct' | 'rename' | 'reshape' | 'default' | 'transform' | 'capability' | 'lossy' | 'unmapped' | 'container' | 'unknown';
export type MappingCoverageStatus = 'mapped' | 'partial' | 'unmapped';

export interface MappingTypeMeta {
  type: MappingViewType;
  label: string;
  color: string;
  lineClassName: string;
}

export interface LossinessMeta {
  value: string;
  label: string;
  color: string;
  isLossy: boolean;
}

export interface MappingViewRow {
  id: string;
  sourceField: string;
  targetField: string;
  ruleDescription: string;
  lossiness: string;
  category?: string;
  mappingType?: string;
  sourceType?: string;
  targetType?: string;
  required?: boolean;
  supported?: boolean;
  defaultValue?: string;
  condition?: string;
  notes?: string;
  sourceSegments: FieldPathSegment[];
  targetSegments: FieldPathSegment[];
  type: MappingViewType;
  typeMeta: MappingTypeMeta;
  lossinessMeta: LossinessMeta;
  coverageStatus: MappingCoverageStatus;
}

export interface MappingViewGroup {
  key: string;
  title: string;
  description: string;
  rows: MappingViewRow[];
  count: number;
  lossyCount: number;
  unmappedCount: number;
  types: MappingViewType[];
}

export interface MappingView {
  groups: MappingViewGroup[];
  totalCount: number;
  lossyCount: number;
  mappedCount: number;
  partialCount: number;
  unmappedCount: number;
}

interface SemanticGroupMeta {
  key: string;
  title: string;
  description: string;
  keywords: string[];
}

const SEMANTIC_GROUPS: SemanticGroupMeta[] = [
  {
    key: 'tools',
    title: '工具调用',
    description: '工具、函数与 tool call 相关字段。',
    keywords: ['tool', 'tools', 'function', 'functions', 'tool_call', 'tool_calls'],
  },
  {
    key: 'reasoning',
    title: '推理信息',
    description: 'Reasoning、thinking 与推理过程相关字段。',
    keywords: ['reasoning', 'thinking', 'thought'],
  },
  {
    key: 'usage',
    title: '用量与缓存',
    description: 'Token 用量、计费统计和缓存 token 相关字段。',
    keywords: ['usage', 'token', 'tokens', 'cache', 'cached'],
  },
  {
    key: 'streaming',
    title: '流式事件',
    description: 'Stream、delta、event 等流式响应字段。',
    keywords: ['stream', 'streaming', 'delta', 'event', 'events', 'chunk'],
  },
  {
    key: 'model',
    title: '模型参数',
    description: '模型名、采样参数、输出长度与停止条件。',
    keywords: ['model', 'temperature', 'top_p', 'top_k', 'max_tokens', 'max_output_tokens', 'stop'],
  },
  {
    key: 'message',
    title: '消息内容',
    description: 'Prompt、message、content、input/output 与 choices 字段。',
    keywords: ['message', 'messages', 'content', 'input', 'output', 'prompt', 'choices', 'choice'],
  },
];

const TYPE_META: Record<MappingViewType, MappingTypeMeta> = {
  direct: { type: 'direct', label: '原值保留', color: 'green', lineClassName: 'is-direct' },
  rename: { type: 'rename', label: '字段改名', color: 'blue', lineClassName: 'is-rename' },
  reshape: { type: 'reshape', label: '结构调整', color: 'purple', lineClassName: 'is-reshape' },
  default: { type: 'default', label: '自动补值', color: 'gold', lineClassName: 'is-default' },
  transform: { type: 'transform', label: '值转换', color: 'geekblue', lineClassName: 'is-transform' },
  capability: { type: 'capability', label: '能力适配', color: 'cyan', lineClassName: 'is-capability' },
  lossy: { type: 'lossy', label: '部分保留', color: 'warning', lineClassName: 'is-lossy' },
  unmapped: { type: 'unmapped', label: '未映射', color: 'error', lineClassName: 'is-unmapped' },
  container: { type: 'container', label: '结构入口', color: 'default', lineClassName: 'is-container' },
  unknown: { type: 'unknown', label: '规则映射', color: 'default', lineClassName: 'is-unknown' },
};

const DIRECT_RULE_KEYWORDS = ['direct', 'as-is', 'pass through', 'passthrough', '透传', '直通', '原样'];
const RENAME_RULE_KEYWORDS = ['rename', 'map to', 'mapped to', '映射为', '重命名'];
const RESHAPE_RULE_KEYWORDS = ['merge', 'split', 'flatten', 'wrap', 'unwrap', '重组', '合并', '拆分', '展开', '转换结构'];
const DEFAULT_RULE_KEYWORDS = ['default', 'fallback', 'if missing', '默认', '缺省', '兜底', '补全'];
const TRANSFORM_RULE_KEYWORDS = ['convert', 'format', 'serialize', 'parse', 'normalize', '转换', '格式化', '归一化', '解析'];
const CAPABILITY_KEYWORDS = ['stream', 'tool', 'function', 'reasoning', 'thinking', 'usage', 'cache', 'token'];

export function parseFieldPath(path: string): FieldPathSegment[] {
  const trimmed = path.trim();
  if (!trimmed || trimmed === '-') {
    return [];
  }

  return trimmed.split('.').flatMap((part) => parsePathPart(part)).filter((segment) => segment.value.length > 0);
}

function parsePathPart(part: string): FieldPathSegment[] {
  const segments: FieldPathSegment[] = [];
  const matcher = /([^\[\]]+)|\[([^\]]*)\]/g;
  let match: RegExpExecArray | null;

  while ((match = matcher.exec(part)) !== null) {
    const isBracketSegment = match[2] !== undefined;
    const raw = match[1] ?? match[2] ?? '';
    const value = raw.trim();
    if (!value) {
      segments.push({ value: '[]', kind: 'array' });
    } else if (value === '*') {
      segments.push({ value: '*', kind: 'wildcard' });
    } else if (/^\d+$/.test(value)) {
      segments.push({ value: `[${value}]`, kind: 'index' });
    } else if (isBracketSegment) {
      segments.push({ value, kind: 'selector' });
    } else {
      segments.push({ value, kind: 'property' });
    }
  }

  return segments.length > 0 ? segments : [{ value: part, kind: part === '*' ? 'wildcard' : 'property' }];
}

export function getMappingTypeMeta(type: MappingViewType): MappingTypeMeta {
  return TYPE_META[type] ?? TYPE_META.unknown;
}

export function getLossinessMeta(lossiness: string): LossinessMeta {
  const value = lossiness || 'UNKNOWN';
  const isLossy = value !== 'NONE';
  return {
    value,
    label: isLossy ? `${value} / 有损` : 'NONE / 无损',
    color: isLossy ? 'warning' : 'success',
    isLossy,
  };
}

export function inferMappingViewType(mapping: ProtocolConversionFieldMappingResponse): MappingViewType {
  const mappingType = mapping.mappingType?.toUpperCase();
  const source = mapping.sourceField.trim();
  const target = mapping.targetField.trim();
  if (isContainerMapping(source, target, mappingType)) {
    return 'container';
  }
  const explicitType = normalizeMappingType(mapping.mappingType);
  if (explicitType) {
    return explicitType;
  }
  const rule = mapping.ruleDescription.toLowerCase();
  const combined = `${source} ${target} ${rule}`.toLowerCase();

  if (getLossinessMeta(mapping.lossiness).isLossy) {
    return 'lossy';
  }
  if (source && target && source === target) {
    return 'direct';
  }
  if (includesAny(rule, DIRECT_RULE_KEYWORDS)) {
    return 'direct';
  }
  if (includesAny(rule, DEFAULT_RULE_KEYWORDS)) {
    return 'default';
  }
  if (includesAny(rule, RESHAPE_RULE_KEYWORDS) || isDepthChangedSignificantly(source, target)) {
    return 'reshape';
  }
  if (includesAny(rule, TRANSFORM_RULE_KEYWORDS)) {
    return 'transform';
  }
  if (includesAny(combined, CAPABILITY_KEYWORDS)) {
    return 'capability';
  }
  if (includesAny(rule, RENAME_RULE_KEYWORDS) || isLikelyRename(source, target)) {
    return 'rename';
  }

  return 'unknown';
}

function isContainerMapping(source: string, target: string, mappingType?: string): boolean {
  if (mappingType?.toUpperCase() !== 'RESHAPE') return false;
  const isSimplePath = (path: string) => Boolean(path) && !/[.[\]/=]/.test(path);
  return isSimplePath(source) && isSimplePath(target);
}

export function getMappingCoverageStatus(mapping: ProtocolConversionFieldMappingResponse): MappingCoverageStatus {
  const target = (mapping.targetPath || mapping.targetField).trim().toLowerCase();
  const mappingType = mapping.mappingType?.toUpperCase();
  if (mapping.supported === false
    || ['DROP', 'UNSUPPORTED', 'UNMAPPED'].includes(mappingType ?? '')
    || ['-', '(dropped)', '(unsupported)', '(unmapped)'].includes(target)) {
    return 'unmapped';
  }
  return getLossinessMeta(mapping.lossiness).isLossy ? 'partial' : 'mapped';
}

export function buildMappingView(mapping: ProtocolConversionMappingResponse): MappingView {
  const groupMap = new Map<string, MappingViewGroup>();

  mapping.fieldMappings.forEach((fieldMapping, index) => {
    const type = inferMappingViewType(fieldMapping);
    const sourceField = fieldMapping.sourcePath || fieldMapping.sourceField;
    const targetField = fieldMapping.targetPath || fieldMapping.targetField;
    const sourceSegments = parseFieldPath(sourceField);
    const targetSegments = parseFieldPath(targetField);
    const row: MappingViewRow = {
      id: `${index}-${sourceField}-${targetField}`,
      sourceField,
      targetField,
      ruleDescription: fieldMapping.ruleDescription,
      lossiness: fieldMapping.lossiness,
      category: fieldMapping.category,
      mappingType: fieldMapping.mappingType,
      sourceType: fieldMapping.sourceType,
      targetType: fieldMapping.targetType,
      required: fieldMapping.required,
      supported: fieldMapping.supported,
      defaultValue: fieldMapping.defaultValue,
      condition: fieldMapping.condition,
      notes: fieldMapping.notes,
      sourceSegments,
      targetSegments,
      type,
      typeMeta: getMappingTypeMeta(type),
      lossinessMeta: getLossinessMeta(fieldMapping.lossiness),
      coverageStatus: getMappingCoverageStatus(fieldMapping),
    };
    const groupMeta = inferSemanticGroup(row);
    const existing = groupMap.get(groupMeta.key);
    const group = existing ?? {
      key: groupMeta.key,
      title: groupMeta.title,
      description: groupMeta.description,
      rows: [],
      count: 0,
      lossyCount: 0,
      unmappedCount: 0,
      types: [],
    };

    group.rows.push(row);
    group.count = group.rows.length;
    group.lossyCount = group.rows.filter((item) => item.lossinessMeta.isLossy).length;
    group.unmappedCount = group.rows.filter((item) => item.coverageStatus === 'unmapped').length;
    group.types = uniqueTypes(group.rows.map((item) => item.type));
    groupMap.set(group.key, group);
  });

  const groups = Array.from(groupMap.values()).sort(sortGroups);
  const rows = groups.flatMap((group) => group.rows);
  return {
    groups,
    totalCount: rows.length,
    lossyCount: rows.filter((item) => item.lossinessMeta.isLossy).length,
    mappedCount: rows.filter((item) => item.coverageStatus === 'mapped').length,
    partialCount: rows.filter((item) => item.coverageStatus === 'partial').length,
    unmappedCount: rows.filter((item) => item.coverageStatus === 'unmapped').length,
  };
}

function inferSemanticGroup(row: MappingViewRow): SemanticGroupMeta {
  const explicit = groupFromCategory(row.category);
  if (explicit) {
    return explicit;
  }
  const combined = `${row.sourceField} ${row.targetField} ${row.ruleDescription}`.toLowerCase();
  const matched = SEMANTIC_GROUPS.find((group) => includesAny(combined, group.keywords));
  if (matched) {
    return matched;
  }

  const firstSegment = [...row.sourceSegments, ...row.targetSegments].find((segment) => segment.kind === 'property');
  if (firstSegment) {
    return {
      key: `field-${firstSegment.value.toLowerCase()}`,
      title: firstSegment.value,
      description: `${firstSegment.value} 相关字段。`,
      keywords: [],
    };
  }

  return { key: 'other', title: '其他字段', description: '未能归入常见协议类型的字段。', keywords: [] };
}

function normalizeMappingType(mappingType?: string): MappingViewType | null {
  switch (mappingType?.toUpperCase()) {
    case 'DIRECT':
      return 'direct';
    case 'RENAME':
      return 'rename';
    case 'RESHAPE':
      return 'reshape';
    case 'DEFAULT':
      return 'default';
    case 'TRANSFORM':
      return 'transform';
    case 'DROP':
    case 'UNSUPPORTED':
    case 'UNMAPPED':
      return 'unmapped';
    case 'LOSSY':
      return 'lossy';
    default:
      return null;
  }
}

function groupFromCategory(category?: string): SemanticGroupMeta | null {
  switch (category?.toUpperCase()) {
    case 'TOOL':
    case 'TOOLS':
      return SEMANTIC_GROUPS[0];
    case 'REASONING':
      return SEMANTIC_GROUPS[1];
    case 'USAGE':
    case 'CACHE':
      return SEMANTIC_GROUPS[2];
    case 'STREAMING':
    case 'STREAM':
      return SEMANTIC_GROUPS[3];
    case 'MODEL':
    case 'SAMPLING':
      return SEMANTIC_GROUPS[4];
    case 'MESSAGE':
    case 'CONTENT':
    case 'CONTENT_BLOCK':
      return SEMANTIC_GROUPS[5];
    case 'METADATA':
      return { key: 'metadata', title: '元数据', description: 'Metadata 与请求上下文相关字段。', keywords: [] };
    case 'OTHER':
      return { key: 'other', title: '其他字段', description: '未能归入常见协议类型的字段。', keywords: [] };
    default:
      return null;
  }
}

function sortGroups(left: MappingViewGroup, right: MappingViewGroup): number {
  if (left.unmappedCount !== right.unmappedCount) {
    return right.unmappedCount - left.unmappedCount;
  }
  const leftIndex = SEMANTIC_GROUPS.findIndex((group) => group.key === left.key);
  const rightIndex = SEMANTIC_GROUPS.findIndex((group) => group.key === right.key);
  const normalizedLeft = leftIndex === -1 ? Number.MAX_SAFE_INTEGER : leftIndex;
  const normalizedRight = rightIndex === -1 ? Number.MAX_SAFE_INTEGER : rightIndex;
  if (normalizedLeft !== normalizedRight) {
    return normalizedLeft - normalizedRight;
  }
  if (left.lossyCount !== right.lossyCount) {
    return right.lossyCount - left.lossyCount;
  }
  return left.title.localeCompare(right.title);
}

function uniqueTypes(types: MappingViewType[]): MappingViewType[] {
  return Array.from(new Set(types));
}

function includesAny(text: string, keywords: string[]): boolean {
  return keywords.some((keyword) => text.includes(keyword));
}

function isDepthChangedSignificantly(source: string, target: string): boolean {
  if (!source || !target) {
    return false;
  }
  return Math.abs(parseFieldPath(source).length - parseFieldPath(target).length) >= 3;
}

function isLikelyRename(source: string, target: string): boolean {
  if (!source || !target || source === target) {
    return false;
  }
  const sourceSegments = parseFieldPath(source).filter((segment) => segment.kind === 'property');
  const targetSegments = parseFieldPath(target).filter((segment) => segment.kind === 'property');
  const sourceLast = sourceSegments.at(-1)?.value;
  const targetLast = targetSegments.at(-1)?.value;
  return Boolean(sourceLast && targetLast && sourceLast !== targetLast && Math.abs(sourceSegments.length - targetSegments.length) <= 1);
}
