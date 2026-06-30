import type { ReactNode } from 'react';
import { Badge, Space, Tooltip, Typography } from 'antd';
import type { ProviderChannelResponse } from '../model/types';
import { ProtocolTag } from './ProtocolTag';
import { ProviderChannelStatusTag } from './ProviderChannelStatusTag';

interface ProviderChannelRowProps {
  /** 渠道响应 */
  channel: ProviderChannelResponse;
  /** 操作区插槽 */
  actions?: ReactNode;
  /** 是否展开模型配置 */
  expanded?: boolean;
}

export function ProviderChannelRow({ channel, actions, expanded = false }: ProviderChannelRowProps) {
  return (
    <Space wrap style={{ width: '100%', justifyContent: 'space-between' }}>
      <Space direction="vertical" size={2}>
        <Space>
          <Typography.Text strong>{channel.name}</Typography.Text>
          {expanded ? <Badge status="processing" text="模型配置" /> : null}
        </Space>
        <Tooltip title={channel.host}>
          <Typography.Text type="secondary" ellipsis style={{ maxWidth: 280 }}>
            {channel.host}
          </Typography.Text>
        </Tooltip>
        <Typography.Text type="secondary">Key: {channel.keyMasked ?? channel.keyRef}</Typography.Text>
        <Typography.Text type="secondary">渠道优先级：{channel.routePriority ?? 0}</Typography.Text>
      </Space>
      <Space wrap>
        {channel.supportedProtocols.map((protocol) => (
          <ProtocolTag key={protocol} protocol={protocol} compact />
        ))}
        <Badge count={channel.supportedModels.length} showZero title="模型数量" />
        <ProviderChannelStatusTag status={channel.status} />
        {actions}
      </Space>
    </Space>
  );
}
