import { Button, Space, Typography, message, notification } from 'antd';
import { CopyOutlined } from '@ant-design/icons';
import { copyText } from '@shared/lib';

interface CopyableTextProps {
  /** 待复制文本 */
  text: string;
  /** 是否脱敏显示 */
  masked?: boolean;
}

export function CopyableText({ text, masked = false }: CopyableTextProps) {
  async function handleCopy(): Promise<void> {
    const result = await copyText(text);
    if (!result.ok) {
      notification.error({ message: '复制失败', description: result.reason ?? '请手动选择文本复制。' });
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
