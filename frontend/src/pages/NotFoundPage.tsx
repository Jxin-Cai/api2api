import { Button, Result } from 'antd';
import { useNavigate } from 'react-router-dom';

import { ROUTE_PATHS } from '@shared/config/constants';
import { SpotlightCard } from '@shared/ui';

export default function NotFoundPage() {
  const navigate = useNavigate();
  return (
    <div className="app-page">
      <SpotlightCard style={{ padding: 24 }}>
        <Result
          status="404"
          title="页面不存在"
          subTitle="请检查访问地址，或返回仪表盘继续操作。"
          extra={<Button type="primary" onClick={(): void => { void navigate(ROUTE_PATHS.root); }}>返回首页</Button>}
        />
      </SpotlightCard>
    </div>
  );
}
