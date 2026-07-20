import { useProviderModels } from '@entities/provider-model';
import { useModelGroups } from '@entities/model-group';
import { ApiCredentialTablePanel } from '@features/manage-api-credentials';
import { ModelGroupTablePanel } from '@features/manage-model-groups';
import { PageHeader } from '@shared/ui';
import { Tabs } from 'antd';

export default function ApiKeysPage() {
  const { modelOptions } = useProviderModels();
  const { options: groupOptions } = useModelGroups();

  return (
    <div className="app-page api-keys-page">
      <PageHeader
        title="API Keys"
        description="通过模型分组统一管理多个访问凭证的模型权限。"
      />
      <Tabs
        defaultActiveKey="keys"
        items={[
          { key: 'keys', label: 'API Keys', children: <ApiCredentialTablePanel groupOptions={groupOptions} /> },
          { key: 'groups', label: '模型分组', children: <ModelGroupTablePanel modelOptions={modelOptions} /> },
        ]}
      />
    </div>
  );
}
