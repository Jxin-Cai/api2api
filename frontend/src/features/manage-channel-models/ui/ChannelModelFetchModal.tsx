import { useState } from 'react';
import { Alert, Button, InputNumber, Modal, Space, message } from 'antd';
import type { ProviderChannelResponse } from '@entities/provider-channel';
import { useChannelModelMutations } from '../model/useChannelModelMutations';

interface ChannelModelFetchModalProps {
  /** 打开状态 */
  open: boolean;
  /** 渠道 ID */
  channelId: number;
  /** 渠道名称 */
  channelName: string;
  /** 关闭回调 */
  onClose: () => void;
  /** 拉取成功回调 */
  onFetched: (channel: ProviderChannelResponse) => void;
}

export function ChannelModelFetchModal({ open, channelId, channelName, onClose, onFetched }: ChannelModelFetchModalProps) {
  const [defaultPriority, setDefaultPriority] = useState(10);
  const { fetchMutation } = useChannelModelMutations();

  async function handleFetch(): Promise<void> {
    if (defaultPriority < 1) {
      message.warning('默认优先级必须大于等于 1');
      return;
    }
    const response = await fetchMutation.mutateAsync({ channelId, body: { defaultPriority } });
    onFetched(response.data);
    onClose();
  }

  return (
    <Modal title={`拉取 ${channelName} 模型`} open={open} onCancel={onClose} footer={null} width={460}>
      <Space direction="vertical" style={{ width: '100%' }}>
        <Alert type="warning" showIcon message="拉取会替换当前模型列表" />
        <InputNumber min={1} value={defaultPriority} onChange={(value) => setDefaultPriority(value ?? 1)} style={{ width: '100%' }} />
        <Button type="primary" loading={fetchMutation.isPending} onClick={handleFetch}>开始拉取</Button>
      </Space>
    </Modal>
  );
}
