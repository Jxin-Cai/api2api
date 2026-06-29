import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export type ThemeMode = 'light' | 'dark';

interface ThemeStoreState {
  /** 当前主题模式 */
  mode: ThemeMode;
  /** 设置主题模式 */
  setMode: (mode: ThemeMode) => void;
  /** 切换深浅色 */
  toggleMode: () => void;
}

/** 将主题写到 <html data-theme> 供 CSS 变量切换 */
function applyThemeAttribute(mode: ThemeMode): void {
  if (typeof document !== 'undefined') {
    document.documentElement.setAttribute('data-theme', mode);
  }
}

export const useThemeStore = create<ThemeStoreState>()(
  persist(
    (set, get) => ({
      mode: 'light',
      setMode: (mode: ThemeMode): void => {
        applyThemeAttribute(mode);
        set({ mode });
      },
      toggleMode: (): void => {
        const next: ThemeMode = get().mode === 'light' ? 'dark' : 'light';
        applyThemeAttribute(next);
        set({ mode: next });
      },
    }),
    {
      name: 'api2api-theme',
      onRehydrateStorage: () => (state): void => {
        applyThemeAttribute(state?.mode ?? 'light');
      },
    },
  ),
);
