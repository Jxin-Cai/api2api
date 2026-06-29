import { Button, Select, Space, message } from 'antd';
import type { SelectOption } from '@shared/types/admin';
import type { ProtocolConversionFilters } from '../model/types';

interface ProtocolConversionFilterBarProps {
  /** 筛选值 */
  value: ProtocolConversionFilters;
  /** 协议选项 */
  protocolOptions?: SelectOption[];
  /** 筛选变化 */
  onChange: (value: ProtocolConversionFilters) => void;
  /** 方向查询 */
  onDirectionSearch: (sourceProtocol: string, targetProtocol: string) => void;
  /** 重置筛选 */
  onReset: () => void;
}

export function ProtocolConversionFilterBar({ value, protocolOptions = [], onChange, onDirectionSearch, onReset }: ProtocolConversionFilterBarProps) {
  function handleDirectionSearch(): void {
    if (!value.sourceProtocol || !value.targetProtocol) {
      message.warning('请选择源协议和目标协议');
      return;
    }
    onDirectionSearch(value.sourceProtocol, value.targetProtocol);
  }

  return (
    <Space wrap>
      <Select allowClear placeholder="源协议" value={value.sourceProtocol} onChange={(sourceProtocol) => onChange({ ...value, sourceProtocol })} options={protocolOptions} style={{ width: 180 }} />
      <Select allowClear placeholder="目标协议" value={value.targetProtocol} onChange={(targetProtocol) => onChange({ ...value, targetProtocol })} options={protocolOptions} style={{ width: 180 }} />
      <Select allowClear placeholder="状态" value={value.status} onChange={(status) => onChange({ ...value, status })} options={[{ label: 'ENABLED', value: 'ENABLED' }, { label: 'DISABLED', value: 'DISABLED' }]} style={{ width: 150 }} />
      <Select allowClear placeholder="实现状态" value={value.implementationStatus} onChange={(implementationStatus) => onChange({ ...value, implementationStatus })} options={[{ label: 'IMPLEMENTED', value: 'IMPLEMENTED' }, { label: 'PARTIAL', value: 'PARTIAL' }, { label: 'NOT_IMPLEMENTED', value: 'NOT_IMPLEMENTED' }]} style={{ width: 180 }} />
      <Button type="primary" onClick={handleDirectionSearch}>按方向查询</Button>
      <Button onClick={onReset}>重置</Button>
    </Space>
  );
}
