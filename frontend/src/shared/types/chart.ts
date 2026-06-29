export interface TrendChartPoint {
  date: string;
  value: number;
  category?: string;
}

export interface RankItem {
  id: string;
  label: string;
  value: number;
  unit?: string;
  meta?: string;
}

export interface ChartSeries<TPoint extends TrendChartPoint = TrendChartPoint> {
  name: string;
  points: TPoint[];
}

export interface MetricTrend {
  value: number;
  direction: 'up' | 'down' | 'flat';
  label?: string;
}
