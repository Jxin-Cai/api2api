import { useState } from 'react';
import { Col, Row, Space, Typography } from 'antd';
import { ProtocolMetadataCard, useProtocolMetadataList } from '@entities/protocol-metadata';
import { PageState } from '@shared/ui';
import { ProtocolMetadataDetailPanel } from './ProtocolMetadataDetailPanel';

export function ProtocolMetadataOverviewPanel() {
  const { protocols, isLoading, isError, refetch } = useProtocolMetadataList();
  const [selectedProtocol, setSelectedProtocol] = useState<string | null>(null);

  if (isLoading) return <PageState status="loading" />;
  if (isError) return <PageState status="error" onRetry={() => void refetch()} />;
  if (protocols.length === 0) return <PageState status="empty" title="暂无协议元数据" />;

  return (
    <Space direction="vertical" size={24} style={{ width: '100%' }}>
      <div>
        <Typography.Title level={5} style={{ marginBottom: 4 }}>支持的协议</Typography.Title>
        <Typography.Text type="secondary">点击查看协议的字段定义详情</Typography.Text>
      </div>
      <Row gutter={[16, 16]}>
        {protocols.map((protocol) => (
          <Col key={protocol.protocolType} xs={24} sm={12} lg={6}>
            <ProtocolMetadataCard
              protocol={protocol}
              selected={selectedProtocol === protocol.protocolType}
              onClick={() => setSelectedProtocol(
                selectedProtocol === protocol.protocolType ? null : protocol.protocolType
              )}
            />
          </Col>
        ))}
      </Row>
      {selectedProtocol ? <ProtocolMetadataDetailPanel protocolType={selectedProtocol} /> : null}
    </Space>
  );
}
