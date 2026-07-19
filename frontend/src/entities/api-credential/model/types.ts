export type ApiCredentialStatus = 'ENABLED' | 'DISABLED' | 'UNKNOWN' | string;

export interface ApiCredentialResponse {
  id: string;
  name: string;
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
  modelWhitelist: string[];
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

export interface ReplaceModelWhitelistRequest {
  modelWhitelist: string[];
}

export interface ChangeTokenLimitRequest {
  tokenLimit: number;
}

export interface ApiCredentialOption {
  label: string;
  value: string;
}
