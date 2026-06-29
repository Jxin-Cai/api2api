import { AppShell } from '@widgets/app-shell';
import type { PortalType } from '@shared/types/portal';

interface ProtectedRouteLayoutProps {
  portal: PortalType;
}

export function ProtectedRouteLayout({ portal }: ProtectedRouteLayoutProps) {
  return <AppShell portal={portal} />;
}

