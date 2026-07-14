import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { ProtocolMetadataOverviewPanel } from '@features/view-protocol-metadata';
import { PageHeader } from '@shared/ui';

export default function AdminProtocolMetadataPage() {
  return (
    <div className="app-page">
      <PageHeader title="协议元数据" />
      <ProtocolMetadataOverviewPanel />
    </div>
  );
}
