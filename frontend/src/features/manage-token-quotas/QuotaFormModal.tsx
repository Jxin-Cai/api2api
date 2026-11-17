import { Alert, Button, Form, InputNumber, Modal, Select, Space, Switch } from 'antd';
import { useEffect } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { QUOTA_QUERY_KEY, saveQuota, type QuotaDraft, type QuotaRule, type QuotaScope, type QuotaTarget } from '@entities/token-quota';
import { useModelGroups } from '@entities/model-group';

export const PERIOD_LABELS = { DAY: '每天', WEEK: '每周', MONTH: '每月', LIFETIME: '累计' } as const;
interface Props { open: boolean; rule: QuotaRule | null; initialScope: QuotaScope; targets: QuotaTarget[]; fixedKey?: QuotaTarget; onClose: () => void }
export function QuotaFormModal({ open, rule, initialScope, targets, fixedKey, onClose }: Props) {
  const [form] = Form.useForm<QuotaDraft>();
  const client = useQueryClient();
  const { groups } = useModelGroups();
  const scope = Form.useWatch('scope', form) ?? initialScope;
  const targetId = Form.useWatch('targetId', form);
  const target = targets.find((item) => item.scope === scope && item.id === targetId);
  const group = groups.find((item) => item.id === target?.modelGroupId);
  const mutation = useMutation({
    mutationFn: (draft: QuotaDraft) => saveQuota(rule?.id ?? null, draft),
    onSuccess: async () => {
      await client.invalidateQueries({ queryKey: QUOTA_QUERY_KEY });
      await client.invalidateQueries({ queryKey: ['apiCredentials'] });
      onClose();
    },
  });
  useEffect(() => {
    if (open) {
      form.resetFields();
      form.setFieldsValue(rule ?? { scope: initialScope, targetId: fixedKey?.id ?? '', model: '', period: 'DAY', limitMillions: 1, enabled: true });
      mutation.reset();
    }
  }, [open, rule, initialScope, fixedKey?.id, form]);
  return (
    <Modal title={fixedKey ? (rule ? '编辑模型每日上限' : '新增模型每日上限') : (rule ? '编辑限流规则' : '新增限流规则')} open={open} onCancel={onClose} footer={null} width={560} destroyOnHidden>
      <Form form={form} layout="vertical" onFinish={(draft) => mutation.mutate(draft)}>
        <Form.Item hidden={Boolean(fixedKey)} name="scope" label="限流对象" rules={[{ required: true }]}>
          <Select options={[{ label: '模型分组', value: 'MODEL_GROUP' }, { label: '指定 API Key', value: 'API_KEY' }]} onChange={() => form.setFieldsValue({ targetId: '', model: '' })} />
        </Form.Item>
        <Form.Item name="targetId" label={scope === 'MODEL_GROUP' ? '模型分组' : 'API Key'} rules={[{ required: true, message: '请选择限流对象' }]}>
          <Select disabled={Boolean(fixedKey)} showSearch optionFilterProp="label" placeholder="选择限流对象" onChange={() => form.setFieldValue('model', '')}
            options={targets.filter((item) => item.scope === scope).map((item) => ({ value: item.id, label: `${item.name}${item.ownerName ? ` · ${item.ownerName}` : ''} · ${item.id}` }))} />
        </Form.Item>
        <Form.Item name="model" label="统计模型" rules={fixedKey && rule?.model !== '' ? [{ required: true, message: '请选择要限制的模型' }] : []} extra={fixedKey ? '各模型分别计量、每日重置；此上限只作用于当前 Key。' : '默认汇总全部模型的用量；也可单独限制某个模型。'}>
          <Select showSearch optionFilterProp="label" options={[...(!fixedKey || rule?.model === '' ? [{ label: '全部模型合计', value: '' }] : []), ...Array.from(new Set([...(group?.modelWhitelist ?? []), ...(rule?.model ? [rule.model] : [])])).map((model) => ({ label: model, value: model }))]} />
        </Form.Item>
        <Form.Item hidden={Boolean(fixedKey)} name="period" label="额度周期" rules={[{ required: true }]}>
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
