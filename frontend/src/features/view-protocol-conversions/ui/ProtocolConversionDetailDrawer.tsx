import { useEffect, useMemo, useState } from 'react';
import { Alert, Button, Card, Descriptions, Drawer, Grid, Popconfirm, Space, Statistic, Tag, Typography } from 'antd';
import { CapabilityTags, buildMappingView, type ProtocolConversionMappingResponse, useProtocolConversionDetail, type ProtocolConversionResponse } from '@entities/protocol-conversion';
import { ProtocolMetadataLink } from '@entities/protocol-metadata';
import { ProtocolMappingPanel } from '@widgets/protocol-mapping-panel';
import { formatProtocolDirection } from '@shared/lib/protocols';
import { PageState } from '@shared/ui';
import { useProtocolConversionMutations } from '../model/useProtocolConversionMutations';
import type { ProtocolConversionActiveTab } from '../model/types';

interface ProtocolConversionDetailDrawerProps {
  /** 打开状态 */
  open: boolean;
  /** 转换定义 ID */
  definitionId?: string | null;
  /** 已加载的转换详情 */
  conversion?: ProtocolConversionResponse | null;
  /** 关闭回调 */
  onClose: () => void;
  /** 状态变更回调 */
  onStatusChanged: (conversion: ProtocolConversionResponse) => void;
}

export function ProtocolConversionDetailDrawer({ open, definitionId = null, conversion = null, onClose, onStatusChanged }: ProtocolConversionDetailDrawerProps) {
  const detailQuery = useProtocolConversionDetail(definitionId, open && conversion === null);
  const { enableMutation, disableMutation } = useProtocolConversionMutations();
  const [current, setCurrent] = useState<ProtocolConversionResponse | null>(conversion);
  const [activeTab, setActiveTab] = useState<ProtocolConversionActiveTab>('request');
  const screens = Grid.useBreakpoint();
  const drawerWidth = screens.lg ? 960 : screens.md ? 760 : '100vw';

  useEffect(() => setCurrent(conversion ?? detailQuery.data?.data ?? null), [conversion, detailQuery.data]);

  useEffect(() => {
    if (open) {
      setActiveTab('request');
    }
  }, [open, definitionId]);

  async function handleToggle(): Promise<void> {
    if (!current) return;
    const response = current.status === 'ENABLED' ? await disableMutation.mutateAsync(current.id) : await enableMutation.mutateAsync(current.id);
    setCurrent(response.data);
    onStatusChanged(response.data);
  }

  const statusChanging = enableMutation.isPending || disableMutation.isPending;

  return (
    <Drawer title="协议转换详情" open={open} onClose={onClose} width={drawerWidth} destroyOnClose>
      {detailQuery.isLoading ? <PageState status="loading" /> : null}
      {detailQuery.isError ? <PageState status="error" onRetry={() => void detailQuery.refetch()} /> : null}
      {!detailQuery.isLoading && !current ? <PageState status="empty" title="暂无转换详情" /> : null}
      {current ? (
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Card size="small">
            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <Space wrap style={{ justifyContent: 'space-between', width: '100%' }} align="start">
                <div>
                  <Typography.Title level={4} style={{ marginBottom: 4 }}>
                    {formatProtocolDirection(current.sourceProtocol, current.targetProtocol)}
                  </Typography.Title>
                  <Typography.Text type="secondary">请求与响应字段在协议间的转换定义</Typography.Text>
                </div>
                <Space wrap>
                  <StatusTag status={current.status} />
                  <ImplementationStatusTag status={current.implementationStatus} />
                  <Popconfirm title={`确认${current.status === 'ENABLED' ? '禁用' : '启用'}该转换定义？`} onConfirm={handleToggle}>
                    <Button loading={statusChanging}>{current.status === 'ENABLED' ? '禁用' : '启用'}</Button>
                  </Popconfirm>
                </Space>
              </Space>
              <Descriptions size="small" column={{ xs: 1, sm: 2 }} bordered>
                <Descriptions.Item label="ID">{current.id}</Descriptions.Item>
                <Descriptions.Item label="Kind">{current.kind}</Descriptions.Item>
                <Descriptions.Item label="源协议">
                  <Space>{current.sourceProtocol}<ProtocolMetadataLink protocolType={current.sourceProtocol} label="字段定义" /></Space>
                </Descriptions.Item>
                <Descriptions.Item label="目标协议">
                  <Space>{current.targetProtocol}<ProtocolMetadataLink protocolType={current.targetProtocol} label="字段定义" /></Space>
                </Descriptions.Item>
                {current.createdAt ? <Descriptions.Item label="创建时间">{formatTimestamp(current.createdAt)}</Descriptions.Item> : null}
                {current.updatedAt ? <Descriptions.Item label="更新时间">{formatTimestamp(current.updatedAt)}</Descriptions.Item> : null}
                <Descriptions.Item label="能力" span={2}><CapabilityTags capability={current.capability} /></Descriptions.Item>
              </Descriptions>
            </Space>
          </Card>

          <MappingOverview requestMapping={current.requestMapping} responseMapping={current.responseMapping} />

          <Alert
            type="info"
            showIcon
            message="优先展示后端结构化参数映射；缺失结构化元数据时由字段路径、规则说明和损耗标记推断。当前为只读说明，最终转换逻辑以后端转换实现为准。"
          />

          <ProtocolMappingPanel conversion={current} activeTab={activeTab} onTabChange={setActiveTab} />
        </Space>
      ) : null}
    </Drawer>
  );
}

function StatusTag({ status }: { status: string }) {
  return <Tag color={status === 'ENABLED' ? 'success' : 'default'}>{status === 'ENABLED' ? `${status} / 已启用` : `${status} / 已禁用`}</Tag>;
}

function ImplementationStatusTag({ status }: { status: string }) {
  const color = status === 'IMPLEMENTED' ? 'success' : status === 'PARTIAL' ? 'warning' : 'error';
  const label = status === 'IMPLEMENTED' ? '已实现' : status === 'PARTIAL' ? '部分实现' : '未实现';
  return <Tag color={color}>{status} / {label}</Tag>;
}

function MappingOverview({ requestMapping, responseMapping }: { requestMapping: ProtocolConversionMappingResponse; responseMapping: ProtocolConversionMappingResponse }) {
  const requestView = useMemo(() => buildMappingView(requestMapping), [requestMapping]);
  const responseView = useMemo(() => buildMappingView(responseMapping), [responseMapping]);

  return (
    <Card size="small" title="映射概览">
      <Space wrap size={16}>
        <Statistic title="Request 字段" value={requestView.totalCount} suffix={`/${requestView.groups.length} 组`} />
        <Statistic title="Request 有损" value={requestView.lossyCount} valueStyle={{ color: requestView.lossyCount > 0 ? 'var(--warning-color)' : 'var(--success-color)' }} />
        <Statistic title="Response 字段" value={responseView.totalCount} suffix={`/${responseView.groups.length} 组`} />
        <Statistic title="Response 有损" value={responseView.lossyCount} valueStyle={{ color: responseView.lossyCount > 0 ? 'var(--warning-color)' : 'var(--success-color)' }} />
      </Space>
    </Card>
  );
}

function formatTimestamp(value: number): string {
  const timestamp = value < 10_000_000_000 ? value * 1000 : value;
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(timestamp);
}
