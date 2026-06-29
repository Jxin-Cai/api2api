import { ApiCredentialTablePanel } from '@features/manage-api-credentials';
import { PageHeader } from '@shared/ui';

export default function ApiKeysPage() {
  return (
    <div className="app-page">
      <PageHeader title="API Key 管理" />
      <ApiCredentialTablePanel />
    </div>
  );
}
