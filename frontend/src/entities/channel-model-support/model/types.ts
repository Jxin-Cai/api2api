import type { ChannelModelSupportResponse, ProviderModelPreviewResponse } from '@shared/api/contracts';
import type { ModelSource } from '@shared/types/admin';

export type { ChannelModelSupportResponse, ProviderModelPreviewResponse };

export interface AdminFetchProviderModelsRequest {
  defaultPriority: number;
}

export interface AdminFetchProviderChannelModelPreviewRequest {
  defaultPriority: number;
}

export interface AdminFetchProviderModelPreviewRequest {
  host: string;
  keyRef: string;
  supportedProtocols: string[];
  defaultPriority: number;
}

export interface AdminUpsertChannelModelRequest {
  requestedModel: string;
  upstreamModel: string;
  upstreamProtocol: string;
  priority: number;
  preferred?: boolean;
  source: ModelSource;
}

export interface AdminBatchUpsertChannelModelItemRequest extends AdminUpsertChannelModelRequest {
  id?: number;
}

export interface AdminBatchUpsertChannelModelsRequest {
  replaceExisting?: boolean;
  models: AdminBatchUpsertChannelModelItemRequest[];
}

export interface AdminRemoveChannelModelRequest {
  requestedModel: string;
  upstreamProtocol: string;
}
