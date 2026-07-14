import { lazy, Suspense, type ReactElement } from 'react';
import type { RouteObject } from 'react-router-dom';

import { RequireRole } from '@features/route-by-role';
import { PageState } from '@shared/ui';

const AdminUsersPage = lazy(() => import('@pages/AdminUsersPage'));
const AdminChannelsPage = lazy(() => import('@pages/AdminChannelsPage'));
const AdminModelMappingsPage = lazy(() => import('@pages/AdminModelMappingsPage'));
const AdminConversionsPage = lazy(() => import('@pages/AdminConversionsPage'));
const AdminProtocolMetadataPage = lazy(() => import('@pages/AdminProtocolMetadataPage'));

function withAdminGuard(element: ReactElement): ReactElement {
  return (
    <RequireRole role="ADMIN">
      <Suspense fallback={<PageState status="loading" title="加载后台页面" />}>
        {element}
      </Suspense>
    </RequireRole>
  );
}

export const adminRoutes: RouteObject[] = [
  { path: '/admin/users', element: withAdminGuard(<AdminUsersPage />) },
  { path: '/admin/channels', element: withAdminGuard(<AdminChannelsPage />) },
  { path: '/admin/model-mappings', element: withAdminGuard(<AdminModelMappingsPage />) },
  { path: '/admin/conversions', element: withAdminGuard(<AdminConversionsPage />) },
  { path: '/admin/protocol-metadata', element: withAdminGuard(<AdminProtocolMetadataPage />) },
];
