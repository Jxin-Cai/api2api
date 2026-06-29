import { Select, Space, Typography } from 'antd';

import { USAGE_PAGE_SIZE_OPTIONS } from '@shared/lib/table';
import type { UsagePageSize } from '@shared/types/table';

interface UsagePageSizeSelectorProps {
  /** 当前每页条数 */
  value: UsagePageSize;
  /** 每页条数变化回调 */
  onChange: (pageSize: UsagePageSize) => void;
  /** 是否禁用 */
  disabled?: boolean;
}

export function UsagePageSizeSelector({ value, onChange, disabled = false }: UsagePageSizeSelectorProps) {
  return (
    <Space>
      <Typography.Text type="secondary">每页</Typography.Text>
      <Select<UsagePageSize>
        value={value}
        disabled={disabled}
        style={{ width: 96 }}
        options={USAGE_PAGE_SIZE_OPTIONS.map((size: UsagePageSize) => ({ label: `${size} 条`, value: size }))}
        onChange={(nextValue: UsagePageSize): void => onChange(nextValue)}
      />
    </Space>
  );
}
