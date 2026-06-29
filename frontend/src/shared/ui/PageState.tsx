import { Button, Empty, Result, Spin } from 'antd';

interface PageStateProps {
  /** 页面状态类型 */
  status: 'loading' | 'empty' | 'error';
  /** 状态标题 */
  title?: string;
  /** 状态描述 */
  description?: string;
  /** 重试回调 */
  onRetry?: () => void;
}

export function PageState({ status, title, description, onRetry }: PageStateProps) {
  if (status === 'loading') {
    return <Spin tip={title ?? '加载中'} />;
  }
  if (status === 'empty') {
    return <Empty description={description ?? title ?? '暂无数据'} />;
  }
  return (
    <Result
      status="error"
      title={title ?? '加载失败'}
      subTitle={description}
      extra={onRetry ? <Button onClick={onRetry}>重试</Button> : undefined}
    />
  );
}
