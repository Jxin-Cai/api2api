import { notifyAuthExpired } from './authSession';
import { appendQuery, jsonBody } from './http';
import { API_SUCCESS_CODE, type ApiErrorShape, type ApiResponse, type QueryParams } from './types';

const AUTHENTICATION_REQUIRED_CODE = 'AUTHENTICATION_REQUIRED';

function getBaseUrl(): string {
  if (import.meta.env.DEV) {
    return '';
  }
  return import.meta.env.VITE_API_BASE_URL ?? '';
}

async function safeParseJson<T>(response: Response): Promise<ApiResponse<T> | null> {
  const contentType = response.headers.get('content-type') ?? '';
  if (response.status === 204) {
    return null;
  }
  const text = await response.text();
  if (!text) {
    return null;
  }
  if (!contentType.includes('application/json')) {
    return null;
  }
  try {
    return JSON.parse(text) as ApiResponse<T>;
  } catch {
    return null;
  }
}

async function parseResponse<T>(response: Response): Promise<ApiResponse<T>> {
  const payload = await safeParseJson<T>(response);

  if (!response.ok) {
    const code = payload?.code || `HTTP_${response.status}`;
    if (response.status === 401 || code === AUTHENTICATION_REQUIRED_CODE) {
      notifyAuthExpired();
    }
    const error: ApiErrorShape = {
      status: response.status,
      code: response.status === 401 ? AUTHENTICATION_REQUIRED_CODE : code,
      message: payload?.message || `HTTP ${response.status}`,
    };
    throw error;
  }

  if (payload === null) {
    return { code: API_SUCCESS_CODE, message: 'success', data: undefined as T };
  }

  if (payload.code !== API_SUCCESS_CODE) {
    const error: ApiErrorShape = {
      status: response.status,
      code: payload.code,
      message: payload.message || '业务处理失败',
    };
    throw error;
  }
  return payload;
}

async function request<T>(
  method: string,
  path: string,
  body?: unknown,
  params?: QueryParams
): Promise<ApiResponse<T>> {
  try {
    const response = await fetch(`${getBaseUrl()}${appendQuery(path, params)}`, {
      method,
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
      },
      body: jsonBody(body),
    });
    return await parseResponse<T>(response);
  } catch (error: unknown) {
    if (typeof error === 'object' && error !== null && 'code' in error) {
      throw error;
    }
    const networkError: ApiErrorShape = {
      status: 0,
      code: 'NETWORK_ERROR',
      message: '网络异常，请检查连接后重试',
    };
    throw networkError;
  }
}

export const apiClient = {
  get<T>(path: string, params?: QueryParams): Promise<ApiResponse<T>> {
    return request<T>('GET', path, undefined, params);
  },
  post<T>(path: string, body?: unknown, params?: QueryParams): Promise<ApiResponse<T>> {
    return request<T>('POST', path, body, params);
  },
  put<T>(path: string, body?: unknown, params?: QueryParams): Promise<ApiResponse<T>> {
    return request<T>('PUT', path, body, params);
  },
  patch<T>(path: string, body?: unknown, params?: QueryParams): Promise<ApiResponse<T>> {
    return request<T>('PATCH', path, body, params);
  },
  delete<T>(path: string, params?: QueryParams): Promise<ApiResponse<T>> {
    return request<T>('DELETE', path, undefined, params);
  },
};
