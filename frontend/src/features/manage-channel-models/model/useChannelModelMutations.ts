import { useMutation, useQueryClient } from '@tanstack/react-query';
import { batchUpsertChannelModels, fetchProviderChannelModelPreview, fetchProviderModels, removeChannelModel, upsertChannelModel } from '@entities/channel-model-support';
import type { AdminBatchUpsertChannelModelsRequest, AdminFetchProviderChannelModelPreviewRequest, AdminFetchProviderModelsRequest, AdminRemoveChannelModelRequest, AdminUpsertChannelModelRequest } from '@entities/channel-model-support';
import { providerChannelQueryKeys } from '@entities/provider-channel';

export function useChannelModelMutations() {
  const queryClient = useQueryClient();
  function invalidate(): Promise<void> {
    return queryClient.invalidateQueries({ queryKey: providerChannelQueryKeys.all });
  }

  const fetchMutation = useMutation({
    mutationFn: (params: { channelId: number; body: AdminFetchProviderModelsRequest }) => fetchProviderModels(params.channelId, params.body),
    onSuccess: invalidate,
  });
  const previewMutation = useMutation({
    mutationFn: (params: { channelId: number; body: AdminFetchProviderChannelModelPreviewRequest }) => fetchProviderChannelModelPreview(params.channelId, params.body),
  });
  const batchUpsertMutation = useMutation({
    mutationFn: (params: { channelId: number; body: AdminBatchUpsertChannelModelsRequest }) => batchUpsertChannelModels(params.channelId, params.body),
    onSuccess: invalidate,
  });
  const upsertMutation = useMutation({
    mutationFn: (params: { channelId: number; modelId: number; body: AdminUpsertChannelModelRequest }) => upsertChannelModel(params.channelId, params.modelId, params.body),
    onSuccess: invalidate,
  });
  const removeMutation = useMutation({
    mutationFn: (params: { channelId: number; body: AdminRemoveChannelModelRequest }) => removeChannelModel(params.channelId, params.body),
    onSuccess: invalidate,
  });

  return { fetchMutation, previewMutation, batchUpsertMutation, upsertMutation, removeMutation };
}
