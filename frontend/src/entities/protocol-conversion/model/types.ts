import type { ImplementationStatus, ProtocolConversionStatus } from '@shared/types/admin';

export interface ProtocolConversionListItemResponse {
  id: number;
  sourceProtocol: string;
  targetProtocol: string;
  kind: string;
  status: ProtocolConversionStatus;
  implementationStatus: ImplementationStatus;
  supportsStreaming: boolean;
  supportsToolCalling: boolean;
  supportsReasoning: boolean;
  supportsUsageMapping: boolean;
  supportsCacheTokenMapping: boolean;
}

export interface ProtocolConversionListResponse {
  conversions: ProtocolConversionListItemResponse[];
}

export interface ProtocolConversionCapabilityResponse {
  supportsStreaming: boolean;
  supportsToolCalling: boolean;
  supportsReasoning: boolean;
  supportsUsageMapping: boolean;
  supportsCacheTokenMapping: boolean;
  supportedContentTypes?: string[];
}

export interface ProtocolConversionFieldMappingResponse {
  sourceField: string;
  targetField: string;
  ruleDescription: string;
  lossiness: string;
}

export interface ProtocolConversionMappingResponse {
  direction: string;
  title: string;
  summary: string;
  fieldMappings: ProtocolConversionFieldMappingResponse[];
}

export interface ProtocolConversionResponse {
  id: number;
  sourceProtocol: string;
  targetProtocol: string;
  kind: string;
  status: ProtocolConversionStatus;
  implementationStatus: ImplementationStatus;
  capability: ProtocolConversionCapabilityResponse;
  requestMapping: ProtocolConversionMappingResponse;
  responseMapping: ProtocolConversionMappingResponse;
  createdAt?: number;
  updatedAt?: number;
}

export interface ProtocolConversionDirectionRequest {
  sourceProtocol: string;
  targetProtocol: string;
}

export type ProtocolConversionCapabilityLike = ProtocolConversionCapabilityResponse | ProtocolConversionListItemResponse;
