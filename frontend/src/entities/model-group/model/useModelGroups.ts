import { useQuery, type UseQueryResult } from '@tanstack/react-query';

import { listModelGroups } from '../api/modelGroupApi';
import type { ModelGroupListResponse, ModelGroupOption, ModelGroupResponse } from './types';

export const MODEL_GROUPS_QUERY_KEY = ['modelGroups'] as const;

export interface UseModelGroupsResult {
  groups: ModelGroupResponse[];
  options: ModelGroupOption[];
  query: UseQueryResult<ModelGroupListResponse>;
}

export function useModelGroups(): UseModelGroupsResult {
  const query = useQuery({
    queryKey: MODEL_GROUPS_QUERY_KEY,
    queryFn: async (): Promise<ModelGroupListResponse> => (await listModelGroups()).data,
  });
  const groups = query.data?.groups ?? [];
  const options = groups.map((group) => ({ label: group.name, value: group.id }));
  return { groups, options, query };
}
