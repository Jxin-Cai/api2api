import { useEffect, useState, type Key } from 'react';
import { Alert, App, Button, Modal, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { batchUpsertChannelModels, fetchProviderChannelModelPreview, type ChannelModelSupportResponse } from '@entities/channel-model-support';
import type { ProviderChannelResponse } from '@entities/provider-channel';
import { getApiErrorMessage } from '@shared/api';

interface ChannelModelBatchFetchModalProps {
  /** 打开状态 */
  open: boolean;
  /** 待验证渠道 */
  channels: ProviderChannelResponse[];
  /** 关闭回调 */
  onClose: () => void;
  /** 渠道变更回调 */
  onChannelChanged: (channel: ProviderChannelResponse) => void;
}

type BatchFetchStatus = 'pending' | 'success' | 'error';

interface CandidateModel extends ChannelModelSupportResponse {
  /** 已保存记录的真实 ID；新增候选为空 */
  existingId?: number;
  /** 已保存但本次上游未返回 */
  missingUpstream: boolean;
}

interface BatchFetchResult {
  channelId: number;
  channelName: string;
  host: string;
  status: BatchFetchStatus;
  /** 候选模型：上游返回的模型 + 已保存但上游未返回的模型 */
  candidates: CandidateModel[];
  /** 勾选的候选模型 ID，仅勾选的模型会在保存时生效 */
  selectedIds: Key[];
  error?: string;
  saving?: boolean;
  saved?: boolean;
}

export function ChannelModelBatchFetchModal({
  open,
  channels,
  onClose,
  onChannelChanged,
}: ChannelModelBatchFetchModalProps) {
  const { message } = App.useApp();
  const [targetChannels, setTargetChannels] = useState<ProviderChannelResponse[]>([]);
  const [results, setResults] = useState<BatchFetchResult[]>([]);
  const [fetching, setFetching] = useState(false);

  useEffect(() => {
    if (!open) {
      setTargetChannels([]);
      setResults([]);
      setFetching(false);
      return;
    }
    setTargetChannels(channels);
    void fetchSelectedChannels(channels);
  }, [open]);

  async function fetchSelectedChannels(selectedChannels: ProviderChannelResponse[]): Promise<void> {
    setFetching(true);
    setResults(selectedChannels.map((channel) => ({
      channelId: channel.id,
      channelName: channel.name,
      host: channel.host,
      status: 'pending',
      candidates: [],
      selectedIds: [],
    })));
    const nextResults = await Promise.all(selectedChannels.map(async (channel): Promise<BatchFetchResult> => {
      try {
        const response = await fetchProviderChannelModelPreview(channel.id, { defaultPriority: 10 });
        const candidates = buildCandidates(channel.supportedModels ?? [], response.data.models);
        return {
          channelId: channel.id,
          channelName: channel.name,
          host: channel.host,
          status: 'success',
          candidates,
          selectedIds: candidates.filter((model) => model.status === 'ENABLED').map((model) => model.id),
        };
      } catch (error) {
        return {
          channelId: channel.id,
          channelName: channel.name,
          host: channel.host,
          status: 'error',
          candidates: [],
          selectedIds: [],
          error: getApiErrorMessage(error, '请检查 Host、Key 和模型列表权限'),
        };
      }
    }));
    setResults(nextResults);
    setFetching(false);
    const succeeded = nextResults.filter((result) => result.status === 'success').length;
    const failed = nextResults.length - succeeded;
    if (failed > 0) {
      message.error(`验证完成：成功 ${succeeded} 个，失败 ${failed} 个`);
      return;
    }
    message.success(`验证成功，已获取 ${succeeded} 个渠道的模型候选，请勾选后保存`);
  }

  function patchResult(channelId: number, patch: Partial<BatchFetchResult>): void {
    setResults((current) => current.map((item) => (item.channelId === channelId ? { ...item, ...patch } : item)));
  }

  async function handleSave(result: BatchFetchResult): Promise<void> {
    const channel = targetChannels.find((item) => item.id === result.channelId);
    if (!channel) {
      return;
    }
    const selectedModels = result.candidates.filter((model) => result.selectedIds.includes(model.id));
    if (selectedModels.length === 0) {
      message.warning(`${channel.name}：请至少勾选一个模型再保存`);
      return;
    }
    patchResult(result.channelId, { saving: true });
    try {
      const response = await batchUpsertChannelModels(channel.id, {
        replaceExisting: true,
        models: selectedModels.map((model) => ({
          id: model.existingId,
          requestedModel: model.requestedModel,
          upstreamModel: model.upstreamModel,
          upstreamProtocol: model.upstreamProtocol,
          priority: model.priority,
          preferred: Boolean(model.preferred),
          source: model.source,
        })),
      });
      onChannelChanged(response.data);
      patchResult(result.channelId, { saved: true });
      message.success(`已保存 ${channel.name} 的 ${selectedModels.length} 个模型为启用模型`);
    } catch (error) {
      message.error(`${channel.name} 保存失败：${getApiErrorMessage(error, '请稍后重试')}`);
    } finally {
      patchResult(result.channelId, { saving: false });
    }
  }

  const succeeded = results.filter((result) => result.status === 'success').length;
  const failed = results.filter((result) => result.status === 'error').length;
  const columns: ColumnsType<BatchFetchResult> = [{
    title: '渠道',
    dataIndex: 'channelName',
    width: 220,
    render: (_value, result) => (
      <Space direction="vertical" size={0}>
        <Typography.Text strong>{result.channelName}</Typography.Text>
        <Typography.Text type="secondary">{result.host}/v1/models</Typography.Text>
      </Space>
    ),
  }, {
    title: '状态',
    dataIndex: 'status',
    width: 110,
    render: (_value, result) => {
      if (result.status === 'pending') {
        return <Tag>验证中</Tag>;
      }
      if (result.status === 'error') {
        return <Tag color="error">失败</Tag>;
      }
      return result.saved ? <Tag color="success">已保存</Tag> : <Tag color="processing">待保存</Tag>;
    },
  }, {
    title: '结果',
    key: 'result',
    render: (_value, result) => {
      if (result.status === 'pending') {
        return <Typography.Text type="secondary">正在请求 host/v1/models</Typography.Text>;
      }
      if (result.status === 'success') {
        return (
          <Typography.Text>
            候选 {result.candidates.length} 个，已勾选 {result.selectedIds.length} 个
          </Typography.Text>
        );
      }
      return <Typography.Text type="danger">{result.error}</Typography.Text>;
    },
  }, {
    title: '操作',
    key: 'actions',
    width: 140,
    render: (_value, result) => (
      <Button
        size="small"
        type="primary"
        disabled={result.status !== 'success' || result.selectedIds.length === 0}
        loading={result.saving}
        onClick={() => void handleSave(result)}
      >
        保存所选模型
      </Button>
    ),
  }];

  return (
    <Modal
      title={`验证并获取 ${targetChannels.length || channels.length} 个渠道的模型列表`}
      open={open}
      onCancel={onClose}
      footer={<Button onClick={onClose}>关闭</Button>}
      width={960}
      destroyOnHidden
    >
      <Space direction="vertical" style={{ width: '100%' }} size={12}>
        <Alert
          type="info"
          showIcon
          message="将请求每个渠道的 host/v1/models，并携带 Authorization: Bearer 渠道 Key。获取结果仅为候选列表，不会自动生效。"
          description={fetching
            ? '正在验证所选渠道，失败渠道会展示上游返回的原因。'
            : `成功 ${succeeded} 个，失败 ${failed} 个。展开渠道行勾选模型，仅勾选并保存的模型才会启用；默认勾选当前已启用的模型。`}
        />
        <Table
          rowKey="channelId"
          size="small"
          columns={columns}
          dataSource={results}
          loading={fetching && results.length === 0}
          pagination={false}
          expandable={{
            rowExpandable: (result) => result.status === 'success' && result.candidates.length > 0,
            defaultExpandAllRows: false,
            expandedRowRender: (result) => (
              <CandidateModelTable
                result={result}
                onSelectionChange={(selectedIds) => patchResult(result.channelId, { selectedIds, saved: false })}
              />
            ),
          }}
        />
      </Space>
    </Modal>
  );
}

interface CandidateModelTableProps {
  result: BatchFetchResult;
  onSelectionChange: (selectedIds: Key[]) => void;
}

function CandidateModelTable({ result, onSelectionChange }: CandidateModelTableProps) {
  const columns: ColumnsType<CandidateModel> = [{
    title: '模型候选',
    dataIndex: 'requestedModel',
    render: (_value, model) => (
      <Space wrap>
        <Typography.Text strong>{model.requestedModel}</Typography.Text>
        <Typography.Text type="secondary">→ {model.upstreamModel}</Typography.Text>
        <Tag>{model.upstreamProtocol}</Tag>
        {renderCandidateOrigin(model)}
      </Space>
    ),
  }, {
    title: '模型排序值',
    dataIndex: 'priority',
    width: 110,
  }, {
    title: '优先模型',
    dataIndex: 'preferred',
    width: 100,
    render: (value: boolean | undefined) => (value ? <Tag color="gold">★ 优先</Tag> : <Typography.Text type="secondary">普通</Typography.Text>),
  }];

  return (
    <Space direction="vertical" style={{ width: '100%' }} size={8}>
      <Table
        rowKey="id"
        size="small"
        columns={columns}
        dataSource={result.candidates}
        pagination={{ pageSize: 8, size: 'small' }}
        rowSelection={{ selectedRowKeys: result.selectedIds, onChange: onSelectionChange }}
      />
      <Space wrap>
        <Button size="small" onClick={() => onSelectionChange(result.candidates.map((model) => model.id))}>全选候选</Button>
        <Button size="small" onClick={() => onSelectionChange([])}>清空选择</Button>
        <Button
          size="small"
          onClick={() => onSelectionChange(result.candidates.filter((model) => model.status === 'ENABLED').map((model) => model.id))}
        >
          恢复为当前启用
        </Button>
      </Space>
    </Space>
  );
}

function renderCandidateOrigin(model: CandidateModel) {
  if (model.existingId === undefined) {
    return <Tag color="success">新增</Tag>;
  }
  if (model.missingUpstream) {
    return <Tag color="warning">已存在（上游未返回）</Tag>;
  }
  return <Tag color="processing">已存在</Tag>;
}

/**
 * 以上游返回的模型为基础合并已保存配置；已保存但上游未返回的模型追加为候选，
 * 避免在“替换保存”时被静默删除。是否生效始终由用户勾选决定。
 */
function buildCandidates(
  existingModels: ChannelModelSupportResponse[],
  fetchedModels: ChannelModelSupportResponse[]
): CandidateModel[] {
  const fetchedKeys = new Set(fetchedModels.map(modelKey));
  const merged: CandidateModel[] = fetchedModels.map((model) => {
    const existing = existingModels.find((item) => modelKey(item) === modelKey(model));
    if (!existing) {
      return { ...model, missingUpstream: false };
    }
    return {
      ...model,
      id: existing.id,
      existingId: existing.id,
      missingUpstream: false,
      priority: existing.priority,
      preferred: existing.preferred,
      source: existing.source,
      status: existing.status,
    };
  });
  const missing: CandidateModel[] = existingModels
    .filter((model) => !fetchedKeys.has(modelKey(model)))
    .map((model) => ({ ...model, existingId: model.id, missingUpstream: true }));
  return [...merged, ...missing];
}

function modelKey(model: Pick<ChannelModelSupportResponse, 'requestedModel' | 'upstreamProtocol'>): string {
  return `${model.requestedModel}::${model.upstreamProtocol}`;
}
