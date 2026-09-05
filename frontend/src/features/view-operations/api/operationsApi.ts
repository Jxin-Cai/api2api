import { useQuery } from '@tanstack/react-query';
import type { ChannelLatencyRankingResponse, ConcurrencyTrendPointResponse } from '@entities/dashboard-metric';
import { apiClient } from '@shared/api';
import { resolveTimeZone } from '@shared/lib/timeZone';

export interface Resource {
  usedBytes: number | null;
  totalBytes: number | null;
  percent: number | null;
}

export interface SystemSnapshot {
  sampledAt: string;
  scope: 'HOST_KERNEL' | 'JVM_VISIBLE';
  operatingSystem: string;
  cpuCores: number | null;
  cpuPercent: number | null;
  loadAverage: number[];
  loadPercent: number | null;
  memory: Resource;
  disk: Resource;
  diskScope: string;
  runtime: { uptimeMillis: number; heap: Resource; threads: number; processCpuPercent: number | null };
  databasePool: { active: number; idle: number; total: number; maximum: number; waiting: number } | null;
  health: 'HEALTHY' | 'WARNING' | 'CRITICAL' | 'UNKNOWN';
  notices: string[];
}

interface TrafficSnapshot {
  sampledAt: string;
  zoneId: string;
  todayConcurrencyTrends: ConcurrencyTrendPointResponse[];
  dailySlowestChannels: ChannelLatencyRankingResponse[];
}

export function useSystemMetrics(live: boolean) {
  return useQuery({
    queryKey: ['operations', 'system'],
    queryFn: async () => (await apiClient.get<SystemSnapshot>('/api/admin/operations/system')).data,
    refetchInterval: live ? 10_000 : false,
    refetchOnWindowFocus: live,
    retry: false,
  });
}

export function useOperationsTraffic(live: boolean) {
  const zoneId = resolveTimeZone();
  return useQuery({
    queryKey: ['operations', 'traffic', zoneId],
    queryFn: async () => (await apiClient.get<TrafficSnapshot>('/api/admin/operations/traffic', { zoneId })).data,
    refetchInterval: live ? 30_000 : false,
    refetchOnWindowFocus: live,
    retry: false,
  });
}
