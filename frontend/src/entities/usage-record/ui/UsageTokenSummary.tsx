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
  const tokenMillions = safeTotalTokens / 1_000_000;
  const tokenDecimals = tokenMillions >= 100 ? 1 : tokenMillions >= 1 ? 2 : 3;
  const formattedTokens = tokenMillions.toLocaleString('zh-CN', {
    minimumFractionDigits: 1,
    maximumFractionDigits: tokenDecimals,
  });
  const formattedExactTokens = safeTotalTokens.toLocaleString('zh-CN');
  const formattedRecordCount = safeRecordCount.toLocaleString('zh-CN');
  const scopeLabel = scope === 'admin' ? '全平台' : '个人';

  return (
    <section className="usage-token-summary" aria-label={`${scopeLabel}使用记录汇总`}>
      {loading ? (
        <Skeleton active title={{ width: 120 }} paragraph={{ rows: 1, width: ['62%'] }} />
      ) : (
        <>
          <div className="usage-token-summary__primary">
            <div className="usage-token-summary__heading">
              <span className="usage-token-summary__label">实际 Token 总量</span>
              <span className="usage-token-summary__scope">{scopeLabel} · 当前筛选</span>
            </div>
            <div className="usage-token-summary__value-row mono-number">
              <strong className="usage-token-summary__value">{formattedTokens}</strong>
              <span className="usage-token-summary__unit">M</span>
            </div>
            <span className="usage-token-summary__exact mono-number">精确值 {formattedExactTokens} Token</span>
          </div>
          <div className="usage-token-summary__secondary">
            <span className="usage-token-summary__label">记录总数</span>
            <div className="usage-token-summary__count-row mono-number">
              <strong className="usage-token-summary__count">{formattedRecordCount}</strong>
              <span className="usage-token-summary__count-unit">条</span>
            </div>
            <span className="usage-token-summary__hint">符合当前筛选条件</span>
          </div>
        </>
      )}
    </section>
  );
}
