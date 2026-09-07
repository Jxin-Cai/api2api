import { Pie } from '@ant-design/charts';
import { Empty, Skeleton } from 'antd';
import { useMemo } from 'react';

import { useThemeStore } from '@shared/config/stores/useThemeStore';
import './DistributionPieChart.css';

export interface DistributionItem { name: string; value: number }

interface Props { title: string; items: DistributionItem[]; loading?: boolean }

function resolveCssColor(variableName: string, fallback: string): string {
  if (typeof window === 'undefined') {
    return fallback;
  }
  const value = window.getComputedStyle(document.documentElement).getPropertyValue(variableName).trim();
  return value.length > 0 ? value : fallback;
}

export function DistributionPieChart({ title, items, loading = false }: Props) {
  const themeMode = useThemeStore((state) => state.mode);
  const data = useMemo(
    () => items.filter((item) => item.value > 0).sort((a, b) => b.value - a.value).slice(0, 12),
    [items],
  );
  const chartTheme = useMemo(() => {
    const isDark = themeMode === 'dark';
    return {
      isDark,
      label: resolveCssColor('--text-primary', isDark ? '#f6f4f1' : '#20201f'),
      secondary: resolveCssColor('--text-secondary', isDark ? '#c9c3bb' : '#62625f'),
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
        <Pie
          key={themeMode}
          data={data}
          angleField="value"
          colorField="name"
          height={240}
          theme={chartTheme.isDark ? 'classicDark' : 'classic'}
          legend={{
            color: {
              position: 'right',
              itemLabelFill: chartTheme.label,
              itemValueFill: chartTheme.secondary,
              navButtonFill: chartTheme.secondary,
              navPageNumFill: chartTheme.secondary,
              titleFill: chartTheme.label,
            },
          }}
          tooltip={{ items: [{ channel: 'value' }] }}
        />
      )}
    </div>
  );
}
