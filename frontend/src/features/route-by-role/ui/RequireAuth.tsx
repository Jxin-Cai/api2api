import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';

import { useCurrentUser } from '@entities/user-account';
import { ROUTE_PATHS } from '@shared/config/constants';
import { PageState } from '@shared/ui';

interface RequireAuthProps {
  children: ReactNode;
}

export function RequireAuth({ children }: RequireAuthProps) {
  const location = useLocation();
  const { user, isLoading, isError, refetch } = useCurrentUser();

  if (isLoading) {
    return <PageState status="loading" title="校验登录态" />;
  }

  if (isError) {
    return <Navigate to={`${ROUTE_PATHS.login}?redirect=${encodeURIComponent(location.pathname + location.search)}`} replace />;
  }

  if (!user) {
    return <Navigate to={`${ROUTE_PATHS.login}?redirect=${encodeURIComponent(location.pathname + location.search)}`} replace />;
  }

  if (String(user.status).toUpperCase() === 'DISABLED') {
    return <PageState status="error" title="账号已禁用" description="请联系管理员启用账号" onRetry={(): void => { refetch().catch(() => undefined); }} />;
  }

  return <>{children}</>;
}
