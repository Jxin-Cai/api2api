export const API_SUCCESS_CODE = 'SUCCESS';

export interface ApiResponse<T> {
  code: string;
  message: string;
  data: T;
}

export interface ApiErrorShape {
  status: number;
  code: string;
  message: string;
}

export interface PageRequest {
  page: number;
  pageSize: 50 | 100 | 200;
}

export interface PageResponse<TItem> {
  records: TItem[];
  total: number;
  page: number;
  pageSize: 50 | 100 | 200;
}

export type { QueryPrimitive as QueryValue, QueryObject as QueryParams } from '../lib/queryString';
