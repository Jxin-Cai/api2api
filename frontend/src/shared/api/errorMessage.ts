import type { ApiErrorShape } from './types';

const KNOWN_API_ERRORS: Record<string, string> = {
  PROVIDER_MODELS_AUTH_FAILED: '上游认证失败，请检查渠道 Key 是否具备模型列表权限',
  PROVIDER_MODELS_PATH_NOT_FOUND: '未找到模型列表接口（默认请求 host/v1/models）',
  PROVIDER_MODELS_TIMEOUT: '请求上游模型列表超时',
  PROVIDER_MODELS_IO_ERROR: '请求上游模型列表失败',
  PROVIDER_MODELS_EMPTY: '上游未返回任何模型',
  PROVIDER_MODELS_RESPONSE_INVALID: '上游模型列表响应格式无效',
  PROVIDER_MODELS_UPSTREAM_PROTOCOLS_EMPTY: '渠道未配置上游调用协议',
  PROVIDER_MODELS_INTERRUPTED: '请求上游模型列表被中断',
  API_KEY_MATERIAL_UNAVAILABLE: '该 API Key 无法解密显示，请重新创建后再复制',
  API_KEY_MATERIAL_CORRUPTED: 'API Key 密文已损坏，请重新创建后再复制',
  API_KEY_MATERIAL_DECRYPTION_FAILED: 'API Key 解密失败，请联系管理员检查加密配置',
};

function isApiErrorShape(error: unknown): error is ApiErrorShape {
  return typeof error === 'object' && error !== null && 'message' in error;
}

export function getApiErrorMessage(error: unknown, fallback: string): string {
  if (!isApiErrorShape(error)) {
    return fallback;
  }
  const mapped = error.code ? KNOWN_API_ERRORS[error.code] : undefined;
  if (error.message && error.message !== error.code && !error.message.startsWith('PROVIDER_MODELS_') && !error.message.startsWith('API_KEY_MATERIAL_')) {
    return error.message;
  }
  return mapped || error.message || fallback;
}
