import type { ApiCredentialResponse, CreateApiCredentialRequest } from '@entities/api-credential';

export interface ApiCredentialCreateModalState {
  form: CreateApiCredentialRequest;
  saveConfirmed: boolean;
}

export type ApiCredentialEditSection = 'name' | 'models' | 'limit';

export interface ApiCredentialEditDrawerState {
  activeSection: ApiCredentialEditSection;
  nameDraft: string;
  modelWhitelistDraft: string[];
  tokenLimitDraft: number;
}

export interface ApiCredentialListSearchState {
  search: string;
}

export interface ApiCredentialToggleStatusState {
  togglingId: string | null;
}

export interface ApiCredentialTablePanelPropsBase {
  initialSelectedCredential?: ApiCredentialResponse | null;
}
