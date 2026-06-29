import { Button, Space, Typography, message } from 'antd';
import { CopyOutlined } from '@ant-design/icons';

interface CopyableTextProps {
  /** 待复制文本 */
  text: string;
  /** 是否脱敏显示 */
  masked?: boolean;
}

export function CopyableText({ text, masked = false }: CopyableTextProps) {
  async function handleCopy(): Promise<void> {
    await navigator.clipboard.writeText(text);
    message.success('已复制');
  }

  return (
    <Space size={4}>
      <Typography.Text code>{masked ? '••••••••' : text}</Typography.Text>
      <Button type="text" size="small" icon={<CopyOutlined />} onClick={handleCopy} />
    </Space>
  );
}
