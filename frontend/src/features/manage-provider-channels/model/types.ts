import type { ProviderChannelResponse, ProtocolMappingRequest } from '@entities/provider-channel';
import type { AdminFormMode } from '@shared/types/admin';

export interface ProviderChannelFormState {
  name: string;
  host: string;
  keyRef: string;
  modelsPath: string;
  routePriority: number;
  supportedProtocols: string[];
  protocolMappings: ProtocolMappingRequest[];
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
