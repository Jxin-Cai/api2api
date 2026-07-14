import { theme as antdTheme, type ThemeConfig } from 'antd';

import type { ThemeMode } from '@shared/config/stores/useThemeStore';

const FONT_FAMILY =
  '-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif';

/** Codex-inspired neutral palette aligned with tokens.css. */
const PALETTE = {
  light: {
    primary: '#20201f',
    bgPage: '#f7f7f5',
    bgSurface: '#ffffff',
    bgElevated: '#f3f3f1',
    textBase: '#20201f',
    textSecondary: '#62625f',
    border: '#e5e5e2',
    siderBg: '#191918',
    headerBg: 'rgba(247, 247, 245, 0.9)',
  },
  dark: {
    primary: '#f0f0ed',
    bgPage: '#181817',
    bgSurface: '#20201f',
    bgElevated: '#252524',
    textBase: '#f6f4f1',
    textSecondary: '#c9c3bb',
    border: '#393937',
    siderBg: '#191918',
    headerBg: 'rgba(24, 24, 23, 0.9)',
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
      borderRadiusLG: 10,
      fontFamily: FONT_FAMILY,
      colorBgLayout: p.bgPage,
      colorBgContainer: p.bgSurface,
      colorTextBase: p.textBase,
      colorTextLightSolid: mode === 'dark' ? '#20201f' : '#ffffff',
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
