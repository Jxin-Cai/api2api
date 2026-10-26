import { TokenQuotaPanel } from '@features/manage-token-quotas/TokenQuotaPanel';
import { PageHeader } from '@shared/ui';
export default function AdminTokenQuotasPage() {
  return <div className="app-page"><PageHeader title="限流" description="按模型分组或指定 API Key 管理 token 额度，统一以百万 token 配置。" /><TokenQuotaPanel /></div>;
}
