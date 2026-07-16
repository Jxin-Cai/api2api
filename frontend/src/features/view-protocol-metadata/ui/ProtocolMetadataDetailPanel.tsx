import { ExportOutlined } from '@ant-design/icons';
import { Alert, Card, Collapse, Descriptions, Space, Spin, Tag, Typography } from 'antd';
import { ProtocolFieldTable, useProtocolMetadataDetail } from '@entities/protocol-metadata';
import { getProtocolMeta } from '@shared/lib/protocols';
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
  const protocolMeta = getProtocolMeta(detail.protocolType);

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
          <Descriptions.Item label="官方规范" span={2}>
            {protocolMeta.officialSpecUrl ? (
              <Typography.Link href={protocolMeta.officialSpecUrl} target="_blank" rel="noreferrer">
                {protocolMeta.referenceVersion ?? '官方 API Reference'} <ExportOutlined />
              </Typography.Link>
            ) : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="核验日期">{protocolMeta.verifiedAt ?? '-'}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Alert
        type="info"
        showIcon
        message="字段来源与运行时支持是两层信息"
        description="下方列出官方协议字段；具体跨协议是否支持、是否有损及触发条件，请在转换详情的逐层映射中查看。未知字段仍由转换器 fail-closed 拒绝。"
      />

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
