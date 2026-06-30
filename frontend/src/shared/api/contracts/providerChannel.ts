import type { EnabledStatus, ModelSource } from '@shared/types/admin';

export interface ChannelModelSupportResponse {
  id: number;
  requestedModel: string;
  upstreamModel: string;
  upstreamProtocol: string;
  priority: number;
  preferred?: boolean;
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
  keyMasked?: string;
  hasKey?: boolean;
  routePriority?: number;
  supportedProtocols: string[];
  supportedModels: ChannelModelSupportResponse[];
  status: EnabledStatus | string;
  createdAt?: number;
  updatedAt?: number;
}

export interface ProviderChannelListResponse {
  channels: ProviderChannelResponse[];
}

export interface ProviderModelPreviewResponse {
  models: ChannelModelSupportResponse[];
}
