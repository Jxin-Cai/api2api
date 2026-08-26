import { useMutation } from '@tanstack/react-query';
import {
  submitChannelEvaluations,
  upsertChannelEvaluationSchedule,
  useInvalidateChannelEvaluations,
  type AdminSubmitChannelEvaluationRequest,
  type AdminUpsertChannelEvaluationScheduleRequest,
} from '@entities/channel-evaluation';

export function useChannelEvaluationMutations(providerChannelId: number) {
  const { invalidate } = useInvalidateChannelEvaluations();

  const submitMutation = useMutation({
    mutationFn: (body: AdminSubmitChannelEvaluationRequest) => submitChannelEvaluations(providerChannelId, body),
    onSuccess: () => invalidate(providerChannelId),
  });
  const upsertScheduleMutation = useMutation({
    mutationFn: (body: AdminUpsertChannelEvaluationScheduleRequest) =>
      upsertChannelEvaluationSchedule(providerChannelId, body),
    onSuccess: () => invalidate(providerChannelId),
  });

  return { submitMutation, upsertScheduleMutation };
}
