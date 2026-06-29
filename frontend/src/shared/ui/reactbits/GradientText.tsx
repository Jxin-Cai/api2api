import type { CSSProperties, ReactNode } from 'react';

import styles from './TextEffects.module.css';

interface GradientTextProps {
  children: ReactNode;
  className?: string;
  style?: CSSProperties;
  /** 是否启用流动动画 */
  animate?: boolean;
}

/**
 * reactbits GradientText（纯 CSS）
 * 文字用品牌渐变填充，可选缓慢流动。
 */
export function GradientText({ children, className, style, animate = true }: GradientTextProps) {
  return (
    <span
      className={[styles.gradientText, animate ? styles.gradientAnimate : '', className]
        .filter(Boolean)
        .join(' ')}
      style={style}
    >
      {children}
    </span>
  );
}
