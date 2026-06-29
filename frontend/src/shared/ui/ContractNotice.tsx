import { Alert, Typography } from 'antd';

interface ContractNoticeProps {
  /** 提示标题 */
  title: string;
  /** 契约缺口说明 */
  description: string;
  /** 缺失的 API 列表 */
  missingApis?: string[];
}

export function ContractNotice({ title, description, missingApis = [] }: ContractNoticeProps) {
  return (
    <Alert
      type="info"
      showIcon
      message={title}
      description={
        <div>
          <Typography.Paragraph style={{ marginBottom: missingApis.length ? 8 : 0 }}>
            {description}
          </Typography.Paragraph>
          {missingApis.length > 0 ? (
            <Typography.Text type="secondary">缺失契约：{missingApis.join('、')}</Typography.Text>
          ) : null}
        </div>
      }
    />
  );
}
