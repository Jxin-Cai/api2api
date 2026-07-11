export function formatTokenMillions(tokens?: number | null): string {
  if (tokens === undefined || tokens === null) {
    return '-';
  }
  return `${(tokens / 1_000_000).toFixed(1)}M`;
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

export function formatUnknown(value?: string | number | null): string {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  return String(value);
}
