import { ProtocolConversionTablePanel } from '@features/view-protocol-conversions';
import { PageHeader } from '@shared/ui';

export default function AdminConversionsPage() {
  return (
    <div className="app-page">
      <PageHeader title="协议转换" />
      <ProtocolConversionTablePanel />
    </div>
  );
}
