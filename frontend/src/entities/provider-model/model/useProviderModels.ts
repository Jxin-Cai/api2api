import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';

import { listProviderModels } from '../api/providerModelApi';
import type { ProviderModelOptionResponse } from '@shared/api/contracts';

export const providerModelQueryKeys = {
  all: ['provider-models'] as const,
};

export interface UseProviderModelsOptions {
  enabled?: boolean;
}

export function useProviderModels(options: UseProviderModelsOptions = {}) {
  const query = useQuery({
    queryKey: providerModelQueryKeys.all,
    queryFn: async (): Promise<ProviderModelOptionResponse[]> => {
      const response = await listProviderModels();
      return Array.isArray(response.data?.models) ? response.data.models : [];
    },
    enabled: options.enabled ?? true,
  });
  const models = query.data ?? [];
  const modelOptions = useMemo(
    () => models.map((model) => ({
      label: `${model.model}${model.providerCount > 1 ? `（${model.providerCount} 个渠道）` : ''}`,
      value: model.model,
    })),
    [models]
  );

  return { ...query, models, modelOptions };
}
