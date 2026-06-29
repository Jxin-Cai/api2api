import { ProviderChannelTablePanel } from '@features/manage-provider-channels';
import { ChannelModelsPanel } from '@widgets/channel-models-panel';
import { PageHeader } from '@shared/ui';

export default function AdminChannelsPage() {
  return (
    <div className="app-page">
      <PageHeader title="供应商渠道" />
      <ProviderChannelTablePanel renderModelsPanel={(channel, onChanged) => <ChannelModelsPanel channel={channel} onChannelChanged={onChanged} />} />
    </div>
  );
}
