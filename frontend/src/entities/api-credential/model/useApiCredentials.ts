import { useQuery, type UseQueryResult } from '@tanstack/react-query';

import { listApiCredentials } from '../api/apiCredentialApi';
import type { ApiCredentialListResponse, ApiCredentialOption, ApiCredentialResponse } from './types';

export const API_CREDENTIALS_QUERY_KEY = ['apiCredentials'] as const;

export interface UseApiCredentialsResult {
  credentials: ApiCredentialResponse[];
  options: ApiCredentialOption[];
  query: UseQueryResult<ApiCredentialListResponse>;
}

export function useApiCredentials(): UseApiCredentialsResult {
  const query = useQuery({
    queryKey: API_CREDENTIALS_QUERY_KEY,
    queryFn: async (): Promise<ApiCredentialListResponse> => {
      const response = await listApiCredentials();
      return response.data;
    },
  });

  const credentials = query.data?.credentials ?? [];
  const options = credentials.map((credential: ApiCredentialResponse): ApiCredentialOption => ({
    label: credential.name || credential.id,
    value: credential.id,
  }));

  return { credentials, options, query };
}
