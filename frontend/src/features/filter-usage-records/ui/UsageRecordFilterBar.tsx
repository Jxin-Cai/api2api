import { Button, DatePicker, Input, Select, Space } from 'antd';
import type { Dayjs } from 'dayjs';
import { useMemo } from 'react';

import { useApiCredentials } from '@entities/api-credential';
import { useProviderChannels, type ProviderChannelResponse } from '@entities/provider-channel';
import { useProviderModels } from '@entities/provider-model';
import { PROTOCOL_OPTIONS } from '@shared/lib/protocols';

import type { UsageFilterOption, UsageRecordFilters, UsageScope } from '../model/types';

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

function toChannelModelOptions(channels: ProviderChannelResponse[]): UsageFilterOption[] {
  const modelNames = new Set<string>();
  channels.forEach((channel) => {
    (channel.supportedModels ?? []).forEach((model) => {
      const name = model.requestedModel?.trim();
      if (name) {
        modelNames.add(name);
      }
    });
  });
  return [...modelNames]
    .sort((left, right) => left.localeCompare(right, 'zh-CN'))
    .map((name) => ({ label: name, value: name }));
}

function withCurrentModelOption(options: UsageFilterOption[], currentModel: string | undefined): UsageFilterOption[] {
  if (!currentModel || options.some((option) => option.value === currentModel)) {
    return options;
  }
  return [{ label: currentModel, value: currentModel }, ...options];
}

export function UsageRecordFilterBar({ scope, filters, onFilterChange, onFiltersChange, onReset, disabled = false }: UsageRecordFilterBarProps) {
  const isAdmin = scope === 'admin';
  const { options: apiCredentialOptions, query: apiCredentialQuery } = useApiCredentials();
  const { modelOptions: portalModelOptions, isLoading: portalModelLoading } = useProviderModels({ enabled: !isAdmin });
  const { channels, channelOptions, isLoading: channelLoading } = useProviderChannels({ enabled: isAdmin });
  const providerChannelOptions = channelOptions.map((option: { label: string; value: number }) => ({
    label: option.label,
    value: String(option.value),
  }));
  const selectableModelOptions = useMemo(
    () => withCurrentModelOption(
      isAdmin ? toChannelModelOptions(channels) : portalModelOptions,
      filters.model
    ),
    [channels, filters.model, isAdmin, portalModelOptions]
  );
  const modelLoading = isAdmin ? channelLoading : portalModelLoading;

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
      <Select
        allowClear
        showSearch
        optionFilterProp="label"
        value={filters.model}
        placeholder="模型"
        style={{ width: 240 }}
        options={selectableModelOptions}
        loading={modelLoading}
        disabled={disabled}
        onChange={(value: string | undefined): void => onFilterChange('model', value)}
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
