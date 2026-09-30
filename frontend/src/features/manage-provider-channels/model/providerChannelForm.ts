import type { ChannelModelSupportResponse } from '@entities/channel-model-support';
import type { ProviderChannelResponse, ProtocolMappingRequest } from '@entities/provider-channel';

export function isHttpHost(host: string): boolean {
  return /^https?:\/\//i.test(host.trim());
}

export function isFormValidationError(error: unknown): boolean {
  return typeof error === 'object' && error !== null && 'errorFields' in error;
}

export function normalizeProtocolMappings(protocols: string[], mappings?: ProtocolMappingRequest[]): ProtocolMappingRequest[] {
  const existing = new Map((mappings ?? []).map((mapping) => [mapping.requestProtocol, mapping.upstreamProtocol]));
  return protocols.map((protocol) => ({
    requestProtocol: protocol,
    upstreamProtocol: existing.get(protocol) ?? protocol,
  }));
}

export function derivePreviewUpstreamProtocols(protocols: string[], mappings?: ProtocolMappingRequest[]): string[] {
  const normalizedMappings = normalizeProtocolMappings(protocols, mappings);
  return Array.from(new Set(normalizedMappings.map((mapping) => mapping.upstreamProtocol)));
}

export function deriveSupportedProtocols(channel: ProviderChannelResponse): string[] {
  const mappings = channel.protocolMappings ?? [];
  if (mappings.length > 0) {
    return mappings.map((mapping) => mapping.requestProtocol);
  }
  return channel.supportedProtocols ?? [];
}

export function modelKey(model: Pick<ChannelModelSupportResponse, 'requestedModel' | 'upstreamProtocol'>): string {
  return `${model.requestedModel}::${model.upstreamProtocol}`;
}

function isMaskedKey(value: string | undefined, keyMasked?: string): boolean {
  const trimmed = value?.trim();
  if (!trimmed) {
    return false;
  }
  return trimmed.includes('****') || Boolean(keyMasked && trimmed === keyMasked);
}

export function sanitizeEditableKey(value: string | undefined, keyMasked?: string): string {
  const trimmed = value?.trim() ?? '';
  return isMaskedKey(trimmed, keyMasked) ? '' : trimmed;
}

/**
 * 上游返回的候选合并已保存配置；已保存但上游未返回的模型继续保留为候选，
 * 是否生效由用户勾选决定，避免替换保存时被静默删除。
 */
export function mergeWithExistingModels(
  models: ChannelModelSupportResponse[],
  existingModels: ChannelModelSupportResponse[]
): ChannelModelSupportResponse[] {
  const fetchedKeys = new Set(models.map(modelKey));
  const merged = models.map((model) => {
    const existing = existingModels.find((item) => modelKey(item) === modelKey(model));
    return existing ? {
      ...model,
      id: existing.id,
      priority: existing.priority,
      preferred: existing.preferred,
      source: existing.source,
      status: existing.status,
    } : model;
  });
  const retained = existingModels.filter((model) => !fetchedKeys.has(modelKey(model)));
  return [...merged, ...retained];
}
