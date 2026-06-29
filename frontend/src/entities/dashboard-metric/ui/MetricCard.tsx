import { Skeleton, Tooltip, Typography } from 'antd';

import { SpotlightCard } from '@shared/ui';
import type { MetricTrend } from '@shared/types/chart';
import './MetricCard.css';

interface MetricCardProps {
  /** 指标标题 */
  title: string;
  /** 主数值 */
  value: number | string | undefined;
  /** 单位 */
  unit?: string;
  /** 原始 token 值 */
  rawValue?: number;
  /** 趋势副信息 */
  trend?: MetricTrend;
  /** 是否加载中 */
  loading?: boolean;
}

function trendText(trend: MetricTrend): string {
  const prefix = trend.direction === 'up' ? '↑' : trend.direction === 'down' ? '↓' : '→';
  return `${prefix} ${trend.value}${trend.label ? ` ${trend.label}` : ''}`;
}

function parseDisplayValue(value: number | string | undefined): { numberValue?: number; suffix?: string; decimals?: number; text: string } {
  if (value === undefined || value === null || value === '') {
    return { text: '--' };
  }
  if (typeof value === 'number') {
    return { numberValue: value, decimals: Number.isInteger(value) ? 0 : 2, text: String(value) };
  }
  const match = value.match(/^(-?\d+(?:\.\d+)?)(.*)$/);
  if (!match) {
    return { text: value };
  }
  return { numberValue: Number(match[1]), suffix: match[2], decimals: match[1].split('.')[1]?.length ?? 0, text: value };
}

export function MetricCard({ title, value, unit, rawValue, trend, loading = false }: MetricCardProps) {
  const display = parseDisplayValue(value);

  return (
    <Tooltip title={rawValue !== undefined ? `原始值：${rawValue}` : undefined}>
      <SpotlightCard className="metric-card">
        {loading ? (
          <Skeleton active paragraph={{ rows: 2 }} />
        ) : (
          <>
            <Typography.Text className="metric-card__title">{title}</Typography.Text>
            <div className="metric-card__value mono-number">
              {display.numberValue !== undefined ? `${display.numberValue.toFixed(display.decimals ?? 0)}${display.suffix ?? ''}` : display.text}
              {unit ? <span className="metric-card__unit">{unit}</span> : null}
            </div>
            {trend ? <Typography.Text className="metric-card__trend">{trendText(trend)}</Typography.Text> : null}
          </>
        )}
      </SpotlightCard>
    </Tooltip>
  );
}
