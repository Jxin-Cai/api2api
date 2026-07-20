import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { Alert, App, Button, Card, Empty, Space, Table, Tag, Tooltip, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMemo, useState, type ReactElement } from 'react';

import { useApiCredentials, type ApiCredentialResponse } from '@entities/api-credential';
import { useModelGroups, type ModelGroupResponse } from '@entities/model-group';
import { PageState } from '@shared/ui';

import { useModelGroupMutations } from '../model/useModelGroupMutations';
import { ModelGroupFormModal } from './ModelGroupFormModal';

interface ModelGroupTablePanelProps {
  modelOptions: Array<{ label: string; value: string }>;
}

export function ModelGroupTablePanel({ modelOptions }: ModelGroupTablePanelProps) {
  const { message, modal } = App.useApp();
  const { groups, query } = useModelGroups();
  const { credentials } = useApiCredentials();
  const { deleteMutation } = useModelGroupMutations();
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<ModelGroupResponse | null>(null);

  const bindingCounts = useMemo(() => credentials.reduce((counts, credential: ApiCredentialResponse) => {
    counts.set(credential.modelGroupId, (counts.get(credential.modelGroupId) ?? 0) + 1);
    return counts;
  }, new Map<string, number>()), [credentials]);

  function openCreate(): void {
    setEditing(null);
    setFormOpen(true);
  }

  function openEdit(group: ModelGroupResponse): void {
    setEditing(group);
    setFormOpen(true);
  }

  function confirmDelete(group: ModelGroupResponse): void {
    const bindingCount = bindingCounts.get(group.id) ?? 0;
    if (bindingCount > 0) {
      message.warning(`请先将该分组下的 ${bindingCount} 个 Key 迁移到其他分组`);
      return;
    }
    modal.confirm({
      title: `删除分组“${group.name}”？`,
      content: '删除后无法恢复。',
      okText: '删除',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: async (): Promise<void> => {
        await deleteMutation.mutateAsync(group.id);
        message.success('模型分组已删除');
      },
    });
  }

  const columns: ColumnsType<ModelGroupResponse> = [
    {
      title: '分组',
      key: 'name',
      width: 240,
      render: (_: unknown, group): ReactElement => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong>{group.name}</Typography.Text>
          <Typography.Text type="secondary" copyable={{ text: group.id }}>{group.id}</Typography.Text>
        </Space>
      ),
    },
    {
      title: '允许的大模型',
      dataIndex: 'modelWhitelist',
      key: 'modelWhitelist',
      render: (models: string[]): ReactElement => (
        <Space size={[4, 6]} wrap>
          {models.length === 0 ? <Typography.Text type="secondary">全部禁用</Typography.Text> : models.slice(0, 4).map((model) => <Tag key={model}>{model}</Tag>)}
          {models.length > 4 ? <Tooltip title={models.slice(4).join(', ')}><Tag>+{models.length - 4}</Tag></Tooltip> : null}
        </Space>
      ),
    },
    {
      title: '已绑定 Key',
      key: 'bindings',
      width: 120,
      render: (_: unknown, group): ReactElement => <Tag color={(bindingCounts.get(group.id) ?? 0) > 0 ? 'blue' : 'default'}>{bindingCounts.get(group.id) ?? 0}</Tag>,
    },
    {
      title: '',
      key: 'actions',
      width: 112,
      align: 'right',
      render: (_: unknown, group): ReactElement => (
        <Space size={4}>
          <Tooltip title="编辑分组"><Button type="text" icon={<EditOutlined />} aria-label={`编辑分组 ${group.name}`} onClick={(): void => openEdit(group)} /></Tooltip>
          <Tooltip title={(bindingCounts.get(group.id) ?? 0) > 0 ? '仍有 Key 绑定，不能删除' : '删除分组'}>
            <Button type="text" danger icon={<DeleteOutlined />} aria-label={`删除分组 ${group.name}`} onClick={(): void => confirmDelete(group)} />
          </Tooltip>
        </Space>
      ),
    },
  ];

  if (query.isError) {
    return <PageState status="error" title="模型分组加载失败" description={query.error.message} onRetry={(): void => { query.refetch().catch((): void => undefined); }} />;
  }

  return (
    <>
      <Alert type="info" showIcon message="分组集中管理模型权限" description="每个 API Key 绑定一个分组。修改分组白名单后，组内所有 Key 的可用模型会立即同步。" style={{ marginBottom: 16 }} />
      <Card className="api-credential-table-card">
        <div className="api-credential-toolbar">
          <div className="api-credential-toolbar__copy">
            <Typography.Title level={4} className="api-credential-toolbar__title">模型分组</Typography.Title>
            <Typography.Text type="secondary">{groups.length} 个分组，共绑定 {credentials.length} 个 Key</Typography.Text>
          </div>
          <Space wrap>
            <Button aria-label="刷新模型分组" icon={<ReloadOutlined />} onClick={(): void => { query.refetch().catch((): void => undefined); }} loading={query.isFetching} />
            <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>创建分组</Button>
          </Space>
        </div>
        <Table<ModelGroupResponse> rowKey="id" columns={columns} dataSource={groups} loading={query.isLoading} pagination={false} scroll={{ x: 760 }} locale={{ emptyText: <Empty description="暂无模型分组，请先创建" /> }} />
      </Card>
      <ModelGroupFormModal open={formOpen} group={editing} modelOptions={modelOptions} onClose={(): void => setFormOpen(false)} onSaved={(): void => undefined} />
    </>
  );
}
