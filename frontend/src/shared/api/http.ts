import { toQueryString } from '../lib/queryString';
import type { QueryParams } from './types';

export function appendQuery(path: string, params?: QueryParams): string {
  if (!params) {
    return path;
  }

  const [pathname, existingQuery = ''] = path.split('?');
  const query = toQueryString(params, existingQuery);
  return query ? `${pathname}?${query}` : pathname;
}

export function encodePathParam(value: string | number): string {
  return encodeURIComponent(String(value));
}

export function jsonBody(body?: unknown): BodyInit | undefined {
  return body === undefined ? undefined : JSON.stringify(body);
}
