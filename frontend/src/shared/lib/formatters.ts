const TOKENS_PER_MILLION = 1_000_000;
const TOKENS_PER_BILLION = 1_000_000_000;

export type TokenCompactUnit = 'M' | 'B';

export interface TokenCompactParts {
  value: string;
  unit: TokenCompactUnit;
}

export function formatTokenMillionsValue(tokens?: number | null): string {
  if (tokens === undefined || tokens === null) {
    return '-';
  }
  return (tokens / TOKENS_PER_MILLION).toFixed(1);
}

export function formatTokenMillions(tokens?: number | null): string {
  const value = formatTokenMillionsValue(tokens);
  return value === '-' ? value : `${value}M`;
}

/** Split token totals into a 1-decimal value and M/B unit. Uses B at 1 billion tokens. */
export function formatTokenCompactParts(tokens?: number | null): TokenCompactParts | null {
  if (tokens === undefined || tokens === null || !Number.isFinite(tokens)) {
    return null;
  }
  if (tokens >= TOKENS_PER_BILLION) {
    return { value: (tokens / TOKENS_PER_BILLION).toFixed(1), unit: 'B' };
  }
  return { value: (tokens / TOKENS_PER_MILLION).toFixed(1), unit: 'M' };
}

/** Format token totals in millions, switching to billions at 1 billion tokens. */
export function formatTokenCompact(tokens?: number | null): string {
  const parts = formatTokenCompactParts(tokens);
  return parts == null ? '-' : `${parts.value}${parts.unit}`;
}

export function formatTokenThousands(tokens?: number | null, decimals = 1): string {
  if (tokens === undefined || tokens === null || !Number.isFinite(tokens)) {
    return '-';
  }
  return `${(tokens / 1_000).toFixed(decimals)}k`;
}

export function formatDateTime(value?: string | number | null): string {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return String(value);
  }
  return date.toLocaleString('zh-CN', { hour12: false });
}

export function formatId(value?: string | number | null, visible = 6): string {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  const text = String(value);
  if (text.length <= visible * 2 + 3) {
    return text;
  }
  return `${text.slice(0, visible)}...${text.slice(-visible)}`;
}
