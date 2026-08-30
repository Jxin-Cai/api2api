import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';

import { listProviderModels } from '../api/providerModelApi';

export const providerModelQueryKeys = {
  all: ['provider-models'] as const,
};

export interface UseProviderModelsOptions {
  enabled?: boolean;
}

export function useProviderModels(options: UseProviderModelsOptions = {}) {
  const query = useQuery({
    queryKey: providerModelQueryKeys.all,
    queryFn: listProviderModels,
    enabled: options.enabled ?? true,
  });
  const models = query.data?.data.models ?? [];
  const modelOptions = useMemo(
    () => models.map((model) => ({
      label: `${model.model}${model.providerCount > 1 ? `（${model.providerCount} 个渠道）` : ''}`,
      value: model.model,
    })),
    [models]
  );

  return { ...query, models, modelOptions };
}
