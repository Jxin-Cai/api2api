import { CopyOutlined, EditOutlined, EllipsisOutlined } from '@ant-design/icons';
import { App, Button, Card, Dropdown, Empty, Modal, Progress, Space, Table, Tag, Tooltip, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMemo, useState, type ReactElement } from 'react';

import { ApiCredentialStatusTag, ApiKeySecretBlock, useApiCredentials, type ApiCredentialResponse, type CreateApiCredentialResponse, type RevealApiCredentialSecretResponse } from '@entities/api-credential';
import { MetricCard } from '@entities/dashboard-metric';
import { formatTokenMillions } from '@shared/lib/formatters';
import { PageState } from '@shared/ui';

import { useApiCredentialMutations } from '../model/useApiCredentialMutations';
import { ApiCredentialCreateModal } from './ApiCredentialCreateModal';
import { ApiCredentialEditDrawer } from './ApiCredentialEditDrawer';
import { ApiCredentialToolbar } from './ApiCredentialToolbar';
import './ApiCredentialTablePanel.css';

interface ApiCredentialTablePanelProps {
  /** 创建或编辑可选模型 */
  modelOptions?: Array<{ label: string; value: string }>;
}

function isEnabled(credential: ApiCredentialResponse): boolean {
  const status = String(credential.status).toUpperCase();
  return status === 'ENABLED' || status === 'ACTIVE';
}

