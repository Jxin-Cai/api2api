import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  createProviderChannel,
  deleteProviderChannel,
  disableProviderChannel,
  enableProviderChannel,
  providerChannelQueryKeys,
  resetAllProviderChannelRateLimits,
  updateProviderChannel,
} from '@entities/provider-channel';
import type { AdminUpdateProviderChannelRequest } from '@entities/provider-channel';
import { MODEL_GROUPS_QUERY_KEY } from '@entities/model-group';
import { providerModelQueryKeys } from '@entities/provider-model';

export function useProviderChannelMutations() {
  const queryClient = useQueryClient();
  async function invalidate(): Promise<void> {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: providerChannelQueryKeys.all }),
      queryClient.invalidateQueries({ queryKey: providerModelQueryKeys.all }),
      queryClient.invalidateQueries({ queryKey: MODEL_GROUPS_QUERY_KEY }),
    ]);
  }

  const createMutation = useMutation({ mutationFn: createProviderChannel, onSuccess: invalidate });
  const updateMutation = useMutation({
    mutationFn: (params: { id: number; body: AdminUpdateProviderChannelRequest }) => updateProviderChannel(params.id, params.body),
    onSuccess: invalidate,
  });
  const enableMutation = useMutation({ mutationFn: enableProviderChannel, onSuccess: invalidate });
  const disableMutation = useMutation({ mutationFn: disableProviderChannel, onSuccess: invalidate });
  const deleteMutation = useMutation({ mutationFn: deleteProviderChannel, onSuccess: invalidate });
  const resetAllRateLimitsMutation = useMutation({ mutationFn: resetAllProviderChannelRateLimits, onSuccess: invalidate });

  return { createMutation, updateMutation, enableMutation, disableMutation, deleteMutation, resetAllRateLimitsMutation, invalidate };
}
