import { Tag, Tooltip } from 'antd';
import { getProtocolMeta } from '@shared/lib/protocols';

interface ProtocolTagProps {
  /** 协议类型 */
  protocol: string;
  /** 是否紧凑显示 */
  compact?: boolean;
}

export function ProtocolTag({ protocol, compact = false }: ProtocolTagProps) {
  const meta = getProtocolMeta(protocol);
  return (
    <Tooltip title={protocol}>
      <Tag color={meta.color}>{compact ? meta.label.replace('OpenAI ', '') : meta.label}</Tag>
    </Tooltip>
  );
}
