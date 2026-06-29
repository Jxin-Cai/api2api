import type { ReactNode } from 'react';
import { motion } from 'framer-motion';

interface FadeContentProps {
  children: ReactNode;
  duration?: number;
  delay?: number;
  /** 是否模糊渐入 */
  blur?: boolean;
  className?: string;
}

/**
 * reactbits FadeContent（轻量版）
 * 纯透明度（可选模糊）渐入，无位移。
 */
export function FadeContent({ children, duration = 0.5, delay = 0, blur = false, className }: FadeContentProps) {
  return (
    <motion.div
      className={className}
      initial={{ opacity: 0, filter: blur ? 'blur(6px)' : 'blur(0px)' }}
      animate={{ opacity: 1, filter: 'blur(0px)' }}
      transition={{ duration, delay, ease: 'easeOut' }}
    >
      {children}
    </motion.div>
  );
}
