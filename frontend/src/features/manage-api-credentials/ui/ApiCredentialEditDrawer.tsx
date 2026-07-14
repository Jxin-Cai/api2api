import { Alert, App, Button, Descriptions, Drawer, Input, InputNumber, Select, Space, Tabs } from 'antd';
import { useEffect, useState } from 'react';

import { ApiCredentialStatusTag, type ApiCredentialResponse } from '@entities/api-credential';

import type { ApiCredentialEditSection } from '../model/types';
import { useApiCredentialMutations } from '../model/useApiCredentialMutations';

interface ApiCredentialEditDrawerProps {
  /** Drawer 打开状态 */
  open: boolean;
  /** 当前编辑的 API Key */
  credential: ApiCredentialResponse | null;
  /** 关闭 Drawer */
  onClose: () => void;
  /** 模型白名单选项 */
  modelOptions?: Array<{ label: string; value: string }>;
  /** 更新成功回调 */
  onUpdated: (credential: ApiCredentialResponse) => void;
}

export function ApiCredentialEditDrawer({ open, credential, onClose, modelOptions = [], onUpdated }: ApiCredentialEditDrawerProps) {
  const { message } = App.useApp();
  const { renameMutation, whitelistMutation, limitMutation } = useApiCredentialMutations();
  const [activeSection, setActiveSection] = useState<ApiCredentialEditSection>('name');
  const [nameDraft, setNameDraft] = useState('');
  const [modelWhitelistDraft, setModelWhitelistDraft] = useState<string[]>([]);
  const [tokenLimitDraft, setTokenLimitDraft] = useState(0);

  useEffect((): void => {
    if (credential) {
      setNameDraft(credential.name);
      setModelWhitelistDraft(credential.modelWhitelist);
      setTokenLimitDraft(credential.tokenLimit);
      setActiveSection('name');
    }
  }, [credential]);

  const submitting = renameMutation.isPending || whitelistMutation.isPending || limitMutation.isPending;

  async function handleSaveName(): Promise<void> {
    if (!credential || !nameDraft.trim()) {
      return;
    }
    const updated = await renameMutation.mutateAsync({ id: credential.id, params: { name: nameDraft.trim() } });
    message.success('名称已更新');
    onUpdated(updated);
  }

  async function handleSaveModels(): Promise<void> {
    if (!credential) {
      return;
    }
    const updated = await whitelistMutation.mutateAsync({ id: credential.id, params: { modelWhitelist: modelWhitelistDraft } });
    message.success('模型白名单已更新');
    onUpdated(updated);
  }

  async function handleSaveLimit(): Promise<void> {
    if (!credential || tokenLimitDraft < 0) {
      return;
    }
    const updated = await limitMutation.mutateAsync({ id: credential.id, params: { tokenLimit: tokenLimitDraft } });
    message.success('Token 上限已更新');
    onUpdated(updated);
  }

  return (
    <Drawer title="编辑 API Key" open={open} onClose={onClose} width={520} destroyOnHidden>
      {!credential ? (
        <Alert type="info" showIcon message="请选择要编辑的 API Key" />
      ) : (
        <Space direction="vertical" style={{ width: '100%' }} size={16}>
          <Descriptions size="small" column={1} bordered>
            <Descriptions.Item label="名称">{credential.name}</Descriptions.Item>
            <Descriptions.Item label="ID">{credential.id}</Descriptions.Item>
            <Descriptions.Item label="状态"><ApiCredentialStatusTag status={credential.status} /></Descriptions.Item>
          </Descriptions>
          <Alert type="info" showIcon message="名称、白名单和 Token 上限会分别保存。若后端 DTO 字段调整，请以后端契约为准。" />
          <Tabs
            activeKey={activeSection}
            onChange={(key): void => setActiveSection(key as ApiCredentialEditSection)}
            items={[
              {
                key: 'name',
                label: '名称',
                children: (
                  <Space direction="vertical" style={{ width: '100%' }}>
                    <Input value={nameDraft} onChange={(event): void => setNameDraft(event.target.value)} disabled={submitting} />
                    <Button type="primary" onClick={handleSaveName} loading={renameMutation.isPending} disabled={!nameDraft.trim()}>保存名称</Button>
                  </Space>
                ),
              },
              {
                key: 'models',
                label: '模型白名单',
                children: (
                  <Space direction="vertical" style={{ width: '100%' }}>
                    <Select mode="tags" value={modelWhitelistDraft} onChange={setModelWhitelistDraft} options={modelOptions} disabled={submitting} placeholder="选择或输入模型" />
                    <Button type="primary" onClick={handleSaveModels} loading={whitelistMutation.isPending}>保存白名单</Button>
                  </Space>
                ),
              },
              {
                key: 'limit',
                label: 'Token 上限',
                children: (
                  <Space direction="vertical" style={{ width: '100%' }}>
                    <InputNumber min={0} precision={0} value={tokenLimitDraft} onChange={(value): void => setTokenLimitDraft(value ?? 0)} style={{ width: '100%' }} disabled={submitting} />
                    <Button type="primary" onClick={handleSaveLimit} loading={limitMutation.isPending} disabled={tokenLimitDraft < 0}>保存上限</Button>
                  </Space>
                ),
              },
            ]}
          />
        </Space>
      )}
    </Drawer>
  );
}