export function ApiCredentialTablePanel({ modelOptions = [] }: ApiCredentialTablePanelProps) {
  const { message, modal } = App.useApp();
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
      actualTokens: summary.actualTokens + (credential.consumedTokens ?? 0),
      totalTokens: summary.totalTokens + (credential.totalTokens ?? 0),
      todayActualTokens: summary.todayActualTokens + (credential.todayConsumedTokens ?? 0),
      todayTotalTokens: summary.todayTotalTokens + (credential.todayTotalTokens ?? 0),
    }),
    { actualTokens: 0, totalTokens: 0, todayActualTokens: 0, todayTotalTokens: 0 }
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

  function confirmToggleStatus(credential: ApiCredentialResponse): void {
    const enabled = isEnabled(credential);
    modal.confirm({
      title: enabled ? '禁用此 API Key？' : '启用此 API Key？',
      content: enabled ? '禁用后，使用此密钥的网关请求将立即失败。' : '启用后，此密钥可以继续发起网关请求。',
      okText: enabled ? '确认禁用' : '确认启用',
      cancelText: '取消',
      okButtonProps: enabled ? { danger: true } : undefined,
      onOk: (): Promise<void> => handleToggleStatus(credential),
    });
  }

  function confirmDelete(credential: ApiCredentialResponse): void {
    modal.confirm({
      title: '永久删除此 API Key？',
      content: '密钥将立即失效且无法恢复，历史使用记录仍会保留。',
      okText: '永久删除',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: (): Promise<void> => handleDelete(credential),
    });
  }

  function handleCreated(_: CreateApiCredentialResponse): void {
    query.refetch().catch((): void => undefined);
  }

  function handleReveal(credential: ApiCredentialResponse): void {
    modal.confirm({
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
          <Typography.Text>实际 {formatTokenMillions(consumedTokens)} / 不限</Typography.Text>
          <Typography.Text type="secondary">总 Token {formatTokenMillions(credential.totalTokens ?? 0)}</Typography.Text>
          <Typography.Text type="secondary">剩余不限</Typography.Text>
        </Space>
      );
    }
    const percent = credential.tokenLimit > 0 ? Math.min(100, Math.round((consumedTokens / credential.tokenLimit) * 100)) : 0;
    return (
      <Space direction="vertical" size={0} style={{ minWidth: 160 }}>
        <Typography.Text>实际 {formatTokenMillions(consumedTokens)} / {formatTokenMillions(credential.tokenLimit)}</Typography.Text>
        <Typography.Text type="secondary">总 Token {formatTokenMillions(credential.totalTokens ?? 0)}</Typography.Text>
        <Progress percent={percent} size="small" status={percent >= 100 ? 'exception' : 'normal'} />
        <Typography.Text type="secondary">剩余 {formatTokenMillions(credential.remainingTokens)}</Typography.Text>
      </Space>
    );
  }

  const columns: ColumnsType<ApiCredentialResponse> = [
    {
      title: '密钥',
      key: 'credential',
      width: 250,
      render: (_: unknown, credential: ApiCredentialResponse): ReactElement => (
        <div className="api-credential-identity">
          <Typography.Text strong>{credential.name}</Typography.Text>
          <Typography.Text className="api-credential-identity__id" copyable={{ text: credential.id }}>
            {credential.id}
          </Typography.Text>
        </div>
      ),
    },
    {
      title: '允许的模型',
      dataIndex: 'modelWhitelist',
      key: 'modelWhitelist',
      width: 250,
      render: (models: string[]): ReactElement => (
        <div className="api-credential-models">
          {models.length ? models.slice(0, 2).map((model) => <Tag key={model}>{model}</Tag>) : <Typography.Text type="secondary">未配置</Typography.Text>}
          {models.length > 2 ? <Tooltip title={models.slice(2).join(', ')}><Tag>+{models.length - 2}</Tag></Tooltip> : null}
        </div>
      ),
    },
    { title: '累计用量 / 限额', key: 'tokenUsage', width: 220, render: (_: unknown, credential: ApiCredentialResponse): ReactElement => renderTokenUsage(credential) },
    {
      title: '今日 Token',
      dataIndex: 'todayConsumedTokens',
      key: 'todayConsumedTokens',
      render: (tokens: number | undefined, credential: ApiCredentialResponse): ReactElement => (
        <Space direction="vertical" size={0}>
          <Typography.Text className="mono-number" title={String(tokens ?? 0)}>
            实际 {formatTokenMillions(tokens ?? 0)}
          </Typography.Text>
          <Typography.Text type="secondary" className="mono-number" title={String(credential.todayTotalTokens ?? 0)}>
            总计 {formatTokenMillions(credential.todayTotalTokens ?? 0)}
          </Typography.Text>
        </Space>
      ),
    },
    { title: '状态', dataIndex: 'status', key: 'status', render: (status: string): ReactElement => <ApiCredentialStatusTag status={status} /> },
    {
      title: '',
      key: 'actions',
      fixed: 'right',
      width: 150,
      align: 'right',
      render: (_: unknown, credential: ApiCredentialResponse): ReactElement => (
        <Space size={4} className="api-credential-actions">
          <Tooltip title="复制完整 Key">
            <Button aria-label={`复制 ${credential.name} 的完整 Key`} type="text" icon={<CopyOutlined />} onClick={(): void => handleReveal(credential)} loading={revealMutation.isPending} />
          </Tooltip>
          <Tooltip title="编辑">
            <Button aria-label={`编辑 ${credential.name}`} type="text" icon={<EditOutlined />} onClick={(): void => setEditing(credential)} />
          </Tooltip>
          <Dropdown
            trigger={['click']}
            menu={{
              items: [
                { key: 'toggle', label: isEnabled(credential) ? '禁用密钥' : '启用密钥', danger: isEnabled(credential) },
                { type: 'divider' },
                { key: 'delete', label: '永久删除', danger: true },
              ],
              onClick: ({ key }): void => key === 'delete' ? confirmDelete(credential) : confirmToggleStatus(credential),
            }}
          >
            <Button aria-label={`${credential.name} 更多操作`} type="text" icon={<EllipsisOutlined />} />
          </Dropdown>
        </Space>
      ),
    },
  ];

  if (query.isError) {
    return <PageState status="error" title="API Key 加载失败" description={query.error.message} onRetry={(): void => { query.refetch().catch((): void => undefined); }} />;
  }

  return (
    <>
      <Space direction="vertical" className="api-credential-panel" size={20}>
        <div className="api-credential-summary">
          <MetricCard title="API Key 总数" value={credentials.length} loading={query.isLoading} />
          <MetricCard title="实际 Token 总用量" value={formatTokenMillions(tokenSummary.actualTokens)} rawValue={tokenSummary.actualTokens} loading={query.isLoading} />
          <MetricCard title="总 Token 总用量" value={formatTokenMillions(tokenSummary.totalTokens)} rawValue={tokenSummary.totalTokens} loading={query.isLoading} />
          <MetricCard title="今日实际 Token" value={formatTokenMillions(tokenSummary.todayActualTokens)} rawValue={tokenSummary.todayActualTokens} loading={query.isLoading} />
          <MetricCard title="今日总 Token" value={formatTokenMillions(tokenSummary.todayTotalTokens)} rawValue={tokenSummary.todayTotalTokens} loading={query.isLoading} />
        </div>
        <Card className="api-credential-table-card">
          <Space direction="vertical" style={{ width: '100%' }} size={16}>
            <ApiCredentialToolbar search={search} onSearchChange={setSearch} onCreateClick={(): void => setCreateOpen(true)} onRefresh={(): void => { query.refetch().catch((): void => undefined); }} loading={query.isFetching} />
            <Table<ApiCredentialResponse>
              rowKey="id"
              columns={columns}
              dataSource={filteredCredentials}
              loading={query.isLoading}
              scroll={{ x: 1140 }}
              locale={{ emptyText: <Empty description="暂无 API Key，请先创建" /> }}
              pagination={false}
              rowClassName="api-credential-table__row"
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
        destroyOnHidden
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
