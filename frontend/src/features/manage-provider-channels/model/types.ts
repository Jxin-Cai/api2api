import type { ProtocolMappingRequest } from '@entities/provider-channel';

export interface ProviderChannelFormState {
  name: string;
  host: string;
  keyRef: string;
  routePriority: number;
  supportedProtocols: string[];
  protocolMappings: ProtocolMappingRequest[];
}
