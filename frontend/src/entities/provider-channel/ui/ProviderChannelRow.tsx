import type { ReactNode } from 'react';
import { Badge, Space, Tag, Tooltip, Typography } from 'antd';
import type { ChannelModelSupportResponse } from '@shared/api/contracts';
import type { ProviderChannelResponse } from '../model/types';
import { ProtocolTag } from './ProtocolTag';
import { ProviderChannelStatusTag } from './ProviderChannelStatusTag';
import { formatProtocolDirection, getProtocolMeta } from '@shared/lib/protocols';

interface ProviderChannelRowProps {
  /** 渠道响应 */
  channel: ProviderChannelResponse;
  /** 操作区插槽 */
  actions?: ReactNode;
  /** 是否展开模型配置 */
  expanded?: boolean;
}

function modelSummary(models: ChannelModelSupportResponse[]): string {
  if (models.length === 0) {
    return '未配置';
  }
  const names = models.map((model) => model.requestedModel);
  const summary = names.slice(0, 3).join('、');
  return names.length > 3 ? `${summary} 等 ${names.length} 个` : summary;
}

function modelTooltip(models: ChannelModelSupportResponse[]): string {
  if (models.length === 0) {
    return '未配置模型';
  }
  return models.map((model) => `${model.requestedModel} → ${model.upstreamModel} (${model.upstreamProtocol}, 排序 ${model.priority})`).join('\n');
}

export function ProviderChannelRow({ channel, actions, expanded = false }: ProviderChannelRowProps) {
  const enabledModels = channel.supportedModels.filter((model) => model.status === 'ENABLED');
  const preferredModels = enabledModels.filter((model) => model.preferred);
  const mappings = channel.protocolMappings ?? [];
  const modelCountTitle = enabledModels.length === channel.supportedModels.length
    ? `启用模型数量：${enabledModels.length}`
    : `启用 ${enabledModels.length} / 总计 ${channel.supportedModels.length}`;

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
        <Typography.Text type="secondary">模型列表路径：{channel.modelsPath ?? '/v1/models'}</Typography.Text>
        <Typography.Text type="secondary">Key: {channel.keyMasked ?? channel.keyRef}</Typography.Text>
        <Typography.Text type="secondary">渠道优先级：{channel.routePriority ?? 0}（数字越大越优先）</Typography.Text>
        <Tooltip title={modelTooltip(enabledModels)}>
          <Typography.Text type="secondary">启用模型：{enabledModels.length} 个（{modelSummary(enabledModels)}）</Typography.Text>
        </Tooltip>
        <Tooltip title={modelTooltip(preferredModels)}>
          <Typography.Text type="secondary">★ 优先模型：{preferredModels.length} 个（{modelSummary(preferredModels)}）</Typography.Text>
        </Tooltip>
      </Space>
      <Space wrap>
        {mappings.length > 0 ? mappings.map((mapping) => (
          <Tooltip key={mapping.requestProtocol} title={`${mapping.requestProtocol} -> ${mapping.upstreamProtocol}`}>
            <Tag color={getProtocolMeta(mapping.upstreamProtocol).color}>
              {formatProtocolDirection(mapping.requestProtocol, mapping.upstreamProtocol)}
            </Tag>
          </Tooltip>
        )) : channel.supportedProtocols.map((protocol) => (
          <ProtocolTag key={protocol} protocol={protocol} compact />
        ))}
        <Badge count={enabledModels.length} showZero title={modelCountTitle} />
        <ProviderChannelStatusTag status={channel.status} />
        {actions}
      </Space>
    </Space>
  );
}
