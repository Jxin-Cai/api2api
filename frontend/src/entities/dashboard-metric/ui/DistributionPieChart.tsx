import { Pie } from '@ant-design/charts';
import { Empty, Skeleton, Tooltip } from 'antd';
import { useMemo } from 'react';

import { useThemeStore } from '@shared/config/stores/useThemeStore';
import { formatTokenCompactParts } from '@shared/lib/formatters';
import './DistributionPieChart.css';

export interface DistributionItem { name: string; value: number }

interface Props { title: string; items: DistributionItem[]; loading?: boolean }

interface SliceItem extends DistributionItem {
  color: string;
  percentLabel: string;
}

interface PieDatum {
  name?: string;
  value?: number;
  percent?: number;
  percentLabel?: string;
}

const SLICE_COLORS = [
  '#c65746',
  '#10b981',
  '#d97706',
  '#2f6fed',
  '#8b5cf6',
  '#0e9f9f',
  '#d9539d',
  '#84994f',
  '#8b8680',
  '#b45309',
  '#2491b3',
  '#7863ff',
];

function resolveCssColor(variableName: string, fallback: string): string {
  if (typeof window === 'undefined') {
    return fallback;
  }
  const value = window.getComputedStyle(document.documentElement).getPropertyValue(variableName).trim();
  return value.length > 0 ? value : fallback;
}

function formatSharePercent(value: number, total: number): string {
  if (!Number.isFinite(value) || !Number.isFinite(total) || total <= 0) {
    return '0.0%';
  }
  return `${((value / total) * 100).toFixed(1)}%`;
}

function firstPieDatum(datum: PieDatum | PieDatum[]): PieDatum {
  if (Array.isArray(datum)) {
    return datum[0] ?? {};
  }
  return datum ?? {};
}

function resolveSliceName(item: { name?: string }): string {
  const name = item.name?.trim();
  return name && name.length > 0 ? name : '未命名';
}

function resolveSlicePercent(item: PieDatum, total: number): string {
  if (typeof item.percentLabel === 'string' && item.percentLabel.length > 0) {
    return item.percentLabel;
  }
  if (typeof item.percent === 'number' && Number.isFinite(item.percent)) {
    return `${(item.percent * 100).toFixed(1)}%`;
  }
  return formatSharePercent(Number(item.value ?? 0), total);
}

function escapeHtml(value: string): string {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;');
}

function formatDistributionTokens(tokens: number): string {
  if (!Number.isFinite(tokens)) {
    return '-';
  }
  const exact = Math.round(tokens).toLocaleString('zh-CN');
  const compact = formatTokenCompactParts(tokens);
  if (compact != null && tokens >= 1_000_000) {
    return `${exact}（${compact.value}${compact.unit}）`;
  }
  return exact;
}

interface TooltipRenderOptions {
  title?: string;
  items?: Array<{ name?: string; value?: string | number; d?: PieDatum; data?: PieDatum }>;
}

function resolveTooltipDatum(options: TooltipRenderOptions, slices: SliceItem[]): PieDatum {
  const item = options.items?.[0];
  const fromItem = item?.d ?? item?.data;
  const name = resolveSliceName(fromItem ?? { name: options.title ?? item?.name });
  const slice = slices.find((candidate) => candidate.name === name);
  if (slice) {
    return slice;
  }
  const rawValue = fromItem?.value ?? item?.value;
  return {
    name,
    value: typeof rawValue === 'number' ? rawValue : Number(rawValue ?? 0),
    percentLabel: fromItem?.percentLabel,
  };
}

function renderSliceTooltip(options: TooltipRenderOptions, slices: SliceItem[], total: number): string {
  const datum = resolveTooltipDatum(options, slices);
  const name = escapeHtml(resolveSliceName(datum));
  const tokens = escapeHtml(formatDistributionTokens(Number(datum.value ?? 0)));
  const percent = escapeHtml(resolveSlicePercent(datum, total));
  return `
    <div class="dashboard-distribution-tooltip">
      <div class="dashboard-distribution-tooltip__name">${name}</div>
      <div class="dashboard-distribution-tooltip__row">
        <span>Token</span>
        <span class="dashboard-distribution-tooltip__value">${tokens}</span>
      </div>
      <div class="dashboard-distribution-tooltip__row">
        <span>占比</span>
        <span class="dashboard-distribution-tooltip__value">${percent}</span>
      </div>
    </div>
  `.trim();
}

