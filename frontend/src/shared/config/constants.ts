export const ROUTE_PATHS = {
  root: '/',
  login: '/login',
  appDashboard: '/app/dashboard',
  appApiKeys: '/app/api-keys',
  appUsage: '/app/usage',
  appSettings: '/app/settings',
  adminDashboard: '/admin/dashboard',
  adminUsers: '/admin/users',
  adminChannels: '/admin/channels',
  adminConversions: '/admin/conversions',
  adminUsage: '/admin/usage',
} as const;

export const PAGE_SIZE_OPTIONS = [50, 100, 200] as const;

export const USER_ROLE_LABELS: Record<string, string> = {
  ADMIN: '管理员',
  USER: '普通用户',
};

export const USER_STATUS_LABELS: Record<string, string> = {
  ACTIVE: '启用',
  ENABLED: '启用',
  DISABLED: '禁用',
};

