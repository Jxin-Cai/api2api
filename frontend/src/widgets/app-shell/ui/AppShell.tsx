import {
  ApiOutlined,
  BarChartOutlined,
  DashboardOutlined,
  KeyOutlined,
  DownOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  MoonOutlined,
  SwapOutlined,
  SunOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import { Button, Drawer, Dropdown, Grid, Layout, Menu, Space, Tooltip, Typography } from 'antd';
import { useEffect, useMemo, type ReactNode } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';

import { useCurrentUser, useLogout } from '@entities/user-account';
import { onAuthExpired } from '@shared/api';
import { PortalMenuSwitcher } from '@features/switch-portal-navigation';
import { ROUTE_PATHS } from '@shared/config/constants';
import { useThemeStore } from '@shared/config/stores/useThemeStore';
import { useUiStore } from '@shared/config/stores/useUiStore';
import { GradientText } from '@shared/ui';
import type { PortalType } from '@shared/types/portal';
import { getActiveMenuKey, getMenuItems } from '../model/menu';
import './AppShell.css';

const { Header, Sider, Content } = Layout;

interface AppShellProps {
  portal: PortalType;
}

const MENU_ICONS: Record<string, ReactNode> = {
  'app-dashboard': <DashboardOutlined />,
  'app-api-keys': <KeyOutlined />,
  'app-usage': <BarChartOutlined />,
  'admin-dashboard': <DashboardOutlined />,
  'admin-users': <TeamOutlined />,
  'admin-channels': <ApiOutlined />,
  'admin-conversions': <SwapOutlined />,
};

export function AppShell({ portal }: AppShellProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const screens = Grid.useBreakpoint();
  const { user } = useCurrentUser();
  const logoutMutation = useLogout();
  const { sidebarCollapsed, mobileDrawerOpen, setSidebarCollapsed, setMobileDrawerOpen } = useUiStore();
  const mode = useThemeStore((state) => state.mode);
  const toggleMode = useThemeStore((state) => state.toggleMode);
  const menuItems = useMemo(() => getMenuItems(portal), [portal]);

  useEffect(() => onAuthExpired(() => {
    navigate(ROUTE_PATHS.login, { replace: true });
  }), [navigate]);

  async function handleLogout(): Promise<void> {
    await logoutMutation.mutateAsync().catch(() => undefined);
    navigate(ROUTE_PATHS.login, { replace: true });
  }

  const accountMenuItems = [
    { key: 'settings', label: '个人设置' },
    { key: 'logout', label: '退出登录', danger: true, icon: <LogoutOutlined /> },
  ];

  const menu = (
    <Menu
      mode="inline"
      selectedKeys={[getActiveMenuKey(location.pathname)]}
      items={menuItems.map((item) => ({ key: item.key, icon: MENU_ICONS[item.key], label: item.label }))}
      onClick={({ key }): void => {
        const target = menuItems.find((item) => item.key === key);
        if (target) {
          navigate(target.path);
          setMobileDrawerOpen(false);
        }
      }}
    />
  );
  const isMobile = !screens.md;

  return (
    <Layout className="app-shell">
      {isMobile ? (
        <Drawer
          placement="left"
          open={mobileDrawerOpen}
          onClose={(): void => setMobileDrawerOpen(false)}
          styles={{ body: { padding: 0, background: 'var(--bg-surface)' } }}
          width={276}
        >
          <div className="app-shell__brand app-shell__brand--drawer">
            <GradientText>意门</GradientText>
          </div>
          {menu}
        </Drawer>
      ) : (
        <Sider
          collapsible
          collapsed={sidebarCollapsed}
          onCollapse={setSidebarCollapsed}
          className="app-shell__sider"
          width={240}
          collapsedWidth={80}
          trigger={null}
        >
          <div className="app-shell__brand">
            <span className="app-shell__logo">门</span>
            {!sidebarCollapsed ? <GradientText>意门</GradientText> : null}
          </div>
          <div className="app-shell__menu">{menu}</div>
        </Sider>
      )}
      <Layout className="app-shell__main">
        <Header className="app-shell__header">
          <Space size={12} className="app-shell__header-left">
            {isMobile ? (
              <Button icon={<MenuUnfoldOutlined />} onClick={(): void => setMobileDrawerOpen(true)} />
            ) : (
              <Button
                icon={sidebarCollapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
                onClick={(): void => setSidebarCollapsed(!sidebarCollapsed)}
              />
            )}
            <div>
              <Typography.Text className="app-shell__portal" strong>{portal === 'admin' ? '管理后台' : '用户门户'}</Typography.Text>
              <div className="app-shell__crumb">连接模型、渠道与协议</div>
            </div>
            <PortalMenuSwitcher currentPortal={portal} />
          </Space>
          <Space size={12} className="app-shell__header-actions">
            <Tooltip title={mode === 'dark' ? '切换浅色模式' : '切换深色模式'}>
              <Button
                aria-label="切换主题"
                icon={mode === 'dark' ? <SunOutlined /> : <MoonOutlined />}
                onClick={toggleMode}
              />
            </Tooltip>
            <Dropdown
              menu={{
                items: accountMenuItems,
                onClick: ({ key }): void => {
                  if (key === 'settings') {
                    navigate(ROUTE_PATHS.appSettings);
                    return;
                  }
                  if (key === 'logout') {
                    void handleLogout();
                  }
                },
              }}
              trigger={['click']}
            >
              <Button loading={logoutMutation.isPending}>
                <Space size={6}>
                  <span>{user?.username ?? '账号'}</span>
                  <DownOutlined />
                </Space>
              </Button>
            </Dropdown>
          </Space>
        </Header>
        <Content className="app-shell__content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
