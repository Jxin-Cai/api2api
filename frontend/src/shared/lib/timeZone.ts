export function browserTimeZone(): string {
  const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone?.trim();
  return timeZone ? timeZone : 'UTC';
}

export function resolveTimeZone(zoneId?: string): string {
  const trimmed = zoneId?.trim();
  return trimmed ? trimmed : browserTimeZone();
}
