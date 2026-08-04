import { Button, Input, Popconfirm, Select, Space } from 'antd';

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
  /** 重置全部限流状态回调 */
  onResetAllRateLimits: () => void;
  /** 当前包含限流模型的渠道数量 */
  rateLimitedChannelCount: number;
  /** 是否正在重置限流状态 */
  resettingRateLimits?: boolean;
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
  onResetAllRateLimits,
  rateLimitedChannelCount,
  resettingRateLimits = false,
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
        <Popconfirm
          title={`确认重置全部 ${rateLimitedChannelCount} 个限流渠道？`}
          description="所有渠道中被限流隔离的模型将立即恢复路由，人工禁用的渠道和模型不受影响。"
          disabled={rateLimitedChannelCount === 0 || resettingRateLimits}
          onConfirm={onResetAllRateLimits}
        >
          <Button disabled={rateLimitedChannelCount === 0} loading={resettingRateLimits}>
            重置全部限流{rateLimitedChannelCount > 0 ? `（${rateLimitedChannelCount}）` : ''}
          </Button>
        </Popconfirm>
        <Button onClick={onRefresh} loading={loading}>刷新</Button>
        <Button type="primary" onClick={onCreateClick}>新建渠道</Button>
      </Space>
    </Space>
  );
}
