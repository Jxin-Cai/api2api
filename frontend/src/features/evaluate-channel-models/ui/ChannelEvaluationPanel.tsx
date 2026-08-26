import { useMemo, useState } from 'react';
import { Alert, App, Button, Form, Input, Select, Space, Switch, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  ChannelEvaluationStatusTag,
  useChannelEvaluationHistory,
  useChannelEvaluationSchedule,
  type ChannelEvaluationResponse,
} from '@entities/channel-evaluation';
import type { ProviderChannelResponse } from '@entities/provider-channel';
import { getApiErrorMessage } from '@shared/api';
import { useChannelEvaluationMutations } from '../model/useChannelEvaluationMutations';

interface ChannelEvaluationPanelProps {
  /** 当前渠道 */
  channel: ProviderChannelResponse;
}

const CRON_PRESETS = [
  { label: '每小时', value: '0 0 * * * *' },
  { label: '每天 02:00', value: '0 0 2 * * *' },
  { label: '每周一 03:00', value: '0 0 3 * * 1' },
];

function formatTime(value?: number | null): string {
  if (!value) {
    return '—';
  }
  return new Date(value).toLocaleString('zh-CN', { hour12: false });
}

function formatScore(score?: number | null): string {
  return score == null ? '—' : Number(score).toFixed(2);
}

