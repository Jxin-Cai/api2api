import { useProviderModels } from '@entities/provider-model';
import { ModelGroupTablePanel } from '@features/manage-model-groups';
import { PageHeader } from '@shared/ui';
export default function AdminModelGroupsPage() {
  const { modelOptions } = useProviderModels();
  return <div className="app-page"><PageHeader title="模型分组" description="管理员统一配置模型权限，前台 API Key 选择分组后即可使用。" /><ModelGroupTablePanel modelOptions={modelOptions} /></div>;
}
