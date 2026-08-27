import { Line } from '@ant-design/charts';
import { Card, Empty, Spin } from 'antd';
import { useMemo } from 'react';

import { useThemeStore } from '@shared/config/stores/useThemeStore';
import type { TrendChartPoint } from '@shared/types/chart';
import './TrendChart.css';

/** 分类色板：≥10 色，明暗主题下均有足够对比度 */
const SERIES_COLORS = [
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
];

interface TrendChartProps {
  /** 图表数据 */
  data: TrendChartPoint[];
  /** X 字段 */
  xField?: string;
  /** Y 字段 */
  yField?: string;
  /** 多系列字段 */
  seriesField?: keyof TrendChartPoint;
  /** 图表高度 */
  height?: number;
  /** 是否加载中 */
  loading?: boolean;
  /** 空状态文案 */
  emptyText?: string;
  /** 数值单位（tooltip / Y 轴），默认 M */
  valueUnit?: string;
}

interface TrendSeries {
  name: string;
  color: string;
}

function resolveCssColor(variableName: string, fallback: string): string {
  if (typeof window === 'undefined') {
    return fallback;
  }
  const value = window.getComputedStyle(document.documentElement).getPropertyValue(variableName).trim();
  return value.length > 0 ? value : fallback;
}

function formatAxisValue(value: number, valueUnit: string): string {
  return `${value.toFixed(1)}${valueUnit}`;
}

function resolveSeriesName(point: TrendChartPoint, seriesField: keyof TrendChartPoint): string {
  const value = point[seriesField];
  return value == null ? '' : String(value);
}

export function TrendChart({
  data,
  xField = 'date',
  yField = 'value',
  seriesField = 'category',
  height = 260,
  loading = false,
  emptyText = '暂无趋势数据',
  valueUnit = 'M',
}: TrendChartProps) {
  const themeMode = useThemeStore((state) => state.mode);
  const series = useMemo((): TrendSeries[] => {
    const seen = new Set<string>();
    const items: TrendSeries[] = [];
    for (const point of data) {
      const name = resolveSeriesName(point, seriesField);
      if (seen.has(name)) {
        continue;
      }
      seen.add(name);
      items.push({
        name,
        color: SERIES_COLORS[items.length % SERIES_COLORS.length],
      });
    }
    return items;
  }, [data, seriesField]);

  const chartTheme = useMemo(() => {
    const isDark = themeMode === 'dark';
    return {
      isDark,
      axisLabel: resolveCssColor('--text-tertiary', isDark ? '#9c958d' : '#8a8378'),
      gridStroke: resolveCssColor('--border-color', isDark ? '#3a3530' : '#e7e2da'),
    };
  }, [themeMode]);

  if (loading) {
    return <Card className="trend-chart-card"><Spin style={{ width: '100%', minHeight: height }} /></Card>;
  }

  if (data.length === 0) {
    return <Card className="trend-chart-card"><Empty description={emptyText} /></Card>;
  }

  return (
    <Card className="trend-chart-card">
      <Line
        key={themeMode}
        data={data}
        xField={xField}
        yField={yField}
        colorField={seriesField}
        height={height}
        theme={chartTheme.isDark ? 'classicDark' : 'classic'}
        shapeField="smooth"
        point={{ sizeField: 3, shapeField: 'circle' }}
        scale={{ color: { domain: series.map((item) => item.name), range: series.map((item) => item.color) } }}
        legend={{ color: false }}
        axis={{
          x: {
            labelFill: chartTheme.axisLabel,
            lineStroke: chartTheme.gridStroke,
            labelAutoRotate: false,
          },
          y: {
            labelFill: chartTheme.axisLabel,
            gridStroke: chartTheme.gridStroke,
            labelFormatter: (value: number) => formatAxisValue(value, valueUnit),
          },
        }}
        tooltip={{
          items: [
            {
              channel: 'y',
              valueFormatter: (value: number) => formatAxisValue(value, valueUnit),
            },
          ],
        }}
      />
      {series.length > 0 ? (
        <ul className="trend-chart-legend" aria-label="趋势图图例">
          {series.map((item) => (
            <li key={item.name} className="trend-chart-legend__item" title={item.name}>
              <span className="trend-chart-legend__swatch" style={{ background: item.color }} />
              <span className="trend-chart-legend__label">{item.name || '未命名'}</span>
            </li>
          ))}
        </ul>
      ) : null}
    </Card>
  );
}
