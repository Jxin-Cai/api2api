import { Alert, Button, Form, InputNumber, Modal, Select, Space, Switch } from 'antd';
import { useEffect } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { QUOTA_QUERY_KEY, saveQuota, type GroupQuotaDraft, type QuotaRule, type QuotaTarget } from '@entities/token-quota';
import { useModelGroups } from '@entities/model-group';

export const PERIOD_LABELS = { DAY: '每天', WEEK: '每周', MONTH: '每月', LIFETIME: '累计' } as const;
interface Props { open: boolean; rule: QuotaRule | null; targets: QuotaTarget[]; onClose: () => void }
export function QuotaFormModal({ open, rule, targets, onClose }: Props) {
  const [form] = Form.useForm<GroupQuotaDraft>();
  const client = useQueryClient();
  const { groups } = useModelGroups();
  const targetId = Form.useWatch('targetId', form);
  const target = targets.find((item) => item.scope === 'MODEL_GROUP' && item.id === targetId);
  const group = groups.find((item) => item.id === target?.modelGroupId);
  const mutation = useMutation({
    mutationFn: (draft: GroupQuotaDraft) => saveQuota(rule?.id ?? null, { ...draft, scope: 'MODEL_GROUP' }),
    onSuccess: async () => {
      await client.invalidateQueries({ queryKey: QUOTA_QUERY_KEY });
      await client.invalidateQueries({ queryKey: ['apiCredentials'] });
      onClose();
    },
  });
  useEffect(() => {
    if (open) {
      form.resetFields();
      form.setFieldsValue(rule
        ? { targetId: rule.targetId, model: rule.model, period: rule.period, limitMillions: rule.limitMillions, enabled: rule.enabled }
        : { targetId: '', model: '', period: 'DAY', limitMillions: 1, enabled: true });
      mutation.reset();
    }
  }, [open, rule, form]);
  return (
    <Modal title={rule ? '编辑分组限流规则' : '新增分组限流规则'} open={open} onCancel={onClose} footer={null} width={560} destroyOnHidden>
      <Form form={form} layout="vertical" onFinish={(draft) => mutation.mutate(draft)} disabled={mutation.isPending}>
        <Form.Item name="targetId" label="模型分组" rules={[{ required: true, message: '请选择模型分组' }]}>
          <Select showSearch optionFilterProp="label" placeholder="选择模型分组" onChange={() => form.setFieldValue('model', '')}
            options={targets.filter((item) => item.scope === 'MODEL_GROUP').map((item) => ({ value: item.id, label: `${item.name} · ${item.id}` }))} />
        </Form.Item>
        <Form.Item name="model" label="统计模型" extra="默认汇总全部模型的用量；也可单独限制某个模型。">
          <Select showSearch optionFilterProp="label" options={[{ label: '全部模型合计', value: '' }, ...Array.from(new Set([...(group?.modelWhitelist ?? []), ...(rule?.model ? [rule.model] : [])])).map((model) => ({ label: model, value: model }))]} />
        </Form.Item>
        <Form.Item name="period" label="额度周期" rules={[{ required: true }]}>
          <Select options={Object.entries(PERIOD_LABELS).map(([value, label]) => ({ value, label }))} />
        </Form.Item>
        <Form.Item name="limitMillions" label="额度（百万 token）" rules={[{ required: true, message: '请输入额度' }, { type: 'number', min: 0.000001, max: 999999999.999999, message: '额度须大于 0，最多 6 位小数' }]} extra="1 = 1,000,000 加权 token；0.5 = 500,000。">
          <InputNumber min={0.000001} max={999999999.999999} precision={6} step={1} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="enabled" label="启用规则" valuePropName="checked"><Switch /></Form.Item>
        {mutation.isError ? <Alert type="error" showIcon message="保存失败" description={mutation.error.message} style={{ marginBottom: 16 }} /> : null}
        <Space style={{ display: 'flex', justifyContent: 'flex-end' }}>
          <Button onClick={onClose} disabled={mutation.isPending}>取消</Button>
          <Button type="primary" htmlType="submit" loading={mutation.isPending}>保存规则</Button>
        </Space>
      </Form>
    </Modal>
  );
}
