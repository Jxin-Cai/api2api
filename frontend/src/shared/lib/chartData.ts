import type { RankItem, TrendChartPoint } from '@shared/types/chart';

export interface TrendLikeDto {
  date?: string;
  bucket?: string;
  bucketStart?: string;
  bucketEnd?: string;
  label?: string;
  value?: number;
  tokens?: number;
  totalTokens?: number;
  category?: string;
  protocol?: string;
  protocolType?: string;
  providerChannelId?: string | number;
  providerChannel?: string;
  providerChannelName?: string;
}

export interface RankLikeDto {
  id?: string | number;
  userId?: string | number;
  userAccountId?: string | number;
  model?: string;
  name?: string;
  label?: string;
  displayName?: string;
  value?: number;
  tokens?: number;
  totalTokens?: number;
  meta?: string;
  username?: string;
}

export function normalizeTrendPoints(items: TrendLikeDto[] | undefined): TrendChartPoint[] {
  return (items ?? []).map((item: TrendLikeDto): TrendChartPoint => {
    const tokens = item.value ?? item.totalTokens ?? item.tokens ?? 0;
    return {
      date: item.date ?? item.bucketStart ?? item.bucket ?? item.label ?? '-',
      value: Number((tokens / 1_000).toFixed(1)),
      category: item.category ?? item.protocol ?? item.protocolType ?? item.providerChannelName ?? item.providerChannel ?? String(item.providerChannelId ?? '未归属渠道'),
    };
  });
}

export function normalizeRankItems(items: RankLikeDto[] | undefined, unit = 'k tokens'): RankItem[] {
  return (items ?? []).map((item: RankLikeDto, index: number): RankItem => {
    const identity = item.id ?? item.userId ?? item.userAccountId ?? item.model ?? item.label ?? index + 1;
    return {
      id: String(identity),
      label: item.label ?? item.displayName ?? item.username ?? item.name ?? item.model ?? String(identity),
      value: Number(((item.value ?? item.totalTokens ?? item.tokens ?? 0) / 1_000).toFixed(1)),
      unit,
      meta: item.meta ?? item.username,
    };
  });
}
