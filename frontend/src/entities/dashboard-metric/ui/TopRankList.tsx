import { Empty, List, Skeleton, Typography } from 'antd';

import { SpotlightCard } from '@shared/ui';
import type { RankItem } from '@shared/types/chart';
import './TopRankList.css';

interface TopRankListProps {
  /** 排行数据 */
  items: RankItem[];
  /** 排行标题 */
  title: string;
  /** 是否加载中 */
  loading?: boolean;
  /** 点击排行项回调 */
  onItemClick?: (item: RankItem) => void;
}

export function TopRankList({ items, title, loading = false, onItemClick }: TopRankListProps) {
  return (
    <SpotlightCard className="top-rank-list">
      <div className="top-rank-list__head">{title}</div>
      {loading ? (
        <Skeleton active paragraph={{ rows: 10 }} />
      ) : items.length === 0 ? (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无排行数据" />
      ) : (
        <List
          size="small"
          dataSource={items.slice(0, 10)}
          renderItem={(item: RankItem, index: number) => (
            <List.Item
              className={onItemClick ? 'top-rank-list__item top-rank-list__item--clickable' : 'top-rank-list__item'}
              onClick={(): void => onItemClick?.(item)}
            >
              <span className="top-rank-list__rank">{index + 1}</span>
              <Typography.Text strong className="top-rank-list__label">{item.label}</Typography.Text>
              <Typography.Text className="top-rank-list__value mono-number">
                {item.unit?.startsWith('k') ? item.value.toFixed(1) : item.value} {item.unit ?? ''}
              </Typography.Text>
            </List.Item>
          )}
        />
      )}
    </SpotlightCard>
  );
}
