import { TokenQuotaPanel } from '@features/manage-token-quotas/TokenQuotaPanel';
import { PageHeader } from '@shared/ui';
export default function AdminTokenQuotasPage() {
  return <div className="app-page"><PageHeader title="限流" description="配置模型分组共享额度，统一以百万 token 计量；Key 每日上限由用户在前台自行设置。" /><TokenQuotaPanel /></div>;
}
