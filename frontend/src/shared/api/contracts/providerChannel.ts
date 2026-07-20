import type { EnabledStatus, ModelSource } from '@shared/types/admin';

export interface ChannelModelSupportResponse {
  id: number;
  requestedModel: string;
  upstreamModel: string;
  upstreamProtocol: string;
  priority: number;
  preferred?: boolean;
  status: EnabledStatus | string;
  rateLimitedAt?: number | null;
  rateLimitResetAt?: number | null;
  source: ModelSource;
  createdAt?: number;
  updatedAt?: number;
}

export interface ProtocolMappingResponse {
  requestProtocol: string;
  upstreamProtocol: string;
}

export interface ProviderChannelResponse {
  id: number;
  name: string;
  host: string;
  keyRef: string;
  keyMasked?: string;
  hasKey?: boolean;
  modelsPath?: string;
  routePriority?: number;
  supportedProtocols: string[];
  protocolMappings?: ProtocolMappingResponse[];
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
