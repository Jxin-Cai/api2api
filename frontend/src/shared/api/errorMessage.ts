import type { ApiErrorShape } from './types';

const PROVIDER_MODEL_FETCH_ERRORS: Record<string, string> = {
  PROVIDER_MODELS_AUTH_FAILED: '上游认证失败，请检查渠道 Key 是否具备模型列表权限',
  PROVIDER_MODELS_PATH_NOT_FOUND: '未找到模型列表接口（默认请求 host/v1/models）',
  PROVIDER_MODELS_TIMEOUT: '请求上游模型列表超时',
  PROVIDER_MODELS_IO_ERROR: '请求上游模型列表失败',
  PROVIDER_MODELS_EMPTY: '上游未返回任何模型',
  PROVIDER_MODELS_RESPONSE_INVALID: '上游模型列表响应格式无效',
  PROVIDER_MODELS_UPSTREAM_PROTOCOLS_EMPTY: '渠道未配置上游调用协议',
  PROVIDER_MODELS_INTERRUPTED: '请求上游模型列表被中断',
};

function isApiErrorShape(error: unknown): error is ApiErrorShape {
  return typeof error === 'object' && error !== null && 'message' in error;
}

export function getApiErrorMessage(error: unknown, fallback: string): string {
  if (!isApiErrorShape(error)) {
    return fallback;
  }
  const mapped = error.code ? PROVIDER_MODEL_FETCH_ERRORS[error.code] : undefined;
  if (error.message && error.message !== error.code && !error.message.startsWith('PROVIDER_MODELS_')) {
    return error.message;
  }
  return mapped || error.message || fallback;
}
