import { apiClient, type ApiResponse } from '@shared/api';

import type {
  ApiCredentialListResponse,
  ApiCredentialResponse,
  ChangeTokenLimitRequest,
  CreateApiCredentialBackendResponse,
  CreateApiCredentialRequest,
  CreateApiCredentialResponse,
  RenameApiCredentialRequest,
  ReplaceModelWhitelistRequest,
  RevealApiCredentialSecretResponse,
} from '../model/types';

interface RevealApiCredentialSecretBackendResponse {
  apiCredentialId: string | number;
  keyPreview?: string;
  plaintextApiKey?: string;
}

interface ApiCredentialBackendResponse {
  id: string | number;
  name?: string;
  modelWhitelist?: string[];
  tokenLimit?: number;
  consumedTokens?: number;
  totalTokens?: number;
  todayConsumedTokens?: number;
  todayTotalTokens?: number;
  remainingTokens?: number | null;
  keyPreview?: string;
  lastUsedAt?: string | number;
  status?: string;
  createdAt?: string | number;
  updatedAt?: string | number;
}

function normalizeCredential(raw: ApiCredentialBackendResponse): ApiCredentialResponse {
  return {
    id: String(raw.id),
    name: raw.name ?? '',
    modelWhitelist: raw.modelWhitelist ?? [],
    tokenLimit: Number(raw.tokenLimit ?? 0),
    consumedTokens: Number(raw.consumedTokens ?? 0),
    totalTokens: Number(raw.totalTokens ?? 0),
    todayConsumedTokens: Number(raw.todayConsumedTokens ?? 0),
    todayTotalTokens: Number(raw.todayTotalTokens ?? 0),
    remainingTokens: raw.remainingTokens ?? null,
    keyPreview: raw.keyPreview,
    lastUsedAt: raw.lastUsedAt,
    status: raw.status ?? 'UNKNOWN',
    createdAt: raw.createdAt,
    updatedAt: raw.updatedAt,
  };
}

function credentialPath(credentialId: string, suffix: string): string {
  return `/api/api-credentials/${encodeURIComponent(credentialId)}${suffix}`;
}

export async function listApiCredentials(): Promise<ApiResponse<ApiCredentialListResponse>> {
  const zoneId = Intl.DateTimeFormat().resolvedOptions().timeZone;
  const response = await apiClient.get<{ credentials: ApiCredentialBackendResponse[] }>('/api/api-credentials', { zoneId });
  return {
    ...response,
    data: { credentials: (response.data.credentials ?? []).map(normalizeCredential) },
  };
}

export async function createApiCredential(
  params: CreateApiCredentialRequest
): Promise<ApiResponse<CreateApiCredentialResponse>> {
  const response = await apiClient.post<CreateApiCredentialBackendResponse>('/api/api-credentials', params);
  return {
    ...response,
    data: {
      ...normalizeCredential(response.data.credential),
      plainApiKey: response.data.plaintextApiKey,
    },
  };
}

export async function revealApiCredentialSecret(credentialId: string): Promise<ApiResponse<RevealApiCredentialSecretResponse>> {
  const response = await apiClient.post<RevealApiCredentialSecretBackendResponse>(credentialPath(credentialId, '/reveal'));
  return {
    ...response,
    data: {
      apiCredentialId: String(response.data.apiCredentialId),
      keyPreview: response.data.keyPreview,
      plainApiKey: response.data.plaintextApiKey,
    },
  };
}

export async function renameApiCredential(
  credentialId: string,
  params: RenameApiCredentialRequest
): Promise<ApiResponse<ApiCredentialResponse>> {
  const response = await apiClient.patch<ApiCredentialBackendResponse>(credentialPath(credentialId, '/name'), params);
  return { ...response, data: normalizeCredential(response.data) };
}

export async function replaceModelWhitelist(
  credentialId: string,
  params: ReplaceModelWhitelistRequest
): Promise<ApiResponse<ApiCredentialResponse>> {
  const response = await apiClient.put<ApiCredentialBackendResponse>(credentialPath(credentialId, '/model-whitelist'), params);
  return { ...response, data: normalizeCredential(response.data) };
}

export async function changeApiCredentialTokenLimit(
  credentialId: string,
  params: ChangeTokenLimitRequest
): Promise<ApiResponse<ApiCredentialResponse>> {
  const response = await apiClient.put<ApiCredentialBackendResponse>(credentialPath(credentialId, '/token-limit'), params);
  return { ...response, data: normalizeCredential(response.data) };
}

export async function enableApiCredential(credentialId: string): Promise<ApiResponse<ApiCredentialResponse>> {
  const response = await apiClient.patch<ApiCredentialBackendResponse>(credentialPath(credentialId, '/enable'));
  return { ...response, data: normalizeCredential(response.data) };
}

export async function disableApiCredential(credentialId: string): Promise<ApiResponse<ApiCredentialResponse>> {
  const response = await apiClient.patch<ApiCredentialBackendResponse>(credentialPath(credentialId, '/disable'));
  return { ...response, data: normalizeCredential(response.data) };
}

export async function deleteApiCredential(credentialId: string): Promise<ApiResponse<void>> {
  return apiClient.delete<void>(credentialPath(credentialId, ''));
}
