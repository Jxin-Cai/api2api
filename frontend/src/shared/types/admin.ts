export type AdminId = number | string;
export type AdminRole = 'ADMIN' | 'USER';
export type AdminStatus = 'ACTIVE' | 'ENABLED' | 'DISABLED';
export type EnabledStatus = 'ENABLED' | 'DISABLED';
export type ModelSource = 'FETCHED' | 'MANUAL' | string;
export type ProtocolConversionStatus = 'ENABLED' | 'DISABLED' | string;
export type ImplementationStatus = 'IMPLEMENTED' | 'PARTIAL' | 'NOT_IMPLEMENTED' | string;

export interface SelectOption<T extends string | number = string> {
  label: string;
  value: T;
}
