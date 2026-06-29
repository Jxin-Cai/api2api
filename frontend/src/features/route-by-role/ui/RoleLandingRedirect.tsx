import { Navigate, useSearchParams } from 'react-router-dom';

import { useCurrentUser } from '@entities/user-account';
import { ROUTE_PATHS } from '@shared/config/constants';
import { PageState } from '@shared/ui';

function isSafeRedirect(value: string | null): value is string {
  return Boolean(value && value.startsWith('/') && !value.startsWith('//'));
}

export function RoleLandingRedirect() {
  const [searchParams] = useSearchParams();
  const { user, isLoading, isError } = useCurrentUser();

  if (isLoading) {
    return <PageState status="loading" title="加载用户信息" />;
  }

  if (isError || !user) {
    return <Navigate to={ROUTE_PATHS.login} replace />;
  }

  const redirect = searchParams.get('redirect');
  if (isSafeRedirect(redirect)) {
    return <Navigate to={redirect} replace />;
  }

  return <Navigate to={user.role === 'ADMIN' ? ROUTE_PATHS.adminDashboard : ROUTE_PATHS.appDashboard} replace />;
}
