import { useEffect } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Alert, App, Button, Form, InputNumber, Modal, Select, Space, Switch } from 'antd';
import { QUOTA_QUERY_KEY, saveCredentialQuota, type QuotaRule, type UserDailyQuotaDraft } from '@entities/token-quota';

interface Props {
  open: boolean;
  keyId: string;
  models: string[];
  rule: QuotaRule | null;
  onClose: () => void;
}

export function CredentialQuotaFormModal({ open, keyId, models, rule, onClose }: Props) {
  const [form] = Form.useForm<UserDailyQuotaDraft>();
  const client = useQueryClient();
  const { message } = App.useApp();
  const mutation = useMutation({
    mutationFn: (draft: UserDailyQuotaDraft) => saveCredentialQuota(keyId, rule?.id ?? null, draft),
    onSuccess: async () => {
      await client.invalidateQueries({ queryKey: QUOTA_QUERY_KEY });
      await client.invalidateQueries({ queryKey: ['apiCredentials'] });
      message.success('自设每日上限已保存');
      onClose();
    },
  });
  useEffect(() => {
    if (open) {
      form.resetFields();
      form.setFieldsValue(rule
        ? { model: rule.model, limitMillions: rule.limitMillions, enabled: rule.enabled }
        : { model: undefined, limitMillions: 1, enabled: true });
      mutation.reset();
    }
  }, [open, keyId, rule, form]);
  return <Modal title={rule ? '编辑自设每日上限' : '新增自设每日上限'} open={open} onCancel={onClose} footer={null} width={560} destroyOnHidden>
    <Alert type="info" showIcon message="自设每日上限与分组共享额度同时生效" description="此上限只作用于当前 Key；提高或停用上限不会改变模型分组的共享额度。" style={{ marginBottom: 16 }} />
    <Form form={form} layout="vertical" onFinish={draft => mutation.mutate(draft)} disabled={mutation.isPending}>
      <Form.Item name="model" label="模型" rules={[{ required: true, message: '请选择模型' }]}>
        <Select showSearch optionFilterProp="label" placeholder="选择要限制的模型" options={Array.from(new Set([...models, ...(rule ? [rule.model] : [])])).map(model => ({ label: model, value: model }))} />
      </Form.Item>
      <Form.Item name="limitMillions" label="每日上限（百万加权 Token）" extra="1 = 1,000,000 Token；0.5 = 500,000。" rules={[{ required: true, message: '请输入每日上限' }, { type: 'number', min: 0.000001, max: 999999999.999999, message: '上限须大于 0，最多 6 位小数' }]}>
        <InputNumber min={0.000001} max={999999999.999999} precision={6} style={{ width: '100%' }} />
      </Form.Item>
      <Form.Item name="enabled" label="启用自设上限" valuePropName="checked"><Switch /></Form.Item>
      {mutation.isError ? <Alert type="error" showIcon message="保存失败" description={mutation.error.message} style={{ marginBottom: 16 }} /> : null}
      <Space style={{ display: 'flex', justifyContent: 'flex-end' }}>
        <Button onClick={onClose}>取消</Button><Button type="primary" htmlType="submit" loading={mutation.isPending}>保存上限</Button>
      </Space>
    </Form>
  </Modal>;
}
