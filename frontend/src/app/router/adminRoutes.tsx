import { Suspense, type ReactElement } from 'react';
import { Navigate, useLocation, type RouteObject } from 'react-router-dom';

import { RequireRole } from '@features/route-by-role';
import { PageState } from '@shared/ui';

import { lazyWithRetry } from './lazyWithRetry';

const AdminOperationsPage = lazyWithRetry(() => import('@pages/AdminOperationsPage'));
const AdminUsersPage = lazyWithRetry(() => import('@pages/AdminUsersPage'));
const AdminChannelsPage = lazyWithRetry(() => import('@pages/AdminChannelsPage'));
const AdminModelMappingsPage = lazyWithRetry(() => import('@pages/AdminModelMappingsPage'));
const AdminConversionsPage = lazyWithRetry(() => import('@pages/AdminConversionsPage'));

function withAdminGuard(element: ReactElement): ReactElement {
  return (
    <RequireRole role="ADMIN">
      <Suspense fallback={<PageState status="loading" title="加载后台页面" />}>
        {element}
      </Suspense>
    </RequireRole>
  );
}

function LegacyProtocolMetadataRedirect(): ReactElement {
  const { search } = useLocation();
  return <Navigate replace to={`/admin/conversions${search}`} />;
}

export const adminRoutes: RouteObject[] = [
  { path: '/admin/operations', element: withAdminGuard(<AdminOperationsPage />) },
  { path: '/admin/users', element: withAdminGuard(<AdminUsersPage />) },
  { path: '/admin/channels', element: withAdminGuard(<AdminChannelsPage />) },
  { path: '/admin/model-mappings', element: withAdminGuard(<AdminModelMappingsPage />) },
  { path: '/admin/conversions', element: withAdminGuard(<AdminConversionsPage />) },
  { path: '/admin/protocol-metadata', element: withAdminGuard(<LegacyProtocolMetadataRedirect />) },
];
