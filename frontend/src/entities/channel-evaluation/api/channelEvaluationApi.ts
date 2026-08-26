import { apiClient } from '@shared/api';
import type { ApiResponse, QueryParams } from '@shared/api';
import type {
  AdminSubmitChannelEvaluationRequest,
  AdminUpsertChannelEvaluationScheduleRequest,
  ChannelEvaluationHistoryResponse,
  ChannelEvaluationScheduleResponse,
  ChannelEvaluationSubmitResponse,
  QueryChannelEvaluationHistoryRequest,
} from '../model/types';

function encodeId(id: number): string {
  return encodeURIComponent(String(id));
}

function evaluationPath(providerChannelId: number, suffix = ''): string {
  return `/api/admin/provider-channels/${encodeId(providerChannelId)}/evaluations${suffix}`;
}

export function submitChannelEvaluations(
  providerChannelId: number,
  params: AdminSubmitChannelEvaluationRequest = {}
): Promise<ApiResponse<ChannelEvaluationSubmitResponse>> {
  return apiClient.post(evaluationPath(providerChannelId), params);
}

export function listChannelEvaluations(
  providerChannelId: number,
  params: QueryChannelEvaluationHistoryRequest = {}
): Promise<ApiResponse<ChannelEvaluationHistoryResponse>> {
  return apiClient.get(evaluationPath(providerChannelId), params as QueryParams);
}

export function loadChannelEvaluationSchedule(
  providerChannelId: number
): Promise<ApiResponse<ChannelEvaluationScheduleResponse | null>> {
  return apiClient.get(evaluationPath(providerChannelId, '/schedule'));
}

export function upsertChannelEvaluationSchedule(
  providerChannelId: number,
  params: AdminUpsertChannelEvaluationScheduleRequest
): Promise<ApiResponse<ChannelEvaluationScheduleResponse>> {
  return apiClient.put(evaluationPath(providerChannelId, '/schedule'), params);
}
