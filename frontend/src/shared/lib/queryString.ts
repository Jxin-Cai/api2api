export type QueryPrimitive = string | number | boolean | null | undefined;
export type QueryObject = Record<string, QueryPrimitive | readonly QueryPrimitive[]>;

export function toQueryString(params: QueryObject, existingQuery = ''): string {
  const search = new URLSearchParams(existingQuery);
  Object.entries(params).forEach(([key, value]: [string, QueryPrimitive | readonly QueryPrimitive[]]): void => {
    const values: readonly QueryPrimitive[] = Array.isArray(value) ? value : [value];
    values.forEach((item: QueryPrimitive): void => {
      if (item !== undefined && item !== null && item !== '') {
        search.append(key, String(item));
      }
    });
  });
  return search.toString();
}
