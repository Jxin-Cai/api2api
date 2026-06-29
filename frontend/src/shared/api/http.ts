import type { QueryParams, QueryValue } from './types';

export function appendQuery(path: string, params?: QueryParams): string {
  if (!params) {
    return path;
  }

  const [pathname, existingQuery = ''] = path.split('?');
  const search = new URLSearchParams(existingQuery);
  Object.entries(params).forEach(([key, value]: [string, QueryValue | readonly QueryValue[]]): void => {
    const values: readonly QueryValue[] = Array.isArray(value) ? value : [value];
    values.forEach((item: QueryValue): void => {
      if (item !== undefined && item !== null && item !== '') {
        search.append(key, String(item));
      }
    });
  });

  const query = search.toString();
  return query ? `${pathname}?${query}` : pathname;
}

export function encodePathParam(value: string | number): string {
  return encodeURIComponent(String(value));
}

export function jsonBody(body?: unknown): BodyInit | undefined {
  return body === undefined ? undefined : JSON.stringify(body);
}
