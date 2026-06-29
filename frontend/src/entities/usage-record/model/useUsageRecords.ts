import { useQuery, type UseQueryResult } from '@tanstack/react-query';

import { queryAdminUsageRecords, queryMyUsageRecords } from '../api/usageRecordApi';
import type {
  QueryAdminUsageRecordsRequest,
  QueryMyUsageRecordsRequest,
  UsageRecordPageResponse,
  UsageScope,
} from './types';

export const USAGE_RECORDS_QUERY_KEY = 'usageRecords';

export interface UseUsageRecordsParams {
  scope: UsageScope;
  filters: QueryMyUsageRecordsRequest | QueryAdminUsageRecordsRequest;
}

export function useUsageRecords(
  params: UseUsageRecordsParams
): UseQueryResult<UsageRecordPageResponse> {
  return useQuery({
    queryKey: [USAGE_RECORDS_QUERY_KEY, params.scope, params.filters],
    queryFn: async (): Promise<UsageRecordPageResponse> => {
      const response = params.scope === 'admin'
        ? await queryAdminUsageRecords(params.filters as QueryAdminUsageRecordsRequest)
        : await queryMyUsageRecords(params.filters as QueryMyUsageRecordsRequest);
      return response.data;
    },
  });
}
