import { theme as antdTheme, type ThemeConfig } from 'antd';

import type { ThemeMode } from '@shared/config/stores/useThemeStore';

const FONT_FAMILY =
  '-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif';

/** 暖灰色板（与 tokens.css 对齐） */
const PALETTE = {
  light: {
    primary: '#8b8680',
    bgPage: '#faf9f5',
    bgSurface: '#fffdf9',
    bgElevated: '#f0eee8',
    textBase: '#2d2a26',
    textSecondary: '#6d6760',
    border: '#e3e1db',
    siderBg: '#f0eee8',
    headerBg: 'rgba(255, 253, 249, 0.85)',
  },
  dark: {
    primary: '#9a948e',
    bgPage: '#151412',
    bgSurface: '#1d1b18',
    bgElevated: '#2a2723',
    textBase: '#f6f4f1',
    textSecondary: '#c9c3bb',
    border: '#3a3530',
    siderBg: '#1d1b18',
    headerBg: 'rgba(29, 27, 24, 0.85)',
  },
} as const;

/** 按模式构建 Ant Design 主题 */
export function buildAppTheme(mode: ThemeMode): ThemeConfig {
  const p = PALETTE[mode];
  return {
    algorithm: mode === 'dark' ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm,
    token: {
      colorPrimary: p.primary,
      colorInfo: p.primary,
      colorSuccess: '#10b981',
      colorWarning: '#d97706',
      colorError: '#c65746',
      borderRadius: 8,
      borderRadiusLG: 12,
      fontFamily: FONT_FAMILY,
      colorBgLayout: p.bgPage,
      colorBgContainer: p.bgSurface,
      colorTextBase: p.textBase,
      colorBorder: p.border,
      colorBorderSecondary: p.border,
      boxShadowTertiary:
        mode === 'dark'
          ? '0 6px 16px -4px rgba(0, 0, 0, 0.4)'
          : '0 4px 12px -2px rgba(0, 0, 0, 0.08)',
    },
    components: {
      Layout: {
        siderBg: p.siderBg,
        headerBg: p.headerBg,
        bodyBg: p.bgPage,
        triggerBg: p.bgElevated,
        triggerColor: p.textSecondary,
      },
      Menu: {
        itemBg: 'transparent',
        subMenuItemBg: 'transparent',
        itemColor: p.textSecondary,
        itemHoverColor: p.textBase,
        itemSelectedColor: p.textBase,
        itemSelectedBg: 'var(--primary-soft-strong)',
        itemHoverBg: 'var(--primary-soft)',
        itemBorderRadius: 10,
        itemMarginInline: 10,
      },
      Card: {
        borderRadiusLG: 12,
        boxShadowTertiary:
          mode === 'dark'
            ? '0 6px 16px -4px rgba(0, 0, 0, 0.4)'
            : '0 4px 12px -2px rgba(0, 0, 0, 0.08)',
      },
      Table: {
        cellPaddingBlock: 12,
        cellPaddingInline: 14,
        headerBg: p.bgElevated,
        borderColor: p.border,
      },
      Button: {
        borderRadius: 8,
        controlHeight: 36,
        primaryShadow: 'none',
      },
      Input: { borderRadius: 8 },
      Select: { borderRadius: 8 },
      Segmented: { borderRadius: 8 },
    },
  };
}
