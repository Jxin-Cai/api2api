import { Children, type ReactNode } from 'react';
import { Col, Row, type ColProps } from 'antd';

interface DashboardSummaryGridProps {
  children: ReactNode;
  /** AntD Col 响应式配置 */
  colProps?: ColProps;
}

export function DashboardSummaryGrid({ children, colProps = { xs: 24, sm: 12, lg: 6 } }: DashboardSummaryGridProps) {
  return (
    <Row gutter={[16, 16]}>
      {Children.map(children, (child, index) => (
        <Col {...colProps} key={index}>{child}</Col>
      ))}
    </Row>
  );
}
