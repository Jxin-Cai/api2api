import { Line } from '@ant-design/charts';
import { Card, Empty, Spin } from 'antd';

import type { TrendChartPoint } from '@shared/types/chart';
import './TrendChart.css';

interface TrendChartProps {
  /** 图表数据 */
  data: TrendChartPoint[];
  /** X 字段 */
  xField?: string;
  /** Y 字段 */
  yField?: string;
  /** 多系列字段 */
  seriesField?: string;
  /** 图表高度 */
  height?: number;
  /** 是否加载中 */
  loading?: boolean;
  /** 空状态文案 */
  emptyText?: string;
}

export function TrendChart({ data, xField = 'date', yField = 'value', seriesField = 'category', height = 260, loading = false, emptyText = '暂无趋势数据' }: TrendChartProps) {
  if (loading) {
    return <Card className="trend-chart-card"><Spin style={{ width: '100%', minHeight: height }} /></Card>;
  }

  if (data.length === 0) {
    return <Card className="trend-chart-card"><Empty description={emptyText} /></Card>;
  }

  return (
    <Card className="trend-chart-card">
      <Line
        data={data}
        xField={xField}
        yField={yField}
        seriesField={seriesField}
        height={height}
        smooth
        point={{ size: 3, shape: 'circle' }}
        color={['#8b8680', '#c65746', '#10b981', '#d97706', '#6d6760']}
        axis={{
          x: { labelFill: 'var(--text-tertiary)', lineStroke: 'var(--border-color)' },
          y: { labelFill: 'var(--text-tertiary)', gridStroke: 'var(--border-color)' },
        }}
        tooltip={{ showMarkers: true }}
        legend={{ color: { itemLabelFill: 'var(--text-secondary)' } }}
      />
    </Card>
  );
}
