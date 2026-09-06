import { Pie } from '@ant-design/charts';
import { Empty, Skeleton, Typography } from 'antd';
import { useMemo } from 'react';

export interface DistributionItem { name: string; value: number }

interface Props { title: string; items: DistributionItem[]; loading?: boolean }

export function DistributionPieChart({ title, items, loading = false }: Props) {
  const data = useMemo(() => items.filter((item) => item.value > 0).sort((a, b) => b.value - a.value).slice(0, 12), [items]);
  return <div className="dashboard-distribution-card">
    <Typography.Title level={5}>{title}</Typography.Title>
    {loading ? <Skeleton active paragraph={{ rows: 5 }} /> : data.length === 0 ? <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无分布数据" /> : <Pie data={data} angleField="value" colorField="name" height={240} legend={{ position: 'right' }} tooltip={{ items: [{ channel: 'value' }] }} />}
  </div>;
}
