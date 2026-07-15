import { ProtocolConversionTablePanel } from '@features/view-protocol-conversions';
import { ProtocolMetadataOverviewPanel } from '@features/view-protocol-metadata';
import { PageHeader } from '@shared/ui';

export default function AdminConversionsPage() {
  return (
    <div className="app-page">
      <PageHeader title="协议转换" description="查看支持的协议规范与协议间字段映射" />
      <section aria-label="支持的协议">
        <ProtocolMetadataOverviewPanel />
      </section>
      <section aria-label="转换映射">
        <ProtocolConversionTablePanel />
      </section>
    </div>
  );
}
