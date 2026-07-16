export type ProtocolType = 'CLAUDE_MESSAGES' | 'OPENAI_RESPONSES' | 'OPENAI_CHAT_COMPLETIONS' | 'AWS_BEDROCK_CONVERSE';

export interface ProtocolMeta {
  label: string;
  color: string;
  officialSpecUrl?: string;
  verifiedAt?: string;
  referenceVersion?: string;
}

export interface CapabilityMeta {
  key: string;
  label: string;
  tooltip: string;
  color: string;
}

export const PROTOCOL_OPTIONS: Array<{ label: string; value: ProtocolType }> = [
  { label: 'Claude Messages', value: 'CLAUDE_MESSAGES' },
  { label: 'OpenAI Responses', value: 'OPENAI_RESPONSES' },
  { label: 'OpenAI Chat Completions', value: 'OPENAI_CHAT_COMPLETIONS' },
];

export const UPSTREAM_PROTOCOL_OPTIONS: Array<{ label: string; value: ProtocolType }> = [
  { label: 'Claude Messages', value: 'CLAUDE_MESSAGES' },
  { label: 'OpenAI Responses', value: 'OPENAI_RESPONSES' },
  { label: 'OpenAI Chat Completions', value: 'OPENAI_CHAT_COMPLETIONS' },
  { label: 'AWS Bedrock Converse', value: 'AWS_BEDROCK_CONVERSE' },
];

const PROTOCOL_META: Record<string, ProtocolMeta> = {
  CLAUDE_MESSAGES: {
    label: 'Claude Messages', color: 'orange', verifiedAt: '2026-07-16', referenceVersion: 'Anthropic SDK 0.111.0',
    officialSpecUrl: 'https://platform.claude.com/docs/en/api/messages/create',
  },
  OPENAI_RESPONSES: {
    label: 'OpenAI Responses', color: 'purple', verifiedAt: '2026-07-16', referenceVersion: 'OpenAI SDK 6.47.0',
    officialSpecUrl: 'https://developers.openai.com/api/reference/resources/responses/methods/create',
  },
  OPENAI_CHAT_COMPLETIONS: {
    label: 'OpenAI Chat Completions', color: 'blue', verifiedAt: '2026-07-16', referenceVersion: 'OpenAI SDK 6.47.0',
    officialSpecUrl: 'https://developers.openai.com/api/reference/resources/chat',
  },
  AWS_BEDROCK_CONVERSE: {
    label: 'AWS Bedrock Converse', color: 'green', verifiedAt: '2026-07-16', referenceVersion: 'Bedrock Runtime 2023-09-30',
    officialSpecUrl: 'https://docs.aws.amazon.com/bedrock/latest/APIReference/API_runtime_Converse.html',
  },
  OPENAI_CHAT: { label: 'OpenAI Chat Completions', color: 'blue' },
  'claude-messages': { label: 'Claude Messages', color: 'orange' },
  'openai-responses': { label: 'OpenAI Responses', color: 'purple' },
  'openai-chat-completions': { label: 'OpenAI Chat Completions', color: 'blue' },
  'openai-chat': { label: 'OpenAI Chat Completions', color: 'blue' },
};

export const CAPABILITY_META: CapabilityMeta[] = [
  { key: 'supportsStreaming', label: 'Streaming', tooltip: '支持流式响应映射', color: 'cyan' },
  { key: 'supportsToolCalling', label: 'Tool', tooltip: '支持工具调用映射', color: 'purple' },
  { key: 'supportsReasoning', label: 'Reasoning', tooltip: '支持 reasoning 字段映射', color: 'blue' },
  { key: 'supportsUsageMapping', label: 'Usage', tooltip: '支持用量统计映射', color: 'green' },
  { key: 'supportsCacheTokenMapping', label: 'Cache', tooltip: '支持缓存 token 映射', color: 'gold' },
];

export function getProtocolMeta(protocol: string): ProtocolMeta {
  return PROTOCOL_META[protocol] ?? { label: protocol, color: 'geekblue' };
}

export function formatProtocolDirection(sourceProtocol: string, targetProtocol: string): string {
  return `${getProtocolMeta(sourceProtocol).label} → ${getProtocolMeta(targetProtocol).label}`;
}
