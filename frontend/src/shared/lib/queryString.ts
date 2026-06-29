import type { PageSize } from '@shared/types/common';

export type QueryPrimitive = string | number | boolean | null | undefined;
export type QueryObject = Record<string, QueryPrimitive | readonly QueryPrimitive[]>;

export function toQueryString(params: QueryObject): string {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]: [string, QueryPrimitive | readonly QueryPrimitive[]]): void => {
    const values: readonly QueryPrimitive[] = Array.isArray(value) ? value : [value];
    values.forEach((item: QueryPrimitive): void => {
      if (item !== undefined && item !== null && item !== '') {
        search.append(key, String(item));
      }
    });
  });
  return search.toString();
}

export function fromQueryString(query: string): Record<string, string | string[]> {
  const search = new URLSearchParams(query.startsWith('?') ? query.slice(1) : query);
  const result: Record<string, string | string[]> = {};
  search.forEach((value: string, key: string): void => {
    const existing = result[key];
    if (Array.isArray(existing)) {
      result[key] = [...existing, value];
    } else if (existing !== undefined) {
      result[key] = [existing, value];
    } else {
      result[key] = value;
    }
  });
  return result;
}

export function normalizePageSize(value: string | number | null | undefined): PageSize {
  const numeric = typeof value === 'string' ? Number(value) : value;
  return numeric === 100 || numeric === 200 ? numeric : 50;
}
