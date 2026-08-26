import { Tag } from 'antd';
import type { EvaluationStatus } from '../model/types';

interface ChannelEvaluationStatusTagProps {
  /** 测评状态 */
  status: EvaluationStatus;
}

const STATUS_COLOR: Record<string, string> = {
  PENDING: 'default',
  RUNNING: 'processing',
  SUCCEEDED: 'success',
  FAILED: 'error',
};

const STATUS_LABEL: Record<string, string> = {
  PENDING: '待提交',
  RUNNING: '测评中',
  SUCCEEDED: '已完成',
  FAILED: '失败',
};

export function ChannelEvaluationStatusTag({ status }: ChannelEvaluationStatusTagProps) {
  return <Tag color={STATUS_COLOR[status] ?? 'default'}>{STATUS_LABEL[status] ?? status}</Tag>;
}
