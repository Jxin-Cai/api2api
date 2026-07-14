import { Alert, App, Button, Checkbox, Form, Input, InputNumber, Modal, Result, Select, Space } from 'antd';
import { useEffect, useState } from 'react';

import { ApiKeySecretBlock, type CreateApiCredentialRequest, type CreateApiCredentialResponse } from '@entities/api-credential';

import { useApiCredentialMutations } from '../model/useApiCredentialMutations';

interface ApiCredentialCreateModalProps {
  /** Modal 打开状态 */
  open: boolean;
  /** 关闭弹窗 */
  onClose: () => void;
  /** 创建成功回调 */
  onCreated: (credential: CreateApiCredentialResponse) => void;
  /** 模型白名单可选项 */
  modelOptions?: Array<{ label: string; value: string }>;
}

const INITIAL_FORM: CreateApiCredentialRequest = { name: '', modelWhitelist: [], tokenLimit: 0 };

export function ApiCredentialCreateModal({ open, onClose, onCreated, modelOptions = [] }: ApiCredentialCreateModalProps) {
  const { modal } = App.useApp();
  const { createMutation } = useApiCredentialMutations();
  const [form, setForm] = useState<CreateApiCredentialRequest>(INITIAL_FORM);
  const [createdResult, setCreatedResult] = useState<CreateApiCredentialResponse | null>(null);
  const [saveConfirmed, setSaveConfirmed] = useState(false);

  useEffect((): void => {
    if (open) {
      setForm(INITIAL_FORM);
      setCreatedResult(null);
      setSaveConfirmed(false);
    }
  }, [open]);

  function handleClose(): void {
    if (createdResult?.plainApiKey && !saveConfirmed) {
      modal.confirm({
        title: '确认已保存明文 API Key？',
        content: '关闭后页面将不再保留明文 key，请确认已复制并安全保存。后续仍可在列表中通过“复制 Key”重新获取。',
        okText: '已保存，关闭',
        cancelText: '继续查看',
        onOk: onClose,
      });
      return;
    }
    onClose();
  }

  async function handleSubmit(): Promise<void> {
    const normalizedName = form.name.trim();
    if (!normalizedName) {
      return;
    }
    const result = await createMutation.mutateAsync({ ...form, name: normalizedName, tokenLimit: Math.max(0, form.tokenLimit) });
    setCreatedResult(result);
    onCreated(result);
  }

  const canSubmit = form.name.trim().length > 0 && form.tokenLimit >= 0;

  return (
    <Modal
      title="创建 API Key"
      open={open}
      onCancel={handleClose}
      width={560}
      footer={createdResult ? <Button type="primary" onClick={handleClose} disabled={Boolean(createdResult.plainApiKey) && !saveConfirmed}>关闭</Button> : null}
      destroyOnHidden
    >
      {createdResult ? (
        <Space direction="vertical" style={{ width: '100%' }} size={16}>
          <Result status="success" title="API Key 创建成功" subTitle="请立即复制并保存明文 key。" />
          <ApiKeySecretBlock plainApiKey={createdResult.plainApiKey ?? ''} credentialName={createdResult.name} onCopied={(): void => setSaveConfirmed(true)} warningMessage="请立即复制并妥善保存。关闭后页面不保留明文，但后续可从列表再次复制。" />
          <Checkbox checked={saveConfirmed} onChange={(event): void => setSaveConfirmed(event.target.checked)}>
            我已安全保存明文 API Key
          </Checkbox>
        </Space>
      ) : (
        <Form layout="vertical" onFinish={handleSubmit}>
          <Form.Item label="名称" required validateStatus={!form.name.trim() ? 'error' : undefined} help={!form.name.trim() ? '请输入 API Key 名称' : undefined}>
            <Input value={form.name} onChange={(event): void => setForm((current) => ({ ...current, name: event.target.value }))} placeholder="例如：生产环境调用" disabled={createMutation.isPending} />
          </Form.Item>
          <Form.Item label="模型白名单">
            <Select
              mode="tags"
              value={form.modelWhitelist}
              onChange={(value: string[]): void => setForm((current) => ({ ...current, modelWhitelist: value }))}
              options={modelOptions}
              placeholder="选择或输入允许调用的模型"
              disabled={createMutation.isPending}
            />
            {modelOptions.length === 0 ? <Alert style={{ marginTop: 8 }} type="info" showIcon message="暂无模型选项，可手动输入模型名。" /> : null}
          </Form.Item>
          <Form.Item label="累计 Token 上限" required>
            <InputNumber min={0} precision={0} value={form.tokenLimit} onChange={(value): void => setForm((current) => ({ ...current, tokenLimit: value ?? 0 }))} style={{ width: '100%' }} disabled={createMutation.isPending} />
          </Form.Item>
          {createMutation.isError ? <Alert type="error" showIcon message="创建失败" description={createMutation.error.message} style={{ marginBottom: 16 }} /> : null}
          <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
            <Button onClick={handleClose} disabled={createMutation.isPending}>取消</Button>
            <Button type="primary" htmlType="submit" loading={createMutation.isPending} disabled={!canSubmit}>创建</Button>
          </Space>
        </Form>
      )}
    </Modal>
  );
}
