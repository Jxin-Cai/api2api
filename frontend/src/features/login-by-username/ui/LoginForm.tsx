import { LoginOutlined, UserOutlined } from '@ant-design/icons';
import { App, Button, Form, Input, Typography } from 'antd';
import { useNavigate, useSearchParams } from 'react-router-dom';

import { GradientText } from '@shared/ui';
import { ROUTE_PATHS } from '@shared/config/constants';
import { useLoginByUsername } from '../model/useLoginByUsername';
import type { LoginFormProps, LoginFormValues } from '../model/types';
import './LoginForm.css';

function getLandingPath(role?: string, redirect?: string | null): string {
  if (redirect && redirect.startsWith('/')) {
    return redirect;
  }
  return role === 'ADMIN' ? ROUTE_PATHS.adminDashboard : ROUTE_PATHS.appDashboard;
}

export function LoginForm({ onSuccess }: LoginFormProps) {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { notification } = App.useApp();
  const mutation = useLoginByUsername();

  async function handleFinish(values: LoginFormValues): Promise<void> {
    try {
      const response = await mutation.mutateAsync({ username: values.username.trim() });
      onSuccess?.(response);
      navigate(getLandingPath(response.role, searchParams.get('redirect')), { replace: true });
    } catch (error) {
      notification.error({
        message: '登录失败',
        description: error instanceof Error ? error.message : '请检查用户名后重试',
      });
    }
  }

  return (
    <Form<LoginFormValues> className="login-form" layout="vertical" onFinish={handleFinish} requiredMark={false}>
      <div className="login-form__head">
        <Typography.Title level={3} className="login-form__title">
          登录 <GradientText>意门</GradientText> 控制台
        </Typography.Title>
        <Typography.Paragraph type="secondary" className="login-form__desc">
          输入用户名进入意门管理控制台。当前 MVP 环境无需密码。
        </Typography.Paragraph>
      </div>
      <Form.Item
        name="username"
        label="用户名"
        rules={[
          { required: true, message: '请输入用户名' },
          { min: 3, max: 64, message: '用户名长度需为 3-64 位' },
          { pattern: /^[a-zA-Z0-9_.-]+$/, message: '仅支持字母、数字、下划线、点和横线' },
        ]}
      >
        <Input prefix={<UserOutlined />} placeholder="admin / user" autoComplete="username" size="large" />
      </Form.Item>
      <Form.Item className="login-form__submit">
        <Button type="primary" htmlType="submit" block size="large" loading={mutation.isPending} icon={<LoginOutlined />}>
          登录控制台
        </Button>
      </Form.Item>
    </Form>
  );
}
