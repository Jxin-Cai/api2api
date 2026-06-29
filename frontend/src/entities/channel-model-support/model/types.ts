import type { ChannelModelSupportResponse } from '@shared/api/contracts';
import type { ModelSource } from '@shared/types/admin';

export type { ChannelModelSupportResponse };

export interface AdminFetchProviderModelsRequest {
  defaultPriority: number;
}

export interface AdminUpsertChannelModelRequest {
  requestedModel: string;
  upstreamModel: string;
  upstreamProtocol: string;
  priority: number;
  source: ModelSource;
}

export interface AdminRemoveChannelModelRequest {
  requestedModel: string;
  upstreamProtocol: string;
}
