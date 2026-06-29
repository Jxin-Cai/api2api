import type { ReactNode } from 'react';
import { Button, Result } from 'antd';
import { Navigate, useNavigate } from 'react-router-dom';

import { useCurrentUser } from '@entities/user-account';
import { ROUTE_PATHS } from '@shared/config/constants';
import { PageState } from '@shared/ui';

interface RequireRoleProps {
  /** 允许访问的角色 */
  role: 'ADMIN' | 'USER';
  /** 子节点 */
  children: ReactNode;
}

export function RequireRole({ role, children }: RequireRoleProps) {
  const navigate = useNavigate();
  const { user, isLoading, isError } = useCurrentUser();

  if (isLoading) {
    return <PageState status="loading" title="校验权限" />;
  }

  if (isError || !user) {
    return <Navigate to={ROUTE_PATHS.login} replace />;
  }

  if (user.role !== role) {
    return (
      <Result
        status="403"
        title="无权访问"
        subTitle="当前账号没有访问该页面的权限。"
        extra={<Button type="primary" onClick={(): void => { void navigate(ROUTE_PATHS.appDashboard); }}>返回前台</Button>}
      />
    );
  }

  return <>{children}</>;
}