export function ChannelEvaluationPanel({ channel }: ChannelEvaluationPanelProps) {
  const { message } = App.useApp();
  const enabledModels = useMemo(
    () => channel.supportedModels.filter((model) => model.status === 'ENABLED'),
    [channel.supportedModels]
  );
  const uniqueEnabledModels = useMemo(() => {
    const seen = new Set<string>();
    return enabledModels.filter((model) => {
      if (seen.has(model.requestedModel)) {
        return false;
      }
      seen.add(model.requestedModel);
      return true;
    });
  }, [enabledModels]);
  const [selectedModels, setSelectedModels] = useState<string[]>([]);
  const { evaluations, summary, isLoading, isFetching } = useChannelEvaluationHistory(
    channel.id,
    { limit: 20, sortField: 'REQUESTED_AT', descending: true },
    { refetchInterval: 15_000 }
  );
  const { schedule } = useChannelEvaluationSchedule(channel.id);
  const { submitMutation, upsertScheduleMutation } = useChannelEvaluationMutations(channel.id);
  const [form] = Form.useForm<{ cronExpression: string; zoneId: string; models: string[]; enabled: boolean }>();

  const inFlight = evaluations.some((item) => item.status === 'PENDING' || item.status === 'RUNNING');

  async function handleSubmit(): Promise<void> {
    if (uniqueEnabledModels.length === 0) {
      message.warning('请先启用至少一个模型再发起测评');
      return;
    }
    try {
      const response = await submitMutation.mutateAsync({ models: selectedModels });
      message.success(`已提交 ${response.data.evaluations.length} 个测评任务`);
    } catch (error) {
      message.error(getApiErrorMessage(error, '提交测评失败'));
    }
  }

  async function handleSaveSchedule(): Promise<void> {
    const values = await form.validateFields();
    try {
      await upsertScheduleMutation.mutateAsync({
        cronExpression: values.cronExpression,
        zoneId: values.zoneId || 'UTC',
        models: values.models ?? [],
        enabled: values.enabled,
      });
      message.success('测评计划已保存');
    } catch (error) {
      message.error(getApiErrorMessage(error, '保存测评计划失败'));
    }
  }

  const columns: ColumnsType<ChannelEvaluationResponse> = [
    { title: '模型', dataIndex: 'requestedModel', width: 180 },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (status: ChannelEvaluationResponse['status']) => <ChannelEvaluationStatusTag status={status} />,
    },
    {
      title: '分数',
      dataIndex: 'score',
      width: 90,
      render: (score: number | null | undefined) => formatScore(score),
    },
    {
      title: '识别模型',
      dataIndex: 'detectedModel',
      render: (_value: string | null | undefined, record) => (
        <Space size={4} wrap>
          <Typography.Text>{record.detectedModel || '—'}</Typography.Text>
          {record.familyMismatch ? <Tag color="warning">家族不一致</Tag> : null}
        </Space>
      ),
    },
    {
      title: '探针',
      key: 'probes',
      width: 140,
      render: (_value, record) => `${record.passedProbeCount ?? 0} / ${record.warningProbeCount ?? 0} / ${record.failedProbeCount ?? 0}`,
    },
    {
      title: '触发',
      dataIndex: 'trigger',
      width: 90,
      render: (trigger: string) => (trigger === 'SCHEDULED' ? '计划' : '手动'),
    },
    {
      title: '提交时间',
      dataIndex: 'requestedAt',
      width: 170,
      render: (value: number | null | undefined) => formatTime(value),
    },
    {
      title: '报告',
      dataIndex: 'reportUrl',
      width: 80,
      render: (url: string | null | undefined, record) => (
        url ? <Typography.Link href={url} target="_blank" rel="noreferrer">查看</Typography.Link> : (record.errorMessage ? <Typography.Text type="danger">{record.errorMessage}</Typography.Text> : '—')
      ),
    },
  ];

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Typography.Text strong>渠道测评</Typography.Text>
      <Space wrap style={{ justifyContent: 'space-between', width: '100%' }}>
        <Typography.Text type="secondary">
          {summary
            ? `最近 ${summary.totalCount} 次测评，平均分 ${formatScore(summary.averageScore)}，失败 ${summary.failedCount} 次`
            : '尚未发起测评'}
        </Typography.Text>
        <Space wrap>
          <Select
            mode="multiple"
            allowClear
            placeholder="默认测评全部启用模型"
            value={selectedModels}
            options={uniqueEnabledModels.map((model) => ({ label: model.requestedModel, value: model.requestedModel }))}
            style={{ minWidth: 240 }}
            onChange={setSelectedModels}
          />
          <Button type="primary" loading={submitMutation.isPending} onClick={() => void handleSubmit()}>
            发起测评
          </Button>
        </Space>
      </Space>
      {inFlight ? <Alert type="info" showIcon message="存在进行中的测评，列表会自动刷新" /> : null}
      <Table
        rowKey="id"
        size="small"
        columns={columns}
        dataSource={evaluations}
        loading={isLoading || isFetching}
        pagination={false}
        scroll={{ x: true }}
        locale={{ emptyText: '暂无测评记录' }}
      />
      <Form
        form={form}
        layout="inline"
        initialValues={{
          cronExpression: schedule?.cronExpression ?? '0 0 2 * * *',
          zoneId: schedule?.zoneId ?? 'UTC',
          models: schedule?.models ?? [],
          enabled: schedule?.enabled ?? false,
        }}
        key={schedule?.id ?? 'new-schedule'}
      >
        <Form.Item name="cronExpression" label="Cron" rules={[{ required: true, message: '请填写 Cron 表达式' }]}>
          <Input placeholder="0 0 2 * * *" style={{ width: 180 }} />
        </Form.Item>
        <Form.Item>
          <Select
            options={CRON_PRESETS}
            placeholder="常用计划"
            style={{ width: 140 }}
            onChange={(value) => form.setFieldValue('cronExpression', value)}
          />
        </Form.Item>
        <Form.Item name="zoneId" label="时区">
          <Input style={{ width: 120 }} />
        </Form.Item>
        <Form.Item name="models" label="模型">
          <Select
            mode="multiple"
            allowClear
            placeholder="全部启用模型"
            options={uniqueEnabledModels.map((model) => ({ label: model.requestedModel, value: model.requestedModel }))}
            style={{ minWidth: 180 }}
          />
        </Form.Item>
        <Form.Item name="enabled" label="启用" valuePropName="checked">
          <Switch />
        </Form.Item>
        <Form.Item>
          <Button loading={upsertScheduleMutation.isPending} onClick={() => void handleSaveSchedule()}>
            保存计划
          </Button>
        </Form.Item>
        {schedule?.nextTriggerAt ? (
          <Form.Item>
            <Typography.Text type="secondary">下次触发：{formatTime(schedule.nextTriggerAt)}</Typography.Text>
          </Form.Item>
        ) : null}
      </Form>
    </Space>
  );
}
