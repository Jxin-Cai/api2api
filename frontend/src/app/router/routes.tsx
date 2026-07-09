import { Navigate, Outlet, type RouteObject } from 'react-router-dom';
import { Suspense, type ReactElement } from 'react';

import { RequireAuth, RequireRole, RoleLandingRedirect } from '@features/route-by-role';
import { PageState, RouteErrorState } from '@shared/ui';
import { ROUTE_PATHS } from '@shared/config/constants';
import { AuthRouteLayout } from '@app/layouts/AuthRouteLayout';
import { ProtectedRouteLayout } from '@app/layouts/ProtectedRouteLayout';
import {
  AdminDashboardPage,
  AdminUsagePage,
  ApiKeysPage,
  AppDashboardPage,
  AppUsagePage,
} from './lazyPages';
import { adminRoutes } from './adminRoutes';
import LoginPage from '@pages/LoginPage';
import SettingsPage from '@pages/SettingsPage';
import NotFoundPage from '@pages/NotFoundPage';

function page(element: ReactElement): ReactElement {
  return <Suspense fallback={<PageState status="loading" title="加载页面中" />}>{element}</Suspense>;
}

export const routes: RouteObject[] = [
  {
    path: ROUTE_PATHS.root,
    element: <RoleLandingRedirect />,
  },
  {
    path: ROUTE_PATHS.login,
    element: <AuthRouteLayout />,
    children: [{ index: true, element: <LoginPage /> }],
  },
  {
    path: '/app',
    element: (
      <RequireAuth>
        <ProtectedRouteLayout portal="app" />
      </RequireAuth>
    ),
    errorElement: <RouteErrorState />,
    children: [
      { index: true, element: <Navigate to={ROUTE_PATHS.appDashboard} replace /> },
      { path: 'dashboard', element: page(<AppDashboardPage />) },
      { path: 'api-keys', element: page(<ApiKeysPage />) },
      { path: 'usage', element: page(<AppUsagePage />) },
      { path: 'settings', element: <SettingsPage /> },
    ],
  },
  {
    path: '/admin',
    element: (
      <RequireRole role="ADMIN">
        <ProtectedRouteLayout portal="admin" />
      </RequireRole>
    ),
    errorElement: <RouteErrorState />,
    children: [
      { index: true, element: <Navigate to={ROUTE_PATHS.adminDashboard} replace /> },
      { path: 'dashboard', element: page(<AdminDashboardPage />) },
      { path: 'usage', element: page(<AdminUsagePage />) },
      ...adminRoutes.map((route) => ({ ...route, path: String(route.path).replace(/^\/admin\//, '') })),
    ],
  },
  {
    path: '*',
    element: <Outlet />,
    children: [{ path: '*', element: <NotFoundPage /> }],
  },
];
