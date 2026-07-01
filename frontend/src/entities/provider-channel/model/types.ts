import type { ProviderChannelListResponse, ProviderChannelResponse, ProtocolMappingResponse } from '@shared/api/contracts';

export type { ProviderChannelListResponse, ProviderChannelResponse, ProtocolMappingResponse };

export interface ProtocolMappingRequest {
  requestProtocol: string;
  upstreamProtocol: string;
}

export interface AdminCreateProviderChannelRequest {
  name: string;
  host: string;
  keyRef: string;
  modelsPath: string;
  routePriority: number;
  supportedProtocols: string[];
  protocolMappings: ProtocolMappingRequest[];
}

export interface AdminUpdateProviderChannelRequest {
  name: string;
  host: string;
  keyRef?: string;
  modelsPath: string;
  routePriority: number;
  supportedProtocols: string[];
  protocolMappings: ProtocolMappingRequest[];
}
