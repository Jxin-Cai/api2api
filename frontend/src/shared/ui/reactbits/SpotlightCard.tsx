import { useRef, type CSSProperties, type MouseEvent, type ReactNode } from 'react';

import styles from './SpotlightCard.module.css';

interface SpotlightCardProps {
  children: ReactNode;
  className?: string;
  style?: CSSProperties;
  /** 聚光颜色 */
  spotlightColor?: string;
  onClick?: () => void;
}

/**
 * reactbits SpotlightCard（轻量版）
 * 鼠标移动时在卡片上生成跟随的径向高光。
 */
export function SpotlightCard({
  children,
  className,
  style,
  spotlightColor = 'var(--primary-soft-strong)',
  onClick,
}: SpotlightCardProps) {
  const ref = useRef<HTMLDivElement>(null);

  function handleMouseMove(event: MouseEvent<HTMLDivElement>): void {
    const node = ref.current;
    if (!node) {
      return;
    }
    const rect = node.getBoundingClientRect();
    node.style.setProperty('--spot-x', `${event.clientX - rect.left}px`);
    node.style.setProperty('--spot-y', `${event.clientY - rect.top}px`);
    node.style.setProperty('--spot-opacity', '1');
  }

  function handleMouseLeave(): void {
    ref.current?.style.setProperty('--spot-opacity', '0');
  }

  return (
    <div
      ref={ref}
      className={[styles.card, onClick ? styles.clickable : '', className].filter(Boolean).join(' ')}
      style={{ ...style, '--spot-color': spotlightColor } as CSSProperties}
      onMouseMove={handleMouseMove}
      onMouseLeave={handleMouseLeave}
      onClick={onClick}
    >
      {children}
    </div>
  );
}
