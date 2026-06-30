import type { ReactNode } from 'react';
import { Space, Tag, Tooltip, Typography } from 'antd';
import type { ChannelModelSupportResponse } from '../model/types';
import { ModelPriorityBadge } from './ModelPriorityBadge';
import { ModelSourceTag } from './ModelSourceTag';

interface ChannelModelSupportRowProps {
  /** 模型支持项 */
  model: ChannelModelSupportResponse;
  /** 是否编辑态 */
  editing?: boolean;
  /** 操作区 */
  actions?: ReactNode;
}

export function ChannelModelSupportRow({ model, editing = false, actions }: ChannelModelSupportRowProps) {
  return (
    <Space wrap style={{ width: '100%', justifyContent: 'space-between' }}>
      <Space wrap>
        <Typography.Text strong>{model.requestedModel}</Typography.Text>
        <Typography.Text type="secondary">→ {model.upstreamModel}</Typography.Text>
        <Tag>{model.upstreamProtocol}</Tag>
        <ModelPriorityBadge priority={model.priority} />
        {model.preferred ? <Tooltip title="该模型请求会优先尝试此渠道，失败后再回退普通模型"><Tag color="gold">★ 优先模型</Tag></Tooltip> : null}
        <ModelSourceTag source={model.source} />
        <Tag color={model.status === 'ENABLED' ? 'success' : 'default'}>{model.status}</Tag>
        {editing ? <Tag color="processing">编辑中</Tag> : null}
      </Space>
      {actions}
    </Space>
  );
}
