import { Card, Descriptions, Space, Tag, Tooltip, Typography } from 'antd';
import type { ReactNode } from 'react';
import { useState } from 'react';

import type { ApiCredentialResponse } from '../model/types';
import { ApiCredentialStatusTag } from './ApiCredentialStatusTag';

interface ApiCredentialRowProps {
  /** API Key 凭证 */
  credential: ApiCredentialResponse;
  /** 操作区插槽 */
  actions?: ReactNode;
  /** 是否使用紧凑卡片模式 */
  compact?: boolean;
}

export function ApiCredentialRow({ credential, actions, compact = false }: ApiCredentialRowProps) {
  const [expandedModels, setExpandedModels] = useState(false);
  const visibleModels = expandedModels ? credential.modelWhitelist : credential.modelWhitelist.slice(0, 3);

  return (
    <Card size="small" hoverable style={{ borderRadius: 10 }}>
      <Space direction="vertical" style={{ width: '100%' }} size={compact ? 8 : 12}>
        <Space align="start" style={{ width: '100%', justifyContent: 'space-between' }}>
          <div>
            <Typography.Text strong>{credential.name}</Typography.Text>
            <br />
            <Tooltip title={credential.id}>
              <Typography.Text type="secondary" copyable>{credential.id}</Typography.Text>
            </Tooltip>
          </div>
          {actions}
        </Space>
        <Descriptions size="small" column={compact ? 1 : 3}>
          <Descriptions.Item label="模型白名单">
            <Space size={4} wrap>
              {visibleModels.length > 0 ? visibleModels.map((model: string) => <Tag key={model}>{model}</Tag>) : <Tag>全部禁用</Tag>}
              {credential.modelWhitelist.length > 3 ? (
                <Tag onClick={(): void => setExpandedModels(!expandedModels)} style={{ cursor: 'pointer' }}>
                  {expandedModels ? '收起' : `+${credential.modelWhitelist.length - 3}`}
                </Tag>
              ) : null}
            </Space>
          </Descriptions.Item>
          <Descriptions.Item label="Token 上限">{credential.tokenLimit}</Descriptions.Item>
          <Descriptions.Item label="状态"><ApiCredentialStatusTag status={credential.status} /></Descriptions.Item>
        </Descriptions>
      </Space>
    </Card>
  );
}
