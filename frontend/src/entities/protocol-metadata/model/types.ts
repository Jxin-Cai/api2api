export interface ProtocolMetadataListItemResponse {
  id: number;
  protocolType: string;
  displayName: string;
  apiSpecVersion: string;
  description: string;
  defaultEndpointPath: string;
  fieldCount: number;
  inputFieldCount: number;
  outputFieldCount: number;
}

export interface ProtocolMetadataListResponse {
  protocols: ProtocolMetadataListItemResponse[];
}

export interface ProtocolFieldDefinitionItemResponse {
  id: number;
  fieldName: string;
  fieldPath: string;
  fieldType: string;
  required: boolean;
  usageDirection: string;
  description: string;
  purpose: string;
  usageContext: string;
  sortOrder: number;
}

export interface ProtocolFieldSectionResponse {
  section: string;
  sectionLabel: string;
  fieldCount: number;
  fields: ProtocolFieldDefinitionItemResponse[];
}

export interface ProtocolMetadataDetailResponse {
  id: number;
  protocolType: string;
  displayName: string;
  apiSpecVersion: string;
  description: string;
  defaultEndpointPath: string;
  sections: ProtocolFieldSectionResponse[];
  fieldCount: number;
  inputFieldCount: number;
  outputFieldCount: number;
  createdAt: number;
  updatedAt: number;
}
