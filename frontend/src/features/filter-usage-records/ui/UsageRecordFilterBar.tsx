import { Button, DatePicker, Input, Select, Space } from 'antd';
import type { Dayjs } from 'dayjs';

import { useApiCredentials } from '@entities/api-credential';
import { useProviderChannels } from '@entities/provider-channel';
import { PROTOCOL_OPTIONS } from '@shared/lib/protocols';

import type { UsageRecordFilters, UsageScope } from '../model/types';

interface UsageRecordFilterBarProps {
  /** 前台或后台范围 */
  scope: UsageScope;
  /** 当前筛选条件 */
  filters: UsageRecordFilters;
  /** 更新单个筛选条件 */
  onFilterChange: <TKey extends keyof UsageRecordFilters>(key: TKey, value: UsageRecordFilters[TKey]) => void;
  /** 批量更新筛选条件 */
  onFiltersChange: (partial: Partial<UsageRecordFilters>) => void;
  /** 重置筛选条件 */
  onReset: () => void;
  /** 是否禁用筛选控件 */
  disabled?: boolean;
}

function toIsoString(value: Dayjs | null): string | undefined {
  return value ? value.toISOString() : undefined;
}

export function UsageRecordFilterBar({ scope, filters, onFilterChange, onFiltersChange, onReset, disabled = false }: UsageRecordFilterBarProps) {
  const isAdmin = scope === 'admin';
  const { options: apiCredentialOptions, query: apiCredentialQuery } = useApiCredentials();
  const { channelOptions, isLoading: channelLoading } = useProviderChannels({ enabled: isAdmin });
  const providerChannelOptions = channelOptions.map((option: { label: string; value: number }) => ({
    label: option.label,
    value: String(option.value),
  }));

  return (
    <Space wrap align="start">
      <Select
        allowClear
        showSearch
        value={filters.apiCredentialId}
        placeholder="API Key"
        style={{ width: 200 }}
        options={apiCredentialOptions}
        loading={apiCredentialQuery.isLoading}
        disabled={disabled}
        onChange={(value: string | undefined): void => onFilterChange('apiCredentialId', value)}
      />
      <Input
        allowClear
        value={filters.model}
        placeholder="模型"
        style={{ width: 180 }}
        disabled={disabled}
        onChange={(event): void => onFilterChange('model', event.target.value || undefined)}
      />
      <Select
        allowClear
        value={filters.protocolType}
        placeholder="协议"
        style={{ width: 150 }}
        options={PROTOCOL_OPTIONS}
        disabled={disabled}
        onChange={(value: string | undefined): void => onFilterChange('protocolType', value)}
      />
      <DatePicker.RangePicker
        showTime
        disabled={disabled}
        onChange={(values: [Dayjs | null, Dayjs | null] | null): void => {
          onFiltersChange({
            startTime: toIsoString(values?.[0] ?? null),
            endTime: toIsoString(values?.[1] ?? null),
          });
        }}
      />
      {scope === 'admin' ? (
        <>
          <Input
            allowClear
            value={filters.userId}
            placeholder="用户 ID"
            style={{ width: 160 }}
            disabled={disabled}
            onChange={(event): void => onFilterChange('userId', event.target.value || undefined)}
          />
          <Select
            allowClear
            showSearch
            value={filters.providerChannelId}
            placeholder="供应商渠道"
            style={{ width: 200 }}
            options={providerChannelOptions}
            loading={channelLoading}
            disabled={disabled}
            onChange={(value: string | undefined): void => onFilterChange('providerChannelId', value)}
          />
        </>
      ) : null}
      <Button onClick={onReset} disabled={disabled}>重置</Button>
    </Space>
  );
}
