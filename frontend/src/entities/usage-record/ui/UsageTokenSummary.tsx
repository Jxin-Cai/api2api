import { Skeleton } from 'antd';

import { formatTokenMillionsValue } from '@shared/lib/formatters';

import type { UsageScope } from '../model/types';
import './UsageTokenSummary.css';

interface UsageTokenSummaryProps {
  /** 过滤后的总 Token 汇总 */
  totalTokens: number;
  /** 过滤后的实际 Token 汇总 */
  actualTokens: number;
  /** 当前筛选记录总数 */
  recordCount?: number;
  /** 前台或后台范围 */
  scope: UsageScope;
  /** 是否加载中 */
  loading?: boolean;
}

function toSafeAmount(value: number): number {
  return Number.isFinite(value) ? Math.max(0, value) : 0;
}

function averageTokens(total: number, recordCount: number): string {
  return (recordCount === 0 ? 0 : Math.round(total / recordCount)).toLocaleString('zh-CN');
}

export function UsageTokenSummary({
  totalTokens,
  actualTokens,
  recordCount = 0,
  scope,
  loading = false,
}: UsageTokenSummaryProps) {
  const safeTotalTokens = toSafeAmount(totalTokens);
  const safeActualTokens = toSafeAmount(actualTokens);
  const safeRecordCount = toSafeAmount(recordCount);
  const formattedTotalTokens = formatTokenMillionsValue(safeTotalTokens);
  const formattedActualTokens = formatTokenMillionsValue(safeActualTokens);
  const formattedRecordCount = safeRecordCount.toLocaleString('zh-CN');
  const formattedAverageActualTokens = averageTokens(safeActualTokens, safeRecordCount);
  const formattedAverageTotalTokens = averageTokens(safeTotalTokens, safeRecordCount);
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
          <Skeleton className="usage-token-summary__skeleton" active title={{ width: 96 }} paragraph={{ rows: 2, width: ['48%', '40%'] }} />
          <Skeleton className="usage-token-summary__skeleton" active title={{ width: 80 }} paragraph={{ rows: 2, width: ['42%', '64%'] }} />
        </>
      ) : (
        <>
          <div className="usage-token-summary__primary">
            <div className="usage-token-summary__heading">
              <div>
                <span className="usage-token-summary__eyebrow">ACTUAL TOKENS</span>
                <h2 className="usage-token-summary__label">实际 Token 总量</h2>
              </div>
              <span className="usage-token-summary__scope">{scopeLabel} / 当前筛选</span>
            </div>
            <div className="usage-token-summary__value-row mono-number">
              <strong className="usage-token-summary__value">{formattedActualTokens}</strong>
              <span className="usage-token-summary__unit">M</span>
            </div>
            <span className="usage-token-summary__hint mono-number">平均 {formattedAverageActualTokens} Token / 条</span>
          </div>
          <div className="usage-token-summary__metric">
            <span className="usage-token-summary__eyebrow">TOTAL TOKENS</span>
            <h2 className="usage-token-summary__label">总 Token 总量</h2>
            <div className="usage-token-summary__value-row mono-number">
              <strong className="usage-token-summary__value usage-token-summary__value--compact">{formattedTotalTokens}</strong>
              <span className="usage-token-summary__unit">M</span>
            </div>
            <span className="usage-token-summary__hint mono-number">平均 {formattedAverageTotalTokens} Token / 条</span>
          </div>
          <div className="usage-token-summary__secondary">
            <span className="usage-token-summary__eyebrow">REQUESTS</span>
            <h2 className="usage-token-summary__label">记录总数</h2>
            <div className="usage-token-summary__count-row mono-number">
              <strong className="usage-token-summary__count">{formattedRecordCount}</strong>
              <span className="usage-token-summary__count-unit">条</span>
            </div>
            <span className="usage-token-summary__hint">基于当前筛选结果汇总，不受当前分页影响</span>
          </div>
        </>
      )}
    </section>
  );
}
