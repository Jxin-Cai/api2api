import type { ReactNode } from 'react';

interface PageHeaderProps {
  /** 页面标题 */
  title: string;
  /** 页面说明：当前统一不展示，保留兼容已有调用方 */
  description?: string;
  /** 右侧操作区 */
  extra?: ReactNode;
}

/** 统一页面页头 */
export function PageHeader({ title, extra }: PageHeaderProps) {
  return (
    <div className="app-page__head">
      <div>
        <h1 className="app-page__title">{title}</h1>
      </div>
      {extra ? <div>{extra}</div> : null}
    </div>
  );
}
