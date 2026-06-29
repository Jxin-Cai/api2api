import { create } from 'zustand';

import type { PortalType } from '@shared/types/portal';

interface UiStoreState {
  /** 侧栏是否折叠 */
  sidebarCollapsed: boolean;
  /** 移动端 Drawer 是否打开 */
  mobileDrawerOpen: boolean;
  /** 最近使用门户 */
  preferredPortal: PortalType;
  /** 设置侧栏折叠态 */
  setSidebarCollapsed: (collapsed: boolean) => void;
  /** 设置移动端 Drawer */
  setMobileDrawerOpen: (open: boolean) => void;
  /** 设置门户偏好 */
  setPreferredPortal: (portal: PortalType) => void;
}

export const useUiStore = create<UiStoreState>((set) => ({
  sidebarCollapsed: false,
  mobileDrawerOpen: false,
  preferredPortal: 'app',
  setSidebarCollapsed: (collapsed: boolean): void => set({ sidebarCollapsed: collapsed }),
  setMobileDrawerOpen: (open: boolean): void => set({ mobileDrawerOpen: open }),
  setPreferredPortal: (portal: PortalType): void => set({ preferredPortal: portal }),
}));
