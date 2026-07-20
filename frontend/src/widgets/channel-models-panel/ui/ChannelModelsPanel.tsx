import { useState } from 'react';
import { Button, Card, Space, Typography } from 'antd';
import type { ProviderChannelResponse } from '@entities/provider-channel';
import { ChannelModelEditorTable, ChannelModelFetchModal } from '@features/manage-channel-models';

interface ChannelModelsPanelProps {
  /** 渠道 */
  channel: ProviderChannelResponse;
  /** 渠道变更回调 */
  onChannelChanged: (channel: ProviderChannelResponse) => void;
}

export function ChannelModelsPanel({ channel, onChannelChanged }: ChannelModelsPanelProps) {
  const [fetchOpen, setFetchOpen] = useState(false);
  const enabledModels = channel.supportedModels.filter((model) => model.status === 'ENABLED');
  const rateLimitedModels = channel.supportedModels.filter((model) => model.status === 'RATE_LIMITED');
  return (
    <Card size="small" title={`模型配置：${channel.name}`}>
      <Space direction="vertical" style={{ width: '100%' }}>
        <Space style={{ justifyContent: 'space-between', width: '100%' }}>
          <Typography.Text type="secondary">当前可路由 {enabledModels.length} 个模型，限流 {rateLimitedModels.length} 个模型</Typography.Text>
          <Button onClick={() => setFetchOpen(true)}>验证并获取模型列表</Button>
        </Space>
        <ChannelModelEditorTable channel={channel} models={channel.supportedModels} onChanged={onChannelChanged} />
        <ChannelModelFetchModal open={fetchOpen} channelId={channel.id} channelName={channel.name} models={channel.supportedModels} onClose={() => setFetchOpen(false)} onFetched={onChannelChanged} />
      </Space>
    </Card>
  );
}
