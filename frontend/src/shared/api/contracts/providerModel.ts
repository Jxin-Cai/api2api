export interface ProviderModelOptionResponse {
  model: string;
  providerCount: number;
  protocols: string[];
}

export interface ProviderModelOptionListResponse {
  models: ProviderModelOptionResponse[];
}
