import { Space, Tag, Tooltip } from 'antd';
import { CAPABILITY_META } from '@shared/lib/protocols';
import type { ProtocolConversionCapabilityLike } from '../model/types';

interface CapabilityTagsProps {
  /** 转换能力对象 */
  capability: ProtocolConversionCapabilityLike;
  /** 紧凑模式隐藏 false 项 */
  compact?: boolean;
}

function readCapability(capability: ProtocolConversionCapabilityLike, key: string): boolean {
  return Boolean(capability[key as keyof ProtocolConversionCapabilityLike]);
}

export function CapabilityTags({ capability, compact = false }: CapabilityTagsProps) {
  const contentTypes = 'supportedContentTypes' in capability ? capability.supportedContentTypes ?? [] : [];
  return (
    <Space size={[4, 4]} wrap>
      {CAPABILITY_META.filter((meta) => !compact || readCapability(capability, meta.key)).map((meta) => {
        const enabled = readCapability(capability, meta.key);
        return (
          <Tooltip key={meta.key} title={meta.tooltip}>
            <Tag color={enabled ? meta.color : 'default'}>{meta.label}</Tag>
          </Tooltip>
        );
      })}
      {contentTypes.map((type) => (
        <Tag key={type} color="geekblue">{type}</Tag>
      ))}
    </Space>
  );
}
