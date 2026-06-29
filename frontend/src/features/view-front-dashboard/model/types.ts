import type { UsageRecordFilters } from '@entities/usage-record';

export interface FrontDashboardPanelProps {
  zoneId?: string;
}

export type FrontDashboardDrillDown = Pick<UsageRecordFilters, 'apiCredentialId' | 'model' | 'protocolType'>;
