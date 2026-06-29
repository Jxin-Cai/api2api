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
  return (
    <Card size="small" title={`模型配置：${channel.name}`}>
      <Space direction="vertical" style={{ width: '100%' }}>
        <Space style={{ justifyContent: 'space-between', width: '100%' }}>
          <Typography.Text type="secondary">当前 {channel.supportedModels.length} 个模型支持项</Typography.Text>
          <Button onClick={() => setFetchOpen(true)}>拉取模型</Button>
        </Space>
        <ChannelModelEditorTable channelId={channel.id} models={channel.supportedModels} onChanged={onChannelChanged} />
        <ChannelModelFetchModal open={fetchOpen} channelId={channel.id} channelName={channel.name} onClose={() => setFetchOpen(false)} onFetched={onChannelChanged} />
      </Space>
    </Card>
  );
}
