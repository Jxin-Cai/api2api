import { Typography } from 'antd';
import { parseFieldPath } from '../model/mappingView';

interface FieldPathTextProps {
  /** 字段路径 */
  value: string;
  /** 是否允许复制 */
  copyable?: boolean;
}

export function FieldPathText({ value, copyable = true }: FieldPathTextProps) {
  const segments = parseFieldPath(value);

  return (
    <Typography.Text className="protocol-field-path" code copyable={copyable ? { text: value } : false}>
      {segments.length > 0 ? segments.map((segment, index) => (
        <span key={`${segment.value}-${index}`} className={`protocol-field-path__segment is-${segment.kind}`}>
          {index > 0 && segment.kind === 'property' ? <span className="protocol-field-path__separator">.</span> : null}
          {segment.value}
        </span>
      )) : value}
    </Typography.Text>
  );
}
