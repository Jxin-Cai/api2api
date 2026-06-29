import type { ReactNode } from 'react';
import { motion } from 'framer-motion';

interface AnimatedContentProps {
  children: ReactNode;
  /** 延迟（秒），用于错位出场 */
  delay?: number;
  /** 位移方向距离（px），正数从下方进入 */
  distance?: number;
  /** 动画时长（秒） */
  duration?: number;
  className?: string;
  /** 是否进入视口才触发；false 则挂载即触发 */
  inView?: boolean;
}

/**
 * reactbits AnimatedContent（轻量版）
 * 子内容渐入 + 轻微上移，支持错位延迟。
 */
export function AnimatedContent({
  children,
  delay = 0,
  distance = 16,
  duration = 0.45,
  className,
  inView = false,
}: AnimatedContentProps) {
  const animateProps = inView
    ? { whileInView: { opacity: 1, y: 0 }, viewport: { once: true, margin: '-40px' } }
    : { animate: { opacity: 1, y: 0 } };

  return (
    <motion.div
      className={className}
      initial={{ opacity: 0, y: distance }}
      transition={{ duration, delay, ease: 'easeOut' }}
      {...animateProps}
    >
      {children}
    </motion.div>
  );
}
