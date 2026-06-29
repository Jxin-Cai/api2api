import type { UsageRecordFilters } from '@entities/usage-record';

export interface AdminDashboardPanelProps {
  zoneId?: string;
}

export type AdminDashboardDrillDown = Pick<UsageRecordFilters, 'userId' | 'providerChannelId' | 'protocolType' | 'model'>;
