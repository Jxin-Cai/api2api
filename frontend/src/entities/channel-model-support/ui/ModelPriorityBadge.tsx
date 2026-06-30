import { Badge, Tooltip } from 'antd';

interface ModelPriorityBadgeProps {
  /** 优先级，数字越小越优先 */
  priority: number;
}

export function ModelPriorityBadge({ priority }: ModelPriorityBadgeProps) {
  const color = priority <= 1 ? '#f5222d' : priority <= 3 ? '#faad14' : '#1677ff';
  return (
    <Tooltip title="模型排序值，数字越小越优先；仅在同一优先组内生效">
      <Badge color={color} text={`排序 ${priority}`} />
    </Tooltip>
  );
}
