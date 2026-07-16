import { Typography } from 'antd';
import { parseFieldPath, type FieldPathSegment } from '../model/mappingView';

interface FieldPathTextProps {
  /** 字段路径 */
  value: string;
  /** 是否允许复制 */
  copyable?: boolean;
  /** 显示逐层字段深度 */
  showDepth?: boolean;
}

export function FieldPathText({ value, copyable = true, showDepth = false }: FieldPathTextProps) {
  const segments = parseFieldPath(value);

  if (showDepth && segments.length > 0) {
    return (
      <span className="protocol-field-tree" aria-label={`字段路径：${value}`}>
        <span className="protocol-field-tree__nodes">
          {segments.map((segment, index) => {
            const display = getSegmentDisplay(segment);
            return (
              <span
                className={`protocol-field-tree__node is-${segment.kind}${index === segments.length - 1 ? ' is-leaf' : ''}`}
                key={`${segment.value}-tree-${index}`}
                style={{ '--field-depth': index } as React.CSSProperties}
              >
                <span className="protocol-field-tree__branch" aria-hidden="true" />
                <span className="protocol-field-tree__marker" aria-hidden="true">
                  {getSegmentMarker(segment, index)}
                </span>
                <span className="protocol-field-tree__content">
                  <span className="protocol-field-tree__value">{display.value}</span>
                  {display.hint ? <span className="protocol-field-tree__hint">{display.hint}</span> : null}
                </span>
              </span>
            );
          })}
        </span>
        {copyable ? (
          <Typography.Text
            className="protocol-field-tree__copy"
            type="secondary"
            copyable={{ text: value, tooltips: ['复制原始路径', '已复制'] }}
          >
            复制原始路径
          </Typography.Text>
        ) : null}
      </span>
    );
  }

  return (
    <span className="protocol-field-path-wrap">
      <Typography.Text className="protocol-field-path" code copyable={copyable ? { text: value } : false}>
        {segments.length > 0 ? segments.map((segment, index) => (
          <span key={`${segment.value}-${index}`} className={`protocol-field-path__segment is-${segment.kind}`}>
            {index > 0 && segment.kind === 'property' ? <span className="protocol-field-path__separator">.</span> : null}
            {segment.kind === 'selector' ? `[${segment.value}]` : segment.value}
          </span>
        )) : value}
      </Typography.Text>
    </span>
  );
}

function getSegmentDisplay(segment: FieldPathSegment): { value: string; hint?: string } {
  if (segment.kind === 'array') {
    return { value: '数组中的每一项', hint: '以下字段会在每个元素中读取' };
  }
  if (segment.kind === 'wildcard') {
    return { value: '任意成员', hint: '匹配该层的所有成员' };
  }
  if (segment.kind === 'index') {
    return { value: `数组第 ${segment.value.slice(1, -1)} 项` };
  }
  if (segment.kind === 'selector') {
    const [field, expectedValue] = segment.value.split('=', 2);
    return expectedValue
      ? { value: `筛选字段 ${field}`, hint: `仅匹配值为 ${expectedValue} 的元素` }
      : { value: `筛选 ${segment.value}`, hint: '仅匹配符合该条件的元素' };
  }

  const conditionIndex = segment.value.indexOf('=');
  if (conditionIndex > 0) {
    return {
      value: segment.value.slice(0, conditionIndex),
      hint: `仅当值为 ${segment.value.slice(conditionIndex + 1)}`,
    };
  }
  return { value: segment.value, hint: '对象字段' };
}

function getSegmentMarker(segment: FieldPathSegment, index: number): string | number {
  if (segment.kind === 'array') return '[]';
  if (segment.kind === 'selector') return '?';
  if (segment.kind === 'wildcard') return '*';
  return index + 1;
}
