import { ROUTE_PATHS } from '@shared/config/constants';
import type { PortalType } from '@shared/types/portal';

export interface AppMenuItem {
  key: string;
  label: string;
  path: string;
  portal: PortalType;
}

export const appMenuItems: AppMenuItem[] = [
  { key: 'app-dashboard', label: '前台仪表盘', path: ROUTE_PATHS.appDashboard, portal: 'app' },
  { key: 'app-api-keys', label: 'API Key', path: ROUTE_PATHS.appApiKeys, portal: 'app' },
  { key: 'app-usage', label: '使用记录', path: ROUTE_PATHS.appUsage, portal: 'app' },
];

export const adminMenuItems: AppMenuItem[] = [
  { key: 'admin-dashboard', label: '后台仪表盘', path: ROUTE_PATHS.adminDashboard, portal: 'admin' },
  { key: 'admin-usage', label: '使用记录', path: ROUTE_PATHS.adminUsage, portal: 'admin' },
  { key: 'admin-users', label: '用户管理', path: ROUTE_PATHS.adminUsers, portal: 'admin' },
  { key: 'admin-channels', label: '供应商渠道', path: ROUTE_PATHS.adminChannels, portal: 'admin' },
  { key: 'admin-conversions', label: '协议转换', path: ROUTE_PATHS.adminConversions, portal: 'admin' },
];

export function getMenuItems(portal: PortalType): AppMenuItem[] {
  return portal === 'admin' ? adminMenuItems : appMenuItems;
}

export function getActiveMenuKey(pathname: string): string {
  const allItems = [...appMenuItems, ...adminMenuItems];
  return allItems.find((item: AppMenuItem): boolean => pathname.startsWith(item.path))?.key ?? 'app-dashboard';
}