export function DistributionPieChart({ title, items, loading = false }: Props) {
  const themeMode = useThemeStore((state) => state.mode);
  const data = useMemo(
    () => items.filter((item) => item.value > 0).sort((a, b) => b.value - a.value).slice(0, 12),
    [items],
  );
  const total = useMemo(() => data.reduce((sum, item) => sum + item.value, 0), [data]);
  const slices = useMemo(
    (): SliceItem[] => data.map((item, index) => ({
      ...item,
      color: SLICE_COLORS[index % SLICE_COLORS.length],
      percentLabel: formatSharePercent(item.value, total),
    })),
    [data, total],
  );
  const chartTheme = useMemo(() => {
    const isDark = themeMode === 'dark';
    return {
      isDark,
      surface: resolveCssColor('--bg-surface', isDark ? '#20201f' : '#ffffff'),
    };
  }, [themeMode]);

  return (
    <div className="dashboard-distribution-card">
      <h3 className="dashboard-distribution-card__title">{title}</h3>
      {loading ? (
        <Skeleton active paragraph={{ rows: 5 }} />
      ) : data.length === 0 ? (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无分布数据" />
      ) : (
        <>
          <div className="dashboard-distribution-card__chart">
            <Pie
              key={themeMode}
              data={slices}
              angleField="value"
              colorField="name"
              height={220}
              autoFit
              margin={0}
              inset={8}
              theme={chartTheme.isDark ? 'classicDark' : 'classic'}
              scale={{ color: { range: slices.map((item) => item.color) } }}
              legend={false}
              label={false}
              tooltip={{
                title: '',
                items: [
                  (datum: PieDatum | PieDatum[]) => {
                    const item = firstPieDatum(datum);
                    return {
                      name: resolveSliceName(item),
                      value: `${formatDistributionTokens(Number(item.value ?? 0))} · ${resolveSlicePercent(item, total)}`,
                      d: item,
                    };
                  },
                ],
                position: 'top',
                bounding: 'viewport',
                offset: 12,
                css: {
                  '.g2-tooltip': {
                    'min-width': '220px',
                    'max-width': '360px',
                    'white-space': 'normal',
                  },
                  '.g2-tooltip-title': {
                    display: 'none',
                  },
                  '.g2-tooltip-list-item': {
                    'white-space': 'normal',
                    overflow: 'visible',
                  },
                  '.g2-tooltip-list-item-name': {
                    'max-width': 'none',
                    overflow: 'visible',
                    'text-overflow': 'unset',
                    'white-space': 'normal',
                    'word-break': 'break-word',
                  },
                  '.g2-tooltip-list-item-value': {
                    'max-width': 'none',
                    overflow: 'visible',
                    'text-overflow': 'unset',
                    'white-space': 'normal',
                  },
                },
                render: (_event: unknown, options: TooltipRenderOptions) => renderSliceTooltip(options, slices, total),
              }}
              style={{
                stroke: chartTheme.surface,
                lineWidth: 1,
              }}
            />
          </div>
          <ul className="dashboard-distribution-card__legend" aria-label={`${title}图例`}>
            {slices.map((item) => (
              <li key={item.name} className="dashboard-distribution-card__legend-item">
                <Tooltip title={`${resolveSliceName(item)}  ${formatDistributionTokens(item.value)}  ${item.percentLabel}`}>
                  <span className="dashboard-distribution-card__legend-hit">
                    <span className="dashboard-distribution-card__legend-swatch" style={{ background: item.color }} />
                    <span className="dashboard-distribution-card__legend-label">{resolveSliceName(item)}</span>
                  </span>
                </Tooltip>
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  );
}
