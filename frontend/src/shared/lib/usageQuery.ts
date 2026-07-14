import { toQueryString, type QueryPrimitive } from './queryString';

type UsageQueryParams = Record<string, QueryPrimitive | readonly QueryPrimitive[]>;

function toQuery(filters: Partial<UsageQueryParams> = {}): string {
  const query = toQueryString(filters);
  return query ? `?${query}` : '';
}

export function buildAppUsageQuery(filters: Partial<UsageQueryParams> = {}): string {
  return toQuery(filters);
}
