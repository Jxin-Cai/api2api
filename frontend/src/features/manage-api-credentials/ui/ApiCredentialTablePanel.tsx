import { Button, Card, Empty, Modal, Popconfirm, Progress, Space, Table, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMemo, useState, type ReactElement } from 'react';

import { ApiCredentialStatusTag, ApiKeySecretBlock, useApiCredentials, type ApiCredentialResponse, type CreateApiCredentialResponse, type RevealApiCredentialSecretResponse } from '@entities/api-credential';
import { MetricCard } from '@entities/dashboard-metric';
import { formatTokenMillions } from '@shared/lib/formatters';
import { DashboardSummaryGrid, PageState } from '@shared/ui';

import { useApiCredentialMutations } from '../model/useApiCredentialMutations';
import { ApiCredentialCreateModal } from './ApiCredentialCreateModal';
import { ApiCredentialEditDrawer } from './ApiCredentialEditDrawer';
import { ApiCredentialToolbar } from './ApiCredentialToolbar';

interface ApiCredentialTablePanelProps {
  /** 创建或编辑可选模型 */
  modelOptions?: Array<{ label: string; value: string }>;
}

function isEnabled(credential: ApiCredentialResponse): boolean {
  const status = String(credential.status).toUpperCase();
  return status === 'ENABLED' || status === 'ACTIVE';
}

