import { ExportOutlined } from '@ant-design/icons';
import { Alert, Collapse, Space, Spin, Tag, Typography } from 'antd';
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
    <Space className="protocol-metadata-detail" direction="vertical" size={20}>
      <header className="protocol-metadata-detail__header">
        <div className="protocol-metadata-detail__heading">
          <div>
            <Typography.Title level={3}>{detail.displayName}</Typography.Title>
            <Typography.Paragraph type="secondary">{detail.description}</Typography.Paragraph>
          </div>
          <Tag className="protocol-metadata-detail__version">{detail.apiSpecVersion}</Tag>
        </div>

        <dl className="protocol-metadata-detail__facts">
          <div className="protocol-metadata-detail__fact protocol-metadata-detail__fact--endpoint">
            <dt>默认端点</dt>
            <dd><Typography.Text code>{detail.defaultEndpointPath}</Typography.Text></dd>
          </div>
          <div className="protocol-metadata-detail__fact">
            <dt>字段总数</dt>
            <dd>{detail.fieldCount}</dd>
          </div>
          <div className="protocol-metadata-detail__fact">
            <dt>入参字段</dt>
            <dd>{detail.inputFieldCount}</dd>
          </div>
          <div className="protocol-metadata-detail__fact">
            <dt>出参字段</dt>
            <dd>{detail.outputFieldCount}</dd>
          </div>
        </dl>

        <div className="protocol-metadata-detail__reference">
          {protocolMeta.officialSpecUrl ? (
            <Typography.Link href={protocolMeta.officialSpecUrl} target="_blank" rel="noreferrer">
              {protocolMeta.referenceVersion ?? '查看官方 API Reference'} <ExportOutlined />
            </Typography.Link>
          ) : null}
          <Typography.Text type="secondary">核验日期 {protocolMeta.verifiedAt ?? '-'}</Typography.Text>
        </div>
      </header>

      <Alert
        className="protocol-metadata-detail__notice"
        type="info"
        showIcon
        message="字段来源与运行时支持是两层信息"
        description="下方列出官方协议字段；具体跨协议是否支持、是否有损及触发条件，请在转换详情的逐层映射中查看。未知字段仍由转换器 fail-closed 拒绝。"
      />

      <Collapse
        className="protocol-metadata-detail__sections"
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
