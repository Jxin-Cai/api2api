export type ApiCredentialStatus = 'ACTIVE' | 'ENABLED' | 'DISABLED' | 'UNKNOWN' | string;

export interface ApiCredentialResponse {
  id: string;
  name: string;
  modelGroupId: string;
  modelWhitelist: string[];
  /** 因分组或 Key 周期额度耗尽而限流的模型 */
  rateLimitedModels: string[];
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

export interface ApiCredentialOption {
  label: string;
  value: string;
}
