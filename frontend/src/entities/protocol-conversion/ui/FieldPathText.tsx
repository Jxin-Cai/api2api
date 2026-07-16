import { Typography } from 'antd';
import { parseFieldPath } from '../model/mappingView';

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

  return (
    <span className="protocol-field-path-wrap">
      <Typography.Text className="protocol-field-path" code copyable={copyable ? { text: value } : false}>
        {segments.length > 0 ? segments.map((segment, index) => (
          <span key={`${segment.value}-${index}`} className={`protocol-field-path__segment is-${segment.kind}`}>
            {index > 0 && segment.kind === 'property' ? <span className="protocol-field-path__separator">.</span> : null}
            {segment.value}
          </span>
        )) : value}
      </Typography.Text>
      {showDepth && segments.length > 0 ? (
        <span className="protocol-field-depth" aria-label={`字段路径共 ${segments.length} 层`}>
          {segments.map((segment, index) => (
            <span className="protocol-field-depth__level" key={`${segment.value}-depth-${index}`} style={{ marginLeft: index * 10 }}>
              <span className="protocol-field-depth__index">L{index + 1}</span>
              <span className={`protocol-field-depth__value is-${segment.kind}`}>{segment.value}</span>
            </span>
          ))}
        </span>
      ) : null}
    </span>
  );
}
