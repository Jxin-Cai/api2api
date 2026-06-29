import type { ReactNode } from 'react';
import { QueryClientProvider } from '@tanstack/react-query';
import { ConfigProvider, App as AntdApp } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { MotionConfig } from 'framer-motion';

import { useThemeStore } from '@shared/config/stores/useThemeStore';
import { queryClient } from './queryClient';
import { buildAppTheme } from './theme';

interface AppProvidersProps {
  /** 应用子节点 */
  children: ReactNode;
}

export function AppProviders({ children }: AppProvidersProps) {
  const mode = useThemeStore((state) => state.mode);

  return (
    <QueryClientProvider client={queryClient}>
      <ConfigProvider locale={zhCN} theme={buildAppTheme(mode)}>
        <MotionConfig reducedMotion="user">
          <AntdApp>{children}</AntdApp>
        </MotionConfig>
      </ConfigProvider>
    </QueryClientProvider>
  );
}
