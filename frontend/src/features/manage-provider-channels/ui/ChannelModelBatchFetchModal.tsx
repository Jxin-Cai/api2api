import { useEffect, useState } from 'react';
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

interface BatchFetchResult {
  channelId: number;
  channelName: string;
  host: string;
  status: BatchFetchStatus;
  models: ChannelModelSupportResponse[];
  error?: string;
  saving?: boolean;
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
      models: [],
    })));
    const nextResults = await Promise.all(selectedChannels.map(async (channel): Promise<BatchFetchResult> => {
      try {
        const response = await fetchProviderChannelModelPreview(channel.id, { defaultPriority: 10 });
        return {
          channelId: channel.id,
          channelName: channel.name,
          host: channel.host,
          status: 'success',
          models: response.data.models,
        };
      } catch (error) {
        return {
          channelId: channel.id,
          channelName: channel.name,
          host: channel.host,
          status: 'error',
          models: [],
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
    message.success(`验证成功，已获取 ${succeeded} 个渠道的模型列表`);
  }

  async function handleSave(result: BatchFetchResult): Promise<void> {
    const channel = targetChannels.find((item) => item.id === result.channelId);
    if (!channel || result.models.length === 0) {
      return;
    }
    setResults((current) => current.map((item) => (
      item.channelId === result.channelId ? { ...item, saving: true } : item
    )));
    try {
      const response = await batchUpsertChannelModels(channel.id, {
        replaceExisting: false,
        models: mergeFetchedModels(channel.supportedModels, result.models),
      });
      onChannelChanged(response.data);
      message.success(`已保存 ${channel.name} 的 ${result.models.length} 个模型`);
    } catch (error) {
      message.error(`${channel.name} 保存失败：${getApiErrorMessage(error, '请稍后重试')}`);
    } finally {
      setResults((current) => current.map((item) => (
        item.channelId === result.channelId ? { ...item, saving: false } : item
      )));
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
      return result.status === 'success' ? <Tag color="success">成功</Tag> : <Tag color="error">失败</Tag>;
    },
  }, {
    title: '结果',
    key: 'result',
    render: (_value, result) => {
      if (result.status === 'pending') {
        return <Typography.Text type="secondary">正在请求 host/v1/models</Typography.Text>;
      }
      if (result.status === 'success') {
        return <Typography.Text>已获取 {result.models.length} 个模型</Typography.Text>;
      }
      return <Typography.Text type="danger">{result.error}</Typography.Text>;
    },
  }, {
    title: '操作',
    key: 'actions',
    width: 120,
    render: (_value, result) => (
      <Button
        size="small"
        disabled={result.status !== 'success' || result.models.length === 0}
        loading={result.saving}
        onClick={() => void handleSave(result)}
      >
        保存模型
      </Button>
    ),
  }];

  return (
    <Modal
      title={`验证并获取 ${targetChannels.length || channels.length} 个渠道的模型列表`}
      open={open}
      onCancel={onClose}
      footer={<Button onClick={onClose}>关闭</Button>}
      width={880}
      destroyOnHidden
    >
      <Space direction="vertical" style={{ width: '100%' }} size={12}>
        <Alert
          type="info"
          showIcon
          message="将请求每个渠道的 host/v1/models，并携带 Authorization: Bearer 渠道 Key。"
          description={fetching
            ? '正在验证所选渠道，失败渠道会展示上游返回的原因。'
            : `成功 ${succeeded} 个，失败 ${failed} 个。失败不会中断其他渠道。`}
        />
        <Table
          rowKey="channelId"
          size="small"
          columns={columns}
          dataSource={results}
          loading={fetching && results.length === 0}
          pagination={false}
        />
      </Space>
    </Modal>
  );
}

function mergeFetchedModels(
  existingModels: ChannelModelSupportResponse[],
  fetchedModels: ChannelModelSupportResponse[]
): Array<{
  id?: number;
  requestedModel: string;
  upstreamModel: string;
  upstreamProtocol: string;
  priority: number;
  preferred: boolean;
  source: ChannelModelSupportResponse['source'];
}> {
  return fetchedModels.map((model) => {
    const existing = existingModels.find((item) => (
      item.requestedModel === model.requestedModel && item.upstreamProtocol === model.upstreamProtocol
    ));
    return {
      id: existing?.id,
      requestedModel: model.requestedModel,
      upstreamModel: model.upstreamModel,
      upstreamProtocol: model.upstreamProtocol,
      priority: existing?.priority ?? model.priority,
      preferred: Boolean(existing?.preferred ?? model.preferred),
      source: existing?.source ?? model.source,
    };
  });
}
