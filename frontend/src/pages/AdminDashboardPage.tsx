import { AdminDashboardPanel } from '@features/view-admin-dashboard';
import { PageHeader } from '@shared/ui';

export default function AdminDashboardPage() {
  return (
    <div className="app-page">
      <PageHeader title="后台仪表盘" />
      <AdminDashboardPanel />
    </div>
  );
}
