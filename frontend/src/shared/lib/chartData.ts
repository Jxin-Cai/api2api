import type { RankItem, TrendChartPoint } from '@shared/types/chart';

import { formatTokenMillionsValue } from './formatters';

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
  credentialId?: string | number;
  credentialName?: string;
}

export interface RankLikeDto {
  id?: string | number;
  userId?: string | number;
  userAccountId?: string | number;
  credentialId?: string | number;
  model?: string;
  name?: string;
  label?: string;
  displayName?: string;
  credentialName?: string;
  value?: number;
  tokens?: number;
  totalTokens?: number;
  meta?: string;
  username?: string;
}

/**
 * 将后端 ISO Instant（如 2026-08-27T00:00:00Z）格式化为短日期 MM-DD；
 * 非日期字符串原样返回。
 */
export function formatBucketDate(raw: string): string {
  const parsed = new Date(raw);
  if (Number.isNaN(parsed.getTime())) {
    return raw;
  }
  const month = String(parsed.getMonth() + 1).padStart(2, '0');
  const day = String(parsed.getDate()).padStart(2, '0');
  return `${month}-${day}`;
}

export interface ConcurrencyLikeDto {
  bucketStart?: string;
  bucketEnd?: string;
  peakConcurrency?: number;
  credentialId?: string | number;
  credentialName?: string;
}

/** 将后端 ISO Instant 格式化为当日时刻 HH:mm；非日期字符串原样返回。 */
export function formatBucketTime(raw: string): string {
  const parsed = new Date(raw);
  if (Number.isNaN(parsed.getTime())) {
    return raw;
  }
  const hours = String(parsed.getHours()).padStart(2, '0');
  const minutes = String(parsed.getMinutes()).padStart(2, '0');
  return `${hours}:${minutes}`;
}

/**
 * 并发曲线点：X 轴为桶起始时刻，Y 轴为峰值并发（整数）。
 * 未带凭证信息的点归入单一“全平台”系列。
 */
export function normalizeConcurrencyPoints(items: ConcurrencyLikeDto[] | undefined, defaultCategory = '全平台'): TrendChartPoint[] {
  return (items ?? []).map((item: ConcurrencyLikeDto): TrendChartPoint => ({
    date: formatBucketTime(item.bucketStart ?? '-'),
    value: Math.max(0, Math.round(Number(item.peakConcurrency ?? 0)) || 0),
    category: item.credentialName ?? (item.credentialId === undefined ? defaultCategory : String(item.credentialId)),
  }));
}

export function normalizeTrendPoints(items: TrendLikeDto[] | undefined): TrendChartPoint[] {
  return (items ?? []).map((item: TrendLikeDto): TrendChartPoint => {
    const tokens = item.value ?? item.totalTokens ?? item.tokens ?? 0;
    const rawDate = item.date ?? item.bucketStart ?? item.bucket ?? item.label ?? '-';
    return {
      date: formatBucketDate(rawDate),
      value: Number(formatTokenMillionsValue(tokens)),
      category: item.category
        ?? item.protocol
        ?? item.protocolType
        ?? item.credentialName
        ?? item.providerChannelName
        ?? item.providerChannel
        ?? String(item.credentialId ?? item.providerChannelId ?? '未归属渠道'),
    };
  });
}

export function normalizeRankItems(items: RankLikeDto[] | undefined, unit = 'M'): RankItem[] {
  return (items ?? []).map((item: RankLikeDto, index: number): RankItem => {
    const identity = item.id ?? item.userId ?? item.userAccountId ?? item.credentialId ?? item.model ?? item.label ?? index + 1;
    const tokens = Number(item.value ?? item.totalTokens ?? item.tokens ?? 0);
    const safeTokens = Number.isFinite(tokens) ? Math.max(0, tokens) : 0;
    return {
      id: String(identity),
      label: item.label ?? item.displayName ?? item.username ?? item.credentialName ?? item.name ?? item.model ?? String(identity),
      value: Number(formatTokenMillionsValue(safeTokens)),
      unit,
      meta: item.meta ?? item.username,
      rawValue: safeTokens,
    };
  });
}
