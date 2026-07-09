import { Button, Result, Space } from 'antd';
import { useRouteError } from 'react-router-dom';
import { isChunkLoadError } from '@shared/lib';
import { ROUTE_PATHS } from '@shared/config/constants';

export function RouteErrorState() {
  const error = useRouteError();

  if (isChunkLoadError(error)) {
    return (
      <div className="app-page" style={{ display: 'flex', justifyContent: 'center', paddingTop: 120 }}>
        <Result
          status="info"
          title="页面资源已更新"
          subTitle="检测到应用版本已更新，请刷新页面后继续使用。"
          extra={
            <Space>
              <Button type="primary" onClick={() => window.location.reload()}>
                刷新页面
              </Button>
              <Button onClick={() => window.location.assign(ROUTE_PATHS.root)}>返回首页</Button>
            </Space>
          }
        />
      </div>
    );
  }

  const message =
    import.meta.env.DEV && error instanceof Error ? error.message : '请稍后重试或刷新页面。';

  return (
    <div className="app-page" style={{ display: 'flex', justifyContent: 'center', paddingTop: 120 }}>
      <Result
        status="error"
        title="页面加载失败"
        subTitle={message}
        extra={
          <Space>
            <Button type="primary" onClick={() => window.location.reload()}>
              刷新页面
            </Button>
            <Button onClick={() => window.location.assign(ROUTE_PATHS.root)}>返回首页</Button>
          </Space>
        }
      />
    </div>
  );
}
