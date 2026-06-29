import { Outlet } from 'react-router-dom';

import { AuthLayout } from '@widgets/auth-layout';

export function AuthRouteLayout() {
  return (
    <AuthLayout>
      <Outlet />
    </AuthLayout>
  );
}
