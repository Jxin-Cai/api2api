export type EvaluationStatus = 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | string;
export type EvaluationTrigger = 'MANUAL' | 'SCHEDULED' | string;
export type EvaluationSortField = 'REQUESTED_AT' | 'SCORE';

export interface ChannelEvaluationResponse {
  id: number;
  providerChannelId: number;
  requestedModel: string;
  upstreamFormat: string;
  providerRunId?: string | null;
  status: EvaluationStatus;
  trigger: EvaluationTrigger;
  score?: number | null;
  detectedFamily?: string | null;
  detectedModel?: string | null;
  detectedConfidence?: number | null;
  familyMismatch?: boolean | null;
  channelSignature?: string | null;
  reportUrl?: string | null;
  passedProbeCount?: number | null;
  warningProbeCount?: number | null;
  failedProbeCount?: number | null;
  totalInputTokens?: number | null;
  totalOutputTokens?: number | null;
  errorMessage?: string | null;
  reportSummary?: string | null;
  requestedAt?: number | null;
  startedAt?: number | null;
  completedAt?: number | null;
  createdAt?: number | null;
  updatedAt?: number | null;
}

export interface ChannelEvaluationScoreSummaryResponse {
  totalCount: number;
  scoredCount: number;
  failedCount: number;
  averageScore?: number | null;
  minScore?: number | null;
  maxScore?: number | null;
}

export interface ChannelEvaluationHistoryResponse {
  evaluations: ChannelEvaluationResponse[];
  summary: ChannelEvaluationScoreSummaryResponse;
  totalElements: number;
  limit: number;
  offset: number;
}

export interface ChannelEvaluationSubmitResponse {
  evaluations: ChannelEvaluationResponse[];
}

export interface ChannelEvaluationScheduleResponse {
  id: number;
  providerChannelId: number;
  cronExpression: string;
  zoneId: string;
  models: string[];
  enabled: boolean;
  lastTriggeredAt?: number | null;
  nextTriggerAt?: number | null;
  createdAt?: number | null;
  updatedAt?: number | null;
}

export interface AdminSubmitChannelEvaluationRequest {
  models?: string[];
}

export interface QueryChannelEvaluationHistoryRequest {
  requestedModel?: string;
  status?: EvaluationStatus;
  from?: string;
  to?: string;
  sortField?: EvaluationSortField;
  descending?: boolean;
  limit?: number;
  offset?: number;
}

export interface AdminUpsertChannelEvaluationScheduleRequest {
  cronExpression: string;
  zoneId?: string;
  models?: string[];
  enabled?: boolean;
}
