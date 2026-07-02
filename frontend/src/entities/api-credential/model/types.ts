export type ApiCredentialStatus = 'ENABLED' | 'DISABLED' | 'UNKNOWN' | string;

export interface ApiCredentialResponse {
  id: string;
  name: string;
  modelWhitelist: string[];
  tokenLimit: number;
  consumedTokens?: number;
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

// TODO: 后端 RenameApiCredentialRequest DTO 当前未落盘，字段以后端补齐为准。
export interface RenameApiCredentialRequest {
  name: string;
}

// TODO: 后端 ReplaceModelWhitelistRequest DTO 当前未落盘，字段以后端补齐为准。
export interface ReplaceModelWhitelistRequest {
  modelWhitelist: string[];
}

// TODO: 后端 ChangeTokenLimitRequest DTO 当前未落盘，字段以后端补齐为准。
export interface ChangeTokenLimitRequest {
  tokenLimit: number;
}

export interface ApiCredentialOption {
  label: string;
  value: string;
}
