import { lazyWithRetry } from './lazyWithRetry';

export const AppDashboardPage = lazyWithRetry(() => import('@pages/AppDashboardPage'));
export const ApiKeysPage = lazyWithRetry(() => import('@pages/ApiKeysPage'));
export const AppUsagePage = lazyWithRetry(() => import('@pages/AppUsagePage'));
export const AdminDashboardPage = lazyWithRetry(() => import('@pages/AdminDashboardPage'));
export const AdminUsagePage = lazyWithRetry(() => import('@pages/AdminUsagePage'));
