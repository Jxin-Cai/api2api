import { useEffect, useRef } from 'react';
import { animate, useInView } from 'framer-motion';

interface CountUpProps {
  /** 目标数值 */
  to: number;
  /** 起始数值 */
  from?: number;
  /** 动画时长（秒） */
  duration?: number;
  /** 小数位 */
  decimals?: number;
  /** 前缀 */
  prefix?: string;
  /** 后缀 */
  suffix?: string;
  /** 千分位分隔 */
  separator?: boolean;
  className?: string;
}

/**
 * reactbits CountUp（轻量版）
 * 数字从 from 滚动到 to，进入视口时触发。
 */
export function CountUp({
  to,
  from = 0,
  duration = 1.2,
  decimals = 0,
  prefix = '',
  suffix = '',
  separator = true,
  className,
}: CountUpProps) {
  const ref = useRef<HTMLSpanElement>(null);
  const inView = useInView(ref, { once: true, margin: '-40px' });

  useEffect(() => {
    const node = ref.current;
    if (!node) {
      return;
    }

    function format(value: number): string {
      const fixed = value.toFixed(decimals);
      if (!separator) {
        return `${prefix}${fixed}${suffix}`;
      }
      const [intPart, decPart] = fixed.split('.');
      const grouped = intPart.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
      return `${prefix}${decPart ? `${grouped}.${decPart}` : grouped}${suffix}`;
    }

    node.textContent = format(from);

    if (!inView) {
      return;
    }

    const controls = animate(from, to, {
      duration,
      ease: 'easeOut',
      onUpdate: (value: number): void => {
        node.textContent = format(value);
      },
    });

    return (): void => controls.stop();
  }, [inView, to, from, duration, decimals, prefix, suffix, separator]);

  return <span ref={ref} className={className} />;
}
