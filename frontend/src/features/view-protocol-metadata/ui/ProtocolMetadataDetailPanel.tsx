import { Card, Collapse, Descriptions, Space, Spin, Tag, Typography } from 'antd';
import { ProtocolFieldTable, useProtocolMetadataDetail } from '@entities/protocol-metadata';
import { PageState } from '@shared/ui';

interface ProtocolMetadataDetailPanelProps {
  protocolType: string;
}

export function ProtocolMetadataDetailPanel({ protocolType }: ProtocolMetadataDetailPanelProps) {
  const { data, isLoading, isError, refetch } = useProtocolMetadataDetail(protocolType);
  const detail = data?.data ?? null;

  if (isLoading) return <Spin style={{ display: 'block', textAlign: 'center', padding: 24 }} />;
  if (isError) return <PageState status="error" onRetry={() => void refetch()} />;
  if (!detail) return null;

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Card size="small">
        <Descriptions size="small" column={{ xs: 1, sm: 2, md: 3 }} bordered>
          <Descriptions.Item label="协议名称">{detail.displayName}</Descriptions.Item>
          <Descriptions.Item label="API 规范版本">
            <Tag color="blue">{detail.apiSpecVersion}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="默认端点">
            <Typography.Text code>{detail.defaultEndpointPath}</Typography.Text>
          </Descriptions.Item>
          <Descriptions.Item label="总字段数">{detail.fieldCount}</Descriptions.Item>
          <Descriptions.Item label="入参字段">{detail.inputFieldCount}</Descriptions.Item>
          <Descriptions.Item label="出参字段">{detail.outputFieldCount}</Descriptions.Item>
          <Descriptions.Item label="描述" span={3}>{detail.description}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Collapse
        defaultActiveKey={detail.sections.slice(0, 3).map((s) => s.section)}
        items={detail.sections.map((section) => ({
          key: section.section,
          label: (
            <Space>
              <Typography.Text strong>{section.sectionLabel}</Typography.Text>
              <Tag>{section.fieldCount} 个字段</Tag>
            </Space>
          ),
          children: <ProtocolFieldTable fields={section.fields} />,
        }))}
      />
    </Space>
  );
}
