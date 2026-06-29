import type { EnabledStatus, ModelSource } from '@shared/types/admin';

export interface ChannelModelSupportResponse {
  id: number;
  requestedModel: string;
  upstreamModel: string;
  upstreamProtocol: string;
  priority: number;
  status: EnabledStatus | string;
  source: ModelSource;
  createdAt?: number;
  updatedAt?: number;
}

export interface ProviderChannelResponse {
  id: number;
  name: string;
  host: string;
  keyRef: string;
  supportedProtocols: string[];
  supportedModels: ChannelModelSupportResponse[];
  status: EnabledStatus | string;
  createdAt?: number;
  updatedAt?: number;
}

export interface ProviderChannelListResponse {
  channels: ProviderChannelResponse[];
}
