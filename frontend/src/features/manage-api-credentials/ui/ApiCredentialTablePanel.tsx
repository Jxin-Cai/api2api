import { Button, Card, Empty, Popconfirm, Space, Table, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMemo, useState, type ReactElement } from 'react';

import { ApiCredentialStatusTag, useApiCredentials, type ApiCredentialResponse, type CreateApiCredentialResponse } from '@entities/api-credential';
import { PageState } from '@shared/ui';

import { useApiCredentialMutations } from '../model/useApiCredentialMutations';
import { ApiCredentialCreateModal } from './ApiCredentialCreateModal';
import { ApiCredentialEditDrawer } from './ApiCredentialEditDrawer';
import { ApiCredentialToolbar } from './ApiCredentialToolbar';

interface ApiCredentialTablePanelProps {
  /** 创建或编辑可选模型 */
  modelOptions?: Array<{ label: string; value: string }>;
}

function isEnabled(credential: ApiCredentialResponse): boolean {
  return String(credential.status).toUpperCase() === 'ENABLED';
}

export function ApiCredentialTablePanel({ modelOptions = [] }: ApiCredentialTablePanelProps) {
  const { credentials, query } = useApiCredentials();
  const { enableMutation, disableMutation } = useApiCredentialMutations();
  const [search, setSearch] = useState('');
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<ApiCredentialResponse | null>(null);

  const filteredCredentials = useMemo((): ApiCredentialResponse[] => {
    const keyword = search.trim().toLowerCase();
    if (!keyword) {
      return credentials;
    }
    return credentials.filter((credential: ApiCredentialResponse): boolean =>
      credential.name.toLowerCase().includes(keyword) || credential.id.toLowerCase().includes(keyword)
    );
  }, [credentials, search]);

  async function handleToggleStatus(credential: ApiCredentialResponse): Promise<void> {
    if (isEnabled(credential)) {
      await disableMutation.mutateAsync(credential.id);
      message.success('API Key 已禁用');
      return;
    }
    await enableMutation.mutateAsync(credential.id);
    message.success('API Key 已启用');
  }

  function handleCreated(_: CreateApiCredentialResponse): void {
    query.refetch().catch((): void => undefined);
  }

  const columns: ColumnsType<ApiCredentialResponse> = [
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: 'ID', dataIndex: 'id', key: 'id', ellipsis: true },
    { title: '模型白名单', dataIndex: 'modelWhitelist', key: 'modelWhitelist', render: (models: string[]): string => models.length ? models.join(', ') : '未配置' },
    { title: 'Token 上限', dataIndex: 'tokenLimit', key: 'tokenLimit', align: 'right' },
    { title: '状态', dataIndex: 'status', key: 'status', render: (status: string): ReactElement => <ApiCredentialStatusTag status={status} /> },
    {
      title: '操作',
      key: 'actions',
      fixed: 'right',
      render: (_: unknown, credential: ApiCredentialResponse): ReactElement => (
        <Space>
          <Button size="small" onClick={(): void => setEditing(credential)}>编辑</Button>
          <Popconfirm
            title={isEnabled(credential) ? '确认禁用该 API Key？' : '确认启用该 API Key？'}
            description={isEnabled(credential) ? '禁用后使用该 key 的调用将失败。' : '启用后该 key 可继续调用。'}
            onConfirm={(): Promise<void> => handleToggleStatus(credential)}
          >
            <Button size="small" danger={isEnabled(credential)} loading={enableMutation.isPending || disableMutation.isPending}>
              {isEnabled(credential) ? '禁用' : '启用'}
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  if (query.isError) {
    return <PageState status="error" title="API Key 加载失败" description={query.error.message} onRetry={(): void => { query.refetch().catch((): void => undefined); }} />;
  }

  return (
    <Card>
      <Space direction="vertical" style={{ width: '100%' }} size={16}>
        <ApiCredentialToolbar search={search} onSearchChange={setSearch} onCreateClick={(): void => setCreateOpen(true)} onRefresh={(): void => { query.refetch().catch((): void => undefined); }} loading={query.isFetching} />
        <Table<ApiCredentialResponse>
          rowKey="id"
          columns={columns}
          dataSource={filteredCredentials}
          loading={query.isLoading}
          scroll={{ x: 900 }}
          locale={{ emptyText: <Empty description="暂无 API Key，请先创建" /> }}
          pagination={false}
        />
      </Space>
      <ApiCredentialCreateModal open={createOpen} onClose={(): void => setCreateOpen(false)} onCreated={handleCreated} modelOptions={modelOptions} />
      <ApiCredentialEditDrawer open={Boolean(editing)} credential={editing} onClose={(): void => setEditing(null)} onUpdated={(credential): void => setEditing(credential)} modelOptions={modelOptions} />
    </Card>
  );
}
