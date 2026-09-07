import { App, Button, Space, Typography } from 'antd';
import { CopyOutlined } from '@ant-design/icons';
import { copyText } from '@shared/lib';

interface CopyableTextProps {
  /** 待复制文本 */
  text: string;
  /** 是否脱敏显示 */
  masked?: boolean;
}

export function CopyableText({ text, masked = false }: CopyableTextProps) {
  const { message, notification } = App.useApp();

  async function handleCopy(): Promise<void> {
    const result = await copyText(text);
    if (!result.ok) {
      notification.warning({ message: '请手动复制', description: result.reason ?? '请按 ⌘C 或 Ctrl+C 复制。' });
      return;
    }
    message.success('已复制');
  }

  return (
    <Space size={4}>
      <Typography.Text code>{masked ? '••••••••' : text}</Typography.Text>
      <Button type="text" size="small" icon={<CopyOutlined />} onClick={handleCopy} />
    </Space>
  );
}
