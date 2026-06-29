import { Tag, Tooltip } from 'antd';

interface ModelSourceTagProps {
  /** 模型来源 */
  source: string;
}

export function ModelSourceTag({ source }: ModelSourceTagProps) {
  const fetched = source === 'FETCHED';
  return (
    <Tooltip title={fetched ? '由上游模型列表拉取' : '管理员手动维护'}>
      <Tag color={fetched ? 'blue' : 'gold'}>{fetched ? '拉取' : source}</Tag>
    </Tooltip>
  );
}
