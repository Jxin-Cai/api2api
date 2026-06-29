export type UsagePageSize = 50 | 100 | 200;

export interface PageRequest {
  page: number;
  pageSize: UsagePageSize;
}

export interface PageResponse<TItem> {
  records: TItem[];
  total: number;
  page: number;
  pageSize: UsagePageSize;
}

export interface TablePaginationState extends PageRequest {
  total: number;
}

export type TableQueryStatus = 'idle' | 'loading' | 'success' | 'error';

export interface SortState {
  field?: string;
  order?: 'ascend' | 'descend';
}
