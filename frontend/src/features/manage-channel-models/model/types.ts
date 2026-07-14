import type { AdminUpsertChannelModelRequest } from '@entities/channel-model-support';

export interface ChannelModelDraft extends AdminUpsertChannelModelRequest {
  status?: string;
}

export type ChannelModelEditingId = number | 'new' | null;
