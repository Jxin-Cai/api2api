import { useMutation, useQueryClient, type UseMutationResult } from '@tanstack/react-query';

import {
  API_CREDENTIALS_QUERY_KEY,
} from '@entities/api-credential';
import {
  MODEL_GROUPS_QUERY_KEY,
  createModelGroup,
  deleteModelGroup,
  updateModelGroup,
  type ModelGroupResponse,
  type SaveModelGroupRequest,
} from '@entities/model-group';

export interface UseModelGroupMutationsResult {
  createMutation: UseMutationResult<ModelGroupResponse, Error, SaveModelGroupRequest>;
  updateMutation: UseMutationResult<ModelGroupResponse, Error, { id: string; params: SaveModelGroupRequest }>;
  deleteMutation: UseMutationResult<void, Error, string>;
}

export function useModelGroupMutations(): UseModelGroupMutationsResult {
  const queryClient = useQueryClient();
  const invalidateGroups = async (): Promise<void> => {
    await queryClient.invalidateQueries({ queryKey: MODEL_GROUPS_QUERY_KEY });
  };
  const invalidateGroupPermissions = async (): Promise<void> => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: MODEL_GROUPS_QUERY_KEY }),
      queryClient.invalidateQueries({ queryKey: API_CREDENTIALS_QUERY_KEY }),
    ]);
  };

  return {
    createMutation: useMutation({
      mutationFn: async (params) => (await createModelGroup(params)).data,
      onSuccess: invalidateGroups,
    }),
    updateMutation: useMutation({
      mutationFn: async ({ id, params }) => (await updateModelGroup(id, params)).data,
      onSuccess: invalidateGroupPermissions,
    }),
    deleteMutation: useMutation({
      mutationFn: async (id) => { await deleteModelGroup(id); },
      onSuccess: invalidateGroups,
    }),
  };
}
