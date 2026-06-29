import type { AdminUpsertChannelModelRequest } from '@entities/channel-model-support';

export interface ChannelModelDraft extends AdminUpsertChannelModelRequest {
  status?: string;
}

export interface ChannelModelFetchState {
  defaultPriority: number;
}

export type ChannelModelEditingId = number | 'new' | null;

export interface ChannelModelDeleteParams {
  requestedModel: string;
  upstreamProtocol: string;
}
