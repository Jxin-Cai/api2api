import { Card, Skeleton, Space, Statistic, Typography } from 'antd';

import { formatTokenMillions, formatTokenThousands } from '@shared/lib/formatters';
import type { UsageScope } from '../model/types';

interface UsageTokenSummaryProps {
  /** 过滤后的 token 汇总 */
  totalTokens: number;
  /** 当前筛选记录总数 */
  recordCount?: number;
  /** 前台或后台范围 */
  scope: UsageScope;
  /** 是否加载中 */
  loading?: boolean;
}

export function UsageTokenSummary({ totalTokens, recordCount = 0, scope, loading = false }: UsageTokenSummaryProps) {
  const formattedTokens = scope === 'front'
    ? formatTokenMillions(totalTokens)
    : formatTokenThousands(totalTokens);
  const formattedRecordCount = scope === 'front'
    ? formatTokenThousands(recordCount)
    : String(recordCount);

  return (
    <Card size="small" styles={{ body: { background: 'linear-gradient(90deg, #f0f7ff, #ffffff)' } }}>
      {loading ? (
        <Skeleton active paragraph={false} />
      ) : (
        <Space size={32} wrap>
          <Statistic
            title={scope === 'admin' ? '全平台过滤后实际 Token' : '个人过滤后实际 Token'}
            value={formattedTokens}
          />
          <Typography.Text type="secondary">记录总数：{formattedRecordCount}</Typography.Text>
        </Space>
      )}
    </Card>
  );
}
