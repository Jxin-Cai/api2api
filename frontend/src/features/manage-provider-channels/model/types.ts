import type { ProviderChannelResponse } from '@entities/provider-channel';
import type { AdminFormMode } from '@shared/types/admin';

export interface ProviderChannelFormState {
  name: string;
  host: string;
  keyRef: string;
  routePriority: number;
  supportedProtocols: string[];
}

export interface ProviderChannelSearchState {
  keyword: string;
}

export interface ProviderChannelDrawerState {
  mode: AdminFormMode;
  channel: ProviderChannelResponse | null;
  open: boolean;
}

export type ProviderChannelToggleStatus = 'enable' | 'disable';
