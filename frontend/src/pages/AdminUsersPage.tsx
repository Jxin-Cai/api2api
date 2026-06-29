import { UserAccountActionPanel } from '@features/manage-user-accounts';
import { PageHeader } from '@shared/ui';

export default function AdminUsersPage() {
  return (
    <div className="app-page">
      <PageHeader title="后台用户管理" />
      <UserAccountActionPanel />
    </div>
  );
}
