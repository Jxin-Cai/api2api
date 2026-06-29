import { App, Button, Card, Descriptions, Form, Input, Space, Typography } from 'antd';
import { useEffect } from 'react';

import { useCurrentUser } from '@entities/user-account';
import { PageState } from '@shared/ui';
import { useUpdateCurrentUserProfile } from '../model/useUpdateCurrentUserProfile';

interface PersonalSettingsFormValues {
  displayName: string;
}

export function PersonalSettingsPanel() {
  const { user, isLoading, isError, refetch } = useCurrentUser();
  const [form] = Form.useForm<PersonalSettingsFormValues>();
  const updateProfileMutation = useUpdateCurrentUserProfile();
  const { message } = App.useApp();

  useEffect(() => {
    if (user) {
      form.setFieldsValue({ displayName: user.displayName });
    }
  }, [form, user]);

  if (isLoading) {
    return <PageState status="loading" title="加载个人信息" />;
  }

  if (isError || !user) {
    return <PageState status="error" title="个人信息加载失败" onRetry={(): void => { refetch().catch(() => undefined); }} />;
  }

  async function handleFinish(values: PersonalSettingsFormValues): Promise<void> {
    try {
      await updateProfileMutation.mutateAsync({ displayName: values.displayName.trim() });
      message.success('个人资料已更新');
    } catch (error) {
      message.error(error instanceof Error ? error.message : '个人资料更新失败');
    }
  }

  return (
    <Card>
      <Space direction="vertical" size={20} style={{ width: '100%' }}>
        <div>
          <Typography.Title level={4}>个人设置</Typography.Title>
          <Typography.Paragraph type="secondary">
            当前可更新显示名称，用户名暂不支持修改。
          </Typography.Paragraph>
        </div>
        <Descriptions size="small" column={1}>
          <Descriptions.Item label="用户 ID">{String(user.id)}</Descriptions.Item>
          <Descriptions.Item label="用户名">{user.username}</Descriptions.Item>
          <Descriptions.Item label="角色">{user.role}</Descriptions.Item>
          <Descriptions.Item label="状态">{user.status}</Descriptions.Item>
        </Descriptions>
        <Form<PersonalSettingsFormValues>
          form={form}
          layout="vertical"
          onFinish={handleFinish}
          requiredMark={false}
          style={{ maxWidth: 420 }}
        >
          <Form.Item
            name="displayName"
            label="显示名称"
            rules={[
              { required: true, message: '请输入显示名称' },
              { max: 100, message: '显示名称不能超过 100 个字符' },
            ]}
          >
            <Input placeholder="请输入显示名称" autoComplete="name" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={updateProfileMutation.isPending}>
              保存
            </Button>
          </Form.Item>
        </Form>
      </Space>
    </Card>
  );
}
