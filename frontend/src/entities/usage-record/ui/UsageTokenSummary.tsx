import { Skeleton } from 'antd';

import type { UsageScope } from '../model/types';
import './UsageTokenSummary.css';

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
  const safeTotalTokens = Number.isFinite(totalTokens) ? Math.max(0, totalTokens) : 0;
  const safeRecordCount = Number.isFinite(recordCount) ? Math.max(0, recordCount) : 0;
  const formattedExactTokens = safeTotalTokens.toLocaleString('zh-CN');
  const formattedRecordCount = safeRecordCount.toLocaleString('zh-CN');
  const formattedAverageTokens = (safeRecordCount === 0 ? 0 : Math.round(safeTotalTokens / safeRecordCount)).toLocaleString('zh-CN');
  const scopeLabel = scope === 'admin' ? '全平台' : '个人';

  return (
    <section
      className="usage-token-summary"
      aria-label={`${scopeLabel}使用记录汇总`}
      aria-busy={loading}
      aria-live="polite"
    >
      {loading ? (
        <>
          <Skeleton className="usage-token-summary__skeleton usage-token-summary__skeleton--primary" active title={{ width: 112 }} paragraph={{ rows: 2, width: ['58%', '36%'] }} />
          <Skeleton className="usage-token-summary__skeleton" active title={{ width: 80 }} paragraph={{ rows: 2, width: ['42%', '64%'] }} />
        </>
      ) : (
        <>
          <div className="usage-token-summary__primary">
            <div className="usage-token-summary__heading">
              <div>
                <span className="usage-token-summary__eyebrow">TOKEN USAGE</span>
                <h2 className="usage-token-summary__label">实际 Token 总量</h2>
              </div>
              <span className="usage-token-summary__scope">{scopeLabel} / 当前筛选</span>
            </div>
            <div className="usage-token-summary__value-row mono-number">
              <strong className="usage-token-summary__value">{formattedExactTokens}</strong>
              <span className="usage-token-summary__unit">Token</span>
            </div>
            <span className="usage-token-summary__hint">基于当前筛选结果汇总，不受当前分页影响</span>
          </div>
          <div className="usage-token-summary__secondary">
            <span className="usage-token-summary__eyebrow">REQUESTS</span>
            <h2 className="usage-token-summary__label">记录总数</h2>
            <div className="usage-token-summary__count-row mono-number">
              <strong className="usage-token-summary__count">{formattedRecordCount}</strong>
              <span className="usage-token-summary__count-unit">条</span>
            </div>
            <span className="usage-token-summary__hint mono-number">平均 {formattedAverageTokens} Token / 条</span>
          </div>
        </>
      )}
    </section>
  );
}
