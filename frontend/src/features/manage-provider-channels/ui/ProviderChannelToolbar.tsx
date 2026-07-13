import { Button, Input, Select, Space } from 'antd';

export type ProviderChannelStatusFilter = 'ENABLED' | 'DISABLED';

interface ProviderChannelToolbarProps {
  /** 搜索词 */
  search: string;
  /** 搜索变化回调 */
  onSearchChange: (value: string) => void;
  /** 状态筛选 */
  statusFilter?: ProviderChannelStatusFilter;
  /** 状态筛选变化回调 */
  onStatusFilterChange: (value: ProviderChannelStatusFilter | undefined) => void;
  /** 新建渠道回调 */
  onCreateClick: () => void;
  /** 刷新回调 */
  onRefresh: () => void;
  /** 是否加载中 */
  loading?: boolean;
}

export function ProviderChannelToolbar({
  search,
  onSearchChange,
  statusFilter,
  onStatusFilterChange,
  onCreateClick,
  onRefresh,
  loading = false,
}: ProviderChannelToolbarProps) {
  return (
    <Space wrap style={{ width: '100%', justifyContent: 'space-between' }}>
      <Space wrap>
        <Input.Search placeholder="搜索名称 / Host / 协议 / 模型" value={search} onChange={(event) => onSearchChange(event.target.value)} style={{ width: 320 }} allowClear />
        <Select<ProviderChannelStatusFilter>
          allowClear
          aria-label="按渠道状态筛选"
          placeholder="全部状态"
          value={statusFilter}
          onChange={onStatusFilterChange}
          options={[
            { label: '已启用', value: 'ENABLED' },
            { label: '已禁用', value: 'DISABLED' },
          ]}
          style={{ width: 140 }}
        />
      </Space>
      <Space>
        <Button onClick={onRefresh} loading={loading}>刷新</Button>
        <Button type="primary" onClick={onCreateClick}>新建渠道</Button>
      </Space>
    </Space>
  );
}
