import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@shared/api';

export type QuotaScope = 'MODEL_GROUP' | 'API_KEY';
export type QuotaPeriod = 'DAY' | 'WEEK' | 'MONTH' | 'LIFETIME';
export interface QuotaDraft {
  scope: QuotaScope;
  targetId: string;
  model: string;
  period: QuotaPeriod;
  limitMillions: number;
  enabled: boolean;
}
export interface QuotaRule extends QuotaDraft {
  id: string;
  usedMillions: number;
  remainingMillions: number;
  limited: boolean;
  resetAt: string | null;
}
export interface QuotaTarget { scope: QuotaScope; id: string; name: string; ownerName: string; modelGroupId: string }
export interface QuotaPage { rules: QuotaRule[]; targets: QuotaTarget[]; zoneId: string }
export const QUOTA_QUERY_KEY = ['token-quotas'] as const;
const EMPTY_TARGETS: QuotaTarget[] = [];
const EMPTY_RULES: QuotaRule[] = [];
export function useTokenQuotas() {
  const query = useQuery({
    queryKey: QUOTA_QUERY_KEY,
    queryFn: async (): Promise<QuotaPage> => (await apiClient.get<QuotaPage>('/api/admin/token-quotas')).data,
    refetchInterval: 30000,
  });
  return { query, rules: query.data?.rules ?? EMPTY_RULES, targets: query.data?.targets ?? EMPTY_TARGETS, zoneId: query.data?.zoneId ?? 'Asia/Shanghai' };
}
export async function saveQuota(id: string | null, draft: QuotaDraft): Promise<QuotaRule> {
  const response = id
    ? await apiClient.put<QuotaRule>(`/api/admin/token-quotas/${encodeURIComponent(id)}`, draft)
    : await apiClient.post<QuotaRule>('/api/admin/token-quotas', draft);
  return response.data;
}
export async function deleteQuota(id: string): Promise<void> {
  await apiClient.delete(`/api/admin/token-quotas/${encodeURIComponent(id)}`);
}
