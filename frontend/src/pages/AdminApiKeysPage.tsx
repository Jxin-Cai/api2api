import { AdminKeyQuotaPanel } from '@features/manage-token-quotas/AdminKeyQuotaPanel';
import { PageHeader } from '@shared/ui';

export default function AdminApiKeysPage() {
  return <div className="app-page">
    <PageHeader title="用户 API Key" description="按用户和 Key 管理不同模型的每日 Token 上限。" />
    <AdminKeyQuotaPanel />
  </div>;
}
