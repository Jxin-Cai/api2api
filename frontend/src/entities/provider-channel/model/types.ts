import type { ProviderChannelListResponse, ProviderChannelResponse } from '@shared/api/contracts';

export type { ProviderChannelListResponse, ProviderChannelResponse };

export interface AdminCreateProviderChannelRequest {
  name: string;
  host: string;
  keyRef: string;
  routePriority: number;
  supportedProtocols: string[];
}

export interface AdminUpdateProviderChannelRequest {
  name: string;
  host: string;
  keyRef?: string;
  routePriority: number;
  supportedProtocols: string[];
}
