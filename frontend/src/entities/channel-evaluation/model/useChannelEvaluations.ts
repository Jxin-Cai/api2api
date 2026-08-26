import { useQuery, useQueryClient } from '@tanstack/react-query';
import { listChannelEvaluations, loadChannelEvaluationSchedule } from '../api/channelEvaluationApi';
import type { QueryChannelEvaluationHistoryRequest } from '../model/types';

export const channelEvaluationQueryKeys = {
  all: ['channel-evaluations'] as const,
  history: (providerChannelId: number, params: QueryChannelEvaluationHistoryRequest) =>
    [...channelEvaluationQueryKeys.all, 'history', providerChannelId, params] as const,
  schedule: (providerChannelId: number) =>
    [...channelEvaluationQueryKeys.all, 'schedule', providerChannelId] as const,
};

export function useChannelEvaluationHistory(
  providerChannelId: number,
  params: QueryChannelEvaluationHistoryRequest = {},
  options: { enabled?: boolean; refetchInterval?: number } = {}
) {
  const query = useQuery({
    queryKey: channelEvaluationQueryKeys.history(providerChannelId, params),
    queryFn: () => listChannelEvaluations(providerChannelId, params),
    enabled: options.enabled ?? true,
    refetchInterval: options.refetchInterval,
  });
  return {
    ...query,
    evaluations: query.data?.data.evaluations ?? [],
    summary: query.data?.data.summary,
    totalElements: query.data?.data.totalElements ?? 0,
  };
}

export function useChannelEvaluationSchedule(
  providerChannelId: number,
  options: { enabled?: boolean } = {}
) {
  const query = useQuery({
    queryKey: channelEvaluationQueryKeys.schedule(providerChannelId),
    queryFn: () => loadChannelEvaluationSchedule(providerChannelId),
    enabled: options.enabled ?? true,
  });
  return {
    ...query,
    schedule: query.data?.data ?? null,
  };
}

export function useInvalidateChannelEvaluations() {
  const queryClient = useQueryClient();
  return {
    invalidate(providerChannelId?: number): Promise<void> {
      if (providerChannelId == null) {
        return queryClient.invalidateQueries({ queryKey: channelEvaluationQueryKeys.all });
      }
      return queryClient.invalidateQueries({
        predicate: (query) => query.queryKey[0] === 'channel-evaluations' && query.queryKey[2] === providerChannelId,
      });
    },
  };
}
