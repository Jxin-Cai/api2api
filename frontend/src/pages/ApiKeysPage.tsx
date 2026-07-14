import { useProviderModels } from '@entities/provider-model';
import { ApiCredentialTablePanel } from '@features/manage-api-credentials';
import { PageHeader } from '@shared/ui';

export default function ApiKeysPage() {
  const { modelOptions } = useProviderModels();

  return (
    <div className="app-page api-keys-page">
      <PageHeader
        title="API Keys"
        description="创建和管理用于网关请求的访问凭证。"
      />
      <ApiCredentialTablePanel modelOptions={modelOptions} />
    </div>
  );
}
