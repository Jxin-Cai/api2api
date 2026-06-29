import { UsageRecordsPanel } from '@widgets/usage-records-panel';
import { PageHeader } from '@shared/ui';

export default function AppUsagePage() {
  return (
    <div className="app-page">
      <PageHeader title="前台使用记录" />
      <UsageRecordsPanel scope="front" />
    </div>
  );
}
