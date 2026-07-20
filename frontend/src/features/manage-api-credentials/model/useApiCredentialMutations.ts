import { useMutation, useQueryClient, type UseMutationResult } from '@tanstack/react-query';

import {
  API_CREDENTIALS_QUERY_KEY,
  changeApiCredentialTokenLimit,
  createApiCredential,
  deleteApiCredential,
  disableApiCredential,
  enableApiCredential,
  renameApiCredential,
  changeApiCredentialModelGroup,
  revealApiCredentialSecret,
  type ApiCredentialResponse,
  type ChangeTokenLimitRequest,
  type CreateApiCredentialRequest,
  type CreateApiCredentialResponse,
  type RenameApiCredentialRequest,
  type ChangeModelGroupRequest,
  type RevealApiCredentialSecretResponse,
} from '@entities/api-credential';

export interface UseApiCredentialMutationsResult {
  createMutation: UseMutationResult<CreateApiCredentialResponse, Error, CreateApiCredentialRequest>;
  renameMutation: UseMutationResult<ApiCredentialResponse, Error, { id: string; params: RenameApiCredentialRequest }>;
  groupMutation: UseMutationResult<ApiCredentialResponse, Error, { id: string; params: ChangeModelGroupRequest }>;
  limitMutation: UseMutationResult<ApiCredentialResponse, Error, { id: string; params: ChangeTokenLimitRequest }>;
  revealMutation: UseMutationResult<RevealApiCredentialSecretResponse, Error, string>;
  enableMutation: UseMutationResult<ApiCredentialResponse, Error, string>;
  disableMutation: UseMutationResult<ApiCredentialResponse, Error, string>;
  deleteMutation: UseMutationResult<void, Error, string>;
}

export function useApiCredentialMutations(): UseApiCredentialMutationsResult {
  const queryClient = useQueryClient();
  const invalidate = async (): Promise<void> => {
    await queryClient.invalidateQueries({ queryKey: API_CREDENTIALS_QUERY_KEY });
  };

  return {
    createMutation: useMutation({ mutationFn: async (params: CreateApiCredentialRequest): Promise<CreateApiCredentialResponse> => (await createApiCredential(params)).data, onSuccess: invalidate }),
    renameMutation: useMutation({ mutationFn: async ({ id, params }: { id: string; params: RenameApiCredentialRequest }): Promise<ApiCredentialResponse> => (await renameApiCredential(id, params)).data, onSuccess: invalidate }),
    groupMutation: useMutation({ mutationFn: async ({ id, params }: { id: string; params: ChangeModelGroupRequest }): Promise<ApiCredentialResponse> => (await changeApiCredentialModelGroup(id, params)).data, onSuccess: invalidate }),
    limitMutation: useMutation({ mutationFn: async ({ id, params }: { id: string; params: ChangeTokenLimitRequest }): Promise<ApiCredentialResponse> => (await changeApiCredentialTokenLimit(id, params)).data, onSuccess: invalidate }),
    revealMutation: useMutation({ mutationFn: async (id: string): Promise<RevealApiCredentialSecretResponse> => (await revealApiCredentialSecret(id)).data }),
    enableMutation: useMutation({ mutationFn: async (id: string): Promise<ApiCredentialResponse> => (await enableApiCredential(id)).data, onSuccess: invalidate }),
    disableMutation: useMutation({ mutationFn: async (id: string): Promise<ApiCredentialResponse> => (await disableApiCredential(id)).data, onSuccess: invalidate }),
    deleteMutation: useMutation({
      mutationFn: async (id: string): Promise<void> => {
        await deleteApiCredential(id);
      },
      onSuccess: invalidate,
    }),
  };
}
