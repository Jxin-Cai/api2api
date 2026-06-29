import { useMemo } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { listProviderChannels } from '../api/providerChannelApi';

export const providerChannelQueryKeys = {
  all: ['provider-channels'] as const,
};

export interface UseProviderChannelsOptions {
  enabled?: boolean;
}

export function useProviderChannels(options: UseProviderChannelsOptions = {}) {
  const queryClient = useQueryClient();
  const query = useQuery({
    queryKey: providerChannelQueryKeys.all,
    queryFn: listProviderChannels,
    enabled: options.enabled ?? true,
  });
  const channels = query.data?.data.channels ?? [];
  const channelOptions = useMemo(
    () => channels.map((channel) => ({ label: channel.name, value: channel.id })),
    [channels]
  );

  function invalidateProviderChannels(): Promise<void> {
    return queryClient.invalidateQueries({ queryKey: providerChannelQueryKeys.all });
  }

  return { ...query, channels, channelOptions, invalidateProviderChannels };
}
