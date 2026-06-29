import type { ImplementationStatus, ProtocolConversionStatus } from '@shared/types/admin';

export interface ProtocolConversionFilters {
  sourceProtocol?: string;
  targetProtocol?: string;
  status?: ProtocolConversionStatus;
  implementationStatus?: ImplementationStatus;
}

export interface ProtocolDirectionQuery {
  sourceProtocol: string;
  targetProtocol: string;
}

export interface ProtocolConversionDrawerState {
  open: boolean;
  definitionId: string | null;
}

export type ProtocolConversionActiveTab = 'request' | 'response';
