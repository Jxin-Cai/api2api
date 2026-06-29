import { UsageRecordsPanel } from '@widgets/usage-records-panel';
import { PageHeader } from '@shared/ui';

export default function AdminUsagePage() {
  return (
    <div className="app-page">
      <PageHeader title="后台使用记录" />
      <UsageRecordsPanel scope="admin" />
    </div>
  );
}
