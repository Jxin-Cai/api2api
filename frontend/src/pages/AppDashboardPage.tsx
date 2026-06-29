import { FrontDashboardPanel } from '@features/view-front-dashboard';
import { PageHeader } from '@shared/ui';

export default function AppDashboardPage() {
  return (
    <div className="app-page">
      <PageHeader title="前台仪表盘" />
      <FrontDashboardPanel />
    </div>
  );
}
