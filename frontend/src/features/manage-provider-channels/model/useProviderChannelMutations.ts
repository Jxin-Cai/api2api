import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  createProviderChannel,
  disableProviderChannel,
  enableProviderChannel,
  providerChannelQueryKeys,
  updateProviderChannel,
} from '@entities/provider-channel';
import type { AdminUpdateProviderChannelRequest } from '@entities/provider-channel';

export function useProviderChannelMutations() {
  const queryClient = useQueryClient();
  function invalidate(): Promise<void> {
    return queryClient.invalidateQueries({ queryKey: providerChannelQueryKeys.all });
  }

  const createMutation = useMutation({ mutationFn: createProviderChannel, onSuccess: invalidate });
  const updateMutation = useMutation({
    mutationFn: (params: { id: number; body: AdminUpdateProviderChannelRequest }) => updateProviderChannel(params.id, params.body),
    onSuccess: invalidate,
  });
  const enableMutation = useMutation({ mutationFn: enableProviderChannel, onSuccess: invalidate });
  const disableMutation = useMutation({ mutationFn: disableProviderChannel, onSuccess: invalidate });

  return { createMutation, updateMutation, enableMutation, disableMutation, invalidate };
}
