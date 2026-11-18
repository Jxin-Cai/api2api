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
  source: 'ADMIN' | 'USER';
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
export function useCredentialQuotas(keyId: string) {
  return useQuery({
    queryKey: [...QUOTA_QUERY_KEY, 'credential', keyId],
    queryFn: async (): Promise<QuotaPage> => (await apiClient.get<QuotaPage>(`/api/api-credentials/${encodeURIComponent(keyId)}/token-quotas`)).data,
    refetchInterval: 30000,
    enabled: Boolean(keyId),
  });
}
export type GroupQuotaDraft = Omit<QuotaDraft, 'scope'> & { scope: 'MODEL_GROUP' };
export async function saveQuota(id: string | null, draft: GroupQuotaDraft): Promise<QuotaRule> {
  const response = id
    ? await apiClient.put<QuotaRule>(`/api/admin/token-quotas/${encodeURIComponent(id)}`, draft)
    : await apiClient.post<QuotaRule>('/api/admin/token-quotas', draft);
  return response.data;
}
export async function deleteQuota(id: string): Promise<void> {
  await apiClient.delete(`/api/admin/token-quotas/${encodeURIComponent(id)}`);
}

export type UserDailyQuotaDraft = Pick<QuotaDraft, 'model' | 'limitMillions' | 'enabled'>;
export async function saveCredentialQuota(keyId: string, ruleId: string | null, draft: UserDailyQuotaDraft): Promise<QuotaRule> {
  const path = `/api/api-credentials/${encodeURIComponent(keyId)}/token-quotas`;
  return (ruleId
    ? await apiClient.put<QuotaRule>(`${path}/${encodeURIComponent(ruleId)}`, draft)
    : await apiClient.post<QuotaRule>(path, draft)).data;
}
export async function deleteCredentialQuota(keyId: string, ruleId: string): Promise<void> {
  await apiClient.delete(`/api/api-credentials/${encodeURIComponent(keyId)}/token-quotas/${encodeURIComponent(ruleId)}`);
}
