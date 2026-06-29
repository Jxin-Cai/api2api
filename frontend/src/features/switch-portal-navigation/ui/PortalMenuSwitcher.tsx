import { Button, Segmented } from 'antd';
import { useNavigate } from 'react-router-dom';

import { useCurrentUser } from '@entities/user-account';
import { ROUTE_PATHS } from '@shared/config/constants';
import type { PortalType } from '@shared/types/portal';

interface PortalMenuSwitcherProps {
  currentPortal: PortalType;
}

export function PortalMenuSwitcher({ currentPortal }: PortalMenuSwitcherProps) {
  const navigate = useNavigate();
  const { user } = useCurrentUser();

  if (user?.role !== 'ADMIN') {
    return <Button type="text" disabled>用户门户</Button>;
  }

  return (
    <Segmented
      size="small"
      value={currentPortal}
      options={[{ label: '前台', value: 'app' }, { label: '后台', value: 'admin' }]}
      onChange={(value): void => { void navigate(value === 'admin' ? ROUTE_PATHS.adminDashboard : ROUTE_PATHS.appDashboard); }}
    />
  );
}
