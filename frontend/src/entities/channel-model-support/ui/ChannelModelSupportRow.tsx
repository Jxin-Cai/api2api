import type { ReactNode } from 'react';
import { Space, Tag, Typography } from 'antd';
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
        <ModelSourceTag source={model.source} />
        <Tag color={model.status === 'ENABLED' ? 'success' : 'default'}>{model.status}</Tag>
        {editing ? <Tag color="processing">编辑中</Tag> : null}
      </Space>
      {actions}
    </Space>
  );
}
