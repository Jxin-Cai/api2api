import { ChannelModelMappingManagementPanel } from '@features/manage-channel-models';
import { PageHeader } from '@shared/ui';

export default function AdminModelMappingsPage() {
  return (
    <div className="app-page">
      <PageHeader title="模型映射" />
      <ChannelModelMappingManagementPanel />
    </div>
  );
}
