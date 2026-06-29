import { toQueryString, type QueryPrimitive } from './queryString';

type UsageQueryParams = Record<string, QueryPrimitive | readonly QueryPrimitive[]>;
type UsagePathScope = 'front' | 'admin';

function toQuery(filters: Partial<UsageQueryParams> = {}): string {
  const query = toQueryString(filters);
  return query ? `?${query}` : '';
}

export function buildUsagePath(scope: UsagePathScope, filters: Partial<UsageQueryParams> = {}): string {
  const basePath = scope === 'admin' ? '/admin/usage' : '/app/usage';
  return `${basePath}${toQuery(filters)}`;
}

export function buildAppUsageQuery(filters: Partial<UsageQueryParams> = {}): string {
  return toQuery(filters);
}

export function buildAdminUsageQuery(filters: Partial<UsageQueryParams> = {}): string {
  return toQuery(filters);
}
