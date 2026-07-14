import { PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons';
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
    <div className="api-credential-toolbar">
      <div className="api-credential-toolbar__copy">
        <Typography.Title level={4} className="api-credential-toolbar__title">凭证</Typography.Title>
        <Typography.Text type="secondary">管理密钥权限、限额与启用状态</Typography.Text>
      </div>
      <Space wrap className="api-credential-toolbar__actions">
        <Input
          aria-label="搜索 API Key"
          value={search}
          onChange={(event): void => onSearchChange(event.target.value)}
          placeholder="搜索名称或 ID"
          allowClear
          prefix={<SearchOutlined aria-hidden="true" />}
          className="api-credential-toolbar__search"
        />
        <Button aria-label="刷新 API Key 列表" icon={<ReloadOutlined />} onClick={onRefresh} loading={loading} />
        <Button type="primary" icon={<PlusOutlined />} onClick={onCreateClick}>新建密钥</Button>
      </Space>
    </div>
  );
}
