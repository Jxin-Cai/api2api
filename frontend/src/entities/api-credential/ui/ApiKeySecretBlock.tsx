import { Alert, App, Button, Space, Typography } from 'antd';
import { useEffect, useRef, useState } from 'react';

import { copyText } from '@shared/lib';
import './ApiKeySecretBlock.css';

interface ApiKeySecretBlockProps {
  /** 创建响应中的明文 API Key */
  plainApiKey: string;
  /** 凭证名称 */
  credentialName?: string;
  /** 复制成功回调 */
  onCopied?: () => void;
  /** 复制后是否自动遮罩 */
  maskAfterCopy?: boolean;
  /** 警告文案 */
  warningMessage?: string;
}

export function ApiKeySecretBlock({ plainApiKey, credentialName, onCopied, maskAfterCopy = false, warningMessage = '请立即保存 API Key，关闭后页面将不再保留明文。' }: ApiKeySecretBlockProps) {
  const { message, notification } = App.useApp();
  const [copied, setCopied] = useState(false);
  const [masked, setMasked] = useState(false);
  const secretRef = useRef<HTMLElement>(null);

  useEffect(() => {
    setCopied(false);
    setMasked(false);
  }, [plainApiKey]);

  async function handleCopy(): Promise<void> {
    const text = plainApiKey.trim();
    if (!text) {
      notification.error({ message: '复制失败', description: '当前没有可复制的 API Key 明文。' });
      return;
    }
    const result = await copyText(text, secretRef.current);
    if (!result.ok) {
      setMasked(false);
      notification.warning({
        message: '请手动复制',
        description: result.reason ?? '请按 ⌘C 或 Ctrl+C 复制已选中的明文。',
      });
      return;
    }
    setCopied(true);
    if (maskAfterCopy) {
      setMasked(true);
    }
    onCopied?.();
    message.success('API Key 已复制');
    window.setTimeout((): void => setCopied(false), 2000);
  }

  if (!plainApiKey) {
    return <Alert type="error" showIcon message="后端未返回明文 key，请重新创建" />;
  }

  return (
    <Space direction="vertical" style={{ width: '100%' }} size={12}>
      <Alert type="warning" showIcon message={warningMessage} />
      {credentialName ? <Typography.Text strong>{credentialName}</Typography.Text> : null}
      <code ref={secretRef} className="api-key-secret-block__code">
        {masked ? '••••••••••••••••••••••••••••••••' : plainApiKey}
      </code>
      <Space>
        <Button type="primary" onClick={handleCopy}>{copied ? '已复制' : '复制'}</Button>
        <Button onClick={(): void => setMasked(!masked)}>{masked ? '显示' : '隐藏'}</Button>
      </Space>
    </Space>
  );
}