export function ApiCredentialTablePanel({ modelOptions = [] }: ApiCredentialTablePanelProps) {
  const { credentials, query } = useApiCredentials();
  const { enableMutation, disableMutation, revealMutation, deleteMutation } = useApiCredentialMutations();
  const [search, setSearch] = useState('');
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<ApiCredentialResponse | null>(null);
  const [revealedSecret, setRevealedSecret] = useState<RevealApiCredentialSecretResponse | null>(null);
  const [revealedCredentialName, setRevealedCredentialName] = useState<string>();

  const filteredCredentials = useMemo((): ApiCredentialResponse[] => {
    const keyword = search.trim().toLowerCase();
    if (!keyword) {
      return credentials;
    }
    return credentials.filter((credential: ApiCredentialResponse): boolean =>
      credential.name.toLowerCase().includes(keyword) || credential.id.toLowerCase().includes(keyword)
    );
  }, [credentials, search]);

  const tokenSummary = useMemo(() => credentials.reduce(
    (summary, credential) => ({
      totalTokens: summary.totalTokens + (credential.consumedTokens ?? 0),
      todayTokens: summary.todayTokens + (credential.todayConsumedTokens ?? 0),
    }),
    { totalTokens: 0, todayTokens: 0 }
  ), [credentials]);

  async function handleToggleStatus(credential: ApiCredentialResponse): Promise<void> {
    if (isEnabled(credential)) {
      await disableMutation.mutateAsync(credential.id);
      message.success('API Key 已禁用');
      return;
    }
    await enableMutation.mutateAsync(credential.id);
    message.success('API Key 已启用');
  }

  async function handleDelete(credential: ApiCredentialResponse): Promise<void> {
    try {
      await deleteMutation.mutateAsync(credential.id);
      message.success('API Key 已删除');
    } catch (error: unknown) {
      message.error(error instanceof Error ? error.message : '删除 API Key 失败');
    }
  }

  function handleCreated(_: CreateApiCredentialResponse): void {
    query.refetch().catch((): void => undefined);
  }

  function handleReveal(credential: ApiCredentialResponse): void {
    Modal.confirm({
      title: '复制完整 API Key？',
      content: '完整 API Key 将短暂显示，请确认当前环境安全，复制后请妥善保管。',
      okText: '继续复制',
      cancelText: '取消',
      onOk: async (): Promise<void> => {
        try {
          const secret = await revealMutation.mutateAsync(credential.id);
          setRevealedCredentialName(credential.name);
          setRevealedSecret(secret);
        } catch (error: unknown) {
          message.error(error instanceof Error ? error.message : '获取 API Key 失败');
        }
      },
    });
  }

  function renderTokenUsage(credential: ApiCredentialResponse): ReactElement {
    const consumedTokens = credential.consumedTokens ?? 0;
    if (credential.tokenLimit === 0) {
      return (
        <Space direction="vertical" size={0}>
          <Typography.Text>{formatTokenMillions(consumedTokens)} / 不限</Typography.Text>
          <Typography.Text type="secondary">剩余不限</Typography.Text>
        </Space>
      );
    }
    const percent = credential.tokenLimit > 0 ? Math.min(100, Math.round((consumedTokens / credential.tokenLimit) * 100)) : 0;
    return (
      <Space direction="vertical" size={0} style={{ minWidth: 160 }}>
        <Typography.Text>{formatTokenMillions(consumedTokens)} / {formatTokenMillions(credential.tokenLimit)}</Typography.Text>
        <Progress percent={percent} size="small" status={percent >= 100 ? 'exception' : 'normal'} />
        <Typography.Text type="secondary">剩余 {formatTokenMillions(credential.remainingTokens)}</Typography.Text>
      </Space>
    );
  }

  const columns: ColumnsType<ApiCredentialResponse> = [
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: 'ID', dataIndex: 'id', key: 'id', ellipsis: true },
    { title: '模型白名单', dataIndex: 'modelWhitelist', key: 'modelWhitelist', render: (models: string[]): string => models.length ? models.join(', ') : '未配置' },
    { title: '累计 Token / 限额', key: 'tokenUsage', render: (_: unknown, credential: ApiCredentialResponse): ReactElement => renderTokenUsage(credential) },
    {
      title: '今日 Token',
      dataIndex: 'todayConsumedTokens',
      key: 'todayConsumedTokens',
      render: (tokens?: number): ReactElement => (
        <Typography.Text className="mono-number" title={String(tokens ?? 0)}>
          {formatTokenMillions(tokens ?? 0)}
        </Typography.Text>
      ),
    },
    { title: '状态', dataIndex: 'status', key: 'status', render: (status: string): ReactElement => <ApiCredentialStatusTag status={status} /> },
    {
      title: '操作',
      key: 'actions',
      fixed: 'right',
      render: (_: unknown, credential: ApiCredentialResponse): ReactElement => (
        <Space>
          <Button size="small" onClick={(): void => setEditing(credential)}>编辑</Button>
          <Button size="small" onClick={(): void => handleReveal(credential)} loading={revealMutation.isPending}>查看并复制 Key</Button>
          <Popconfirm
            title={isEnabled(credential) ? '确认禁用该 API Key？' : '确认启用该 API Key？'}
            description={isEnabled(credential) ? '禁用后使用该 key 的调用将失败。' : '启用后该 key 可继续调用。'}
            onConfirm={(): Promise<void> => handleToggleStatus(credential)}
          >
            <Button size="small" danger={isEnabled(credential)} loading={enableMutation.isPending || disableMutation.isPending}>
              {isEnabled(credential) ? '禁用' : '启用'}
            </Button>
          </Popconfirm>
          <Popconfirm
            title="确认删除该 API Key？"
            description="删除后该 Key 将立即失效且无法恢复，历史使用记录不受影响。"
            okText="确认删除"
            cancelText="取消"
            okButtonProps={{ danger: true }}
            onConfirm={(): Promise<void> => handleDelete(credential)}
          >
            <Button
              size="small"
              danger
              loading={deleteMutation.isPending && deleteMutation.variables === credential.id}
            >
              删除
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
    <>
      <Space direction="vertical" style={{ width: '100%' }} size={20}>
        <DashboardSummaryGrid>
          <MetricCard title="API Key 总数" value={credentials.length} loading={query.isLoading} />
          <MetricCard title="Token 总用量" value={formatTokenMillions(tokenSummary.totalTokens)} rawValue={tokenSummary.totalTokens} loading={query.isLoading} />
          <MetricCard title="今日 Token" value={formatTokenMillions(tokenSummary.todayTokens)} rawValue={tokenSummary.todayTokens} loading={query.isLoading} />
        </DashboardSummaryGrid>
        <Card>
          <Space direction="vertical" style={{ width: '100%' }} size={16}>
            <ApiCredentialToolbar search={search} onSearchChange={setSearch} onCreateClick={(): void => setCreateOpen(true)} onRefresh={(): void => { query.refetch().catch((): void => undefined); }} loading={query.isFetching} />
            <Table<ApiCredentialResponse>
              rowKey="id"
              columns={columns}
              dataSource={filteredCredentials}
              loading={query.isLoading}
              scroll={{ x: 1040 }}
              locale={{ emptyText: <Empty description="暂无 API Key，请先创建" /> }}
              pagination={false}
            />
          </Space>
        </Card>
      </Space>
      <ApiCredentialCreateModal open={createOpen} onClose={(): void => setCreateOpen(false)} onCreated={handleCreated} modelOptions={modelOptions} />
      <ApiCredentialEditDrawer open={Boolean(editing)} credential={editing} onClose={(): void => setEditing(null)} onUpdated={(credential): void => setEditing(credential)} modelOptions={modelOptions} />
      <Modal
        title="复制 API Key"
        open={Boolean(revealedSecret)}
        onCancel={(): void => { setRevealedSecret(null); setRevealedCredentialName(undefined); }}
        footer={<Button type="primary" onClick={(): void => { setRevealedSecret(null); setRevealedCredentialName(undefined); }}>关闭</Button>}
        destroyOnClose
      >
        <ApiKeySecretBlock
          plainApiKey={revealedSecret?.plainApiKey ?? ''}
          credentialName={revealedCredentialName}
          maskAfterCopy
          warningMessage="完整 API Key 已临时显示。复制后请妥善保管，关闭窗口后页面不会保留明文。"
        />
      </Modal>
    </>
  );
}
