import { Col, Row, Space, Typography } from 'antd';
import { useSearchParams } from 'react-router-dom';
import { ProtocolMetadataCard, useProtocolMetadataList } from '@entities/protocol-metadata';
import { PageState } from '@shared/ui';
import { ProtocolMetadataDetailModal } from './ProtocolMetadataDetailModal';

export function ProtocolMetadataOverviewPanel() {
  const { protocols, isLoading, isError, refetch } = useProtocolMetadataList();
  const [searchParams, setSearchParams] = useSearchParams();
  const selectedProtocol = searchParams.get('protocol');

  function handleProtocolSelect(protocolType: string): void {
    const nextParams = new URLSearchParams(searchParams);
    nextParams.set('protocol', protocolType);
    setSearchParams(nextParams);
  }

  function handleModalClose(): void {
    const nextParams = new URLSearchParams(searchParams);
    nextParams.delete('protocol');
    setSearchParams(nextParams);
  }

  if (isLoading) return <PageState status="loading" />;
  if (isError) return <PageState status="error" onRetry={() => void refetch()} />;
  if (protocols.length === 0) return <PageState status="empty" title="暂无协议元数据" />;

  return (
    <Space direction="vertical" size={24} style={{ width: '100%' }}>
      <div>
        <Typography.Title level={5} style={{ marginBottom: 4 }}>支持的协议</Typography.Title>
        <Typography.Text type="secondary">点击协议卡片，在弹窗中查看官方规范与字段定义</Typography.Text>
      </div>
      <Row gutter={[16, 16]}>
        {protocols.map((protocol) => (
          <Col key={protocol.protocolType} xs={24} sm={12} lg={6}>
            <ProtocolMetadataCard
              protocol={protocol}
              selected={selectedProtocol === protocol.protocolType}
              onClick={() => handleProtocolSelect(protocol.protocolType)}
            />
          </Col>
        ))}
      </Row>
      <ProtocolMetadataDetailModal
        protocolType={selectedProtocol}
        open={selectedProtocol !== null}
        onClose={handleModalClose}
      />
    </Space>
  );
}
