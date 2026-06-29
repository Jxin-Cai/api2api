export type Nullable<T> = T | null;
export type EntityId = string | number;
export type DateTimeString = string;
export type PageSize = 50 | 100 | 200;

export interface SelectOption<TValue extends string | number = string> {
  label: string;
  value: TValue;
}

export interface DateRangeValue {
  startAt?: DateTimeString;
  endAt?: DateTimeString;
}
