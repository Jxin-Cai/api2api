/** 模型名 -> 每日加权 Token 上限 */
export type ModelDailyLimits = Record<string, number>;

export interface ModelGroupResponse {
  id: string;
  name: string;
  modelWhitelist: string[];
  /** 已配置每日上限的模型及其上限 */
  modelDailyLimits: ModelDailyLimits;
  /** 已配置上限的模型当天（跨分组内所有 Key）已消耗的加权 Token */
  modelDailyUsage: Record<string, number>;
  /** 当天已触发每日上限、正在限流的模型 */
  rateLimitedModels: string[];
  /** 每日上限所依据的时区 */
  dailyLimitZoneId?: string;
  createdAt?: string | number;
  updatedAt?: string | number;
}

export interface ModelGroupListResponse {
  groups: ModelGroupResponse[];
}

export interface SaveModelGroupRequest {
  name: string;
  modelWhitelist: string[];
  modelDailyLimits: ModelDailyLimits;
}

export interface ModelGroupOption {
  label: string;
  value: string;
}
