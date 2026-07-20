export interface ModelGroupResponse {
  id: string;
  name: string;
  modelWhitelist: string[];
  createdAt?: string | number;
  updatedAt?: string | number;
}

export interface ModelGroupListResponse {
  groups: ModelGroupResponse[];
}

export interface SaveModelGroupRequest {
  name: string;
  modelWhitelist: string[];
}

export interface ModelGroupOption {
  label: string;
  value: string;
}
