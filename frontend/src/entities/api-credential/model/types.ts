export type ApiCredentialStatus = 'ACTIVE' | 'ENABLED' | 'DISABLED' | 'UNKNOWN' | string;

export interface ApiCredentialResponse {
  id: string;
  name: string;
  modelGroupId: string;
  modelWhitelist: string[];
  tokenLimit: number;
  consumedTokens?: number;
  totalTokens?: number;
  todayConsumedTokens?: number;
  todayTotalTokens?: number;
  remainingTokens?: number | null;
  keyPreview?: string;
  lastUsedAt?: string | number;
  status: ApiCredentialStatus;
  createdAt?: string | number;
  updatedAt?: string | number;
}

export interface ApiCredentialListResponse {
  credentials: ApiCredentialResponse[];
}

export interface CreateApiCredentialRequest {
  name: string;
  modelGroupId: string;
  tokenLimit: number;
}

export interface CreateApiCredentialBackendResponse {
  credential: ApiCredentialResponse;
  plaintextApiKey?: string;
}

export interface RevealApiCredentialSecretResponse {
  apiCredentialId: string;
  keyPreview?: string;
  plainApiKey?: string;
}

export interface CreateApiCredentialResponse extends ApiCredentialResponse {
  plainApiKey?: string;
}

export interface RenameApiCredentialRequest {
  name: string;
}

export interface ChangeModelGroupRequest {
  modelGroupId: string;
}

export interface ChangeTokenLimitRequest {
  tokenLimit: number;
}

export interface ApiCredentialOption {
  label: string;
  value: string;
}
