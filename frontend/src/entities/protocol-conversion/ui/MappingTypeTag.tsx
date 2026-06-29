import { Tag } from 'antd';
import { getMappingTypeMeta, type MappingViewType } from '../model/mappingView';

interface MappingTypeTagProps {
  /** 映射类型 */
  type: MappingViewType;
}

export function MappingTypeTag({ type }: MappingTypeTagProps) {
  const meta = getMappingTypeMeta(type);
  return <Tag color={meta.color}>{meta.label}</Tag>;
}
