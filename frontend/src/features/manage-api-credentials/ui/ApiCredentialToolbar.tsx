import { Button, Input, Space, Typography } from 'antd';

interface ApiCredentialToolbarProps {
  /** 本地搜索关键词 */
  search: string;
  /** 搜索变化回调 */
  onSearchChange: (value: string) => void;
  /** 创建入口 */
  onCreateClick: () => void;
  /** 刷新回调 */
  onRefresh: () => void;
  /** 是否加载中 */
  loading?: boolean;
}

export function ApiCredentialToolbar({ search, onSearchChange, onCreateClick, onRefresh, loading = false }: ApiCredentialToolbarProps) {
  return (
    <Space style={{ width: '100%', justifyContent: 'space-between' }} wrap>
      <div>
        <Typography.Title level={4} style={{ margin: 0 }}>API Key 管理</Typography.Title>
        <Typography.Text type="secondary">创建、复制、启停和配置 API Key。</Typography.Text>
      </div>
      <Space wrap>
        <Input.Search value={search} onChange={(event): void => onSearchChange(event.target.value)} placeholder="搜索名称或 ID" allowClear style={{ width: 260 }} />
        <Button onClick={onRefresh} loading={loading}>刷新</Button>
        <Button type="primary" onClick={onCreateClick}>创建 API Key</Button>
      </Space>
    </Space>
  );
}
