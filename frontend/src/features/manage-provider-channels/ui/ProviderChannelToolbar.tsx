import { Button, Input, Space } from 'antd';

interface ProviderChannelToolbarProps {
  /** 搜索词 */
  search: string;
  /** 搜索变化回调 */
  onSearchChange: (value: string) => void;
  /** 新建渠道回调 */
  onCreateClick: () => void;
  /** 刷新回调 */
  onRefresh: () => void;
  /** 是否加载中 */
  loading?: boolean;
}

export function ProviderChannelToolbar({ search, onSearchChange, onCreateClick, onRefresh, loading = false }: ProviderChannelToolbarProps) {
  return (
    <Space wrap style={{ width: '100%', justifyContent: 'space-between' }}>
      <Input.Search placeholder="搜索 name / host / protocol" value={search} onChange={(event) => onSearchChange(event.target.value)} style={{ width: 320 }} allowClear />
      <Space>
        <Button onClick={onRefresh} loading={loading}>刷新</Button>
        <Button type="primary" onClick={onCreateClick}>新建渠道</Button>
      </Space>
    </Space>
  );
}
