import type { CSSProperties, ReactNode } from 'react';

import styles from './TextEffects.module.css';

interface ShinyTextProps {
  children: ReactNode;
  className?: string;
  style?: CSSProperties;
  /** 扫光周期（秒） */
  speed?: number;
  /** 是否禁用 */
  disabled?: boolean;
}

/**
 * reactbits ShinyText（纯 CSS）
 * 文字上有一道高光循环扫过。
 */
export function ShinyText({ children, className, style, speed = 4, disabled = false }: ShinyTextProps) {
  return (
    <span
      className={[styles.shinyText, disabled ? styles.shinyDisabled : '', className]
        .filter(Boolean)
        .join(' ')}
      style={{ ...style, animationDuration: `${speed}s` }}
    >
      {children}
    </span>
  );
}
