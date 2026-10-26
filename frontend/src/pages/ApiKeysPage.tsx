import { useModelGroups } from '@entities/model-group';
import { ApiCredentialTablePanel } from '@features/manage-api-credentials';
import { PageHeader } from '@shared/ui';

export default function ApiKeysPage() {
  const { options: groupOptions } = useModelGroups();
  return (
    <div className="app-page api-keys-page">
      <PageHeader title="API Keys" description="选择管理员配置的模型分组，为应用创建访问凭证。" />
      <ApiCredentialTablePanel groupOptions={groupOptions} />
    </div>
  );
}
