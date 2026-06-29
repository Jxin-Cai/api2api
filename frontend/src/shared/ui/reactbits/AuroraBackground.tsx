import styles from './AuroraBackground.module.css';

interface AuroraBackgroundProps {
  className?: string;
}

/**
 * reactbits Aurora（轻量 CSS 版，无 WebGL）
 * 用多层模糊渐变球缓慢漂移，模拟极光流体背景。
 */
export function AuroraBackground({ className }: AuroraBackgroundProps) {
  return (
    <div className={[styles.aurora, className].filter(Boolean).join(' ')} aria-hidden="true">
      <span className={`${styles.blob} ${styles.blob1}`} />
      <span className={`${styles.blob} ${styles.blob2}`} />
      <span className={`${styles.blob} ${styles.blob3}`} />
      <div className={styles.grain} />
    </div>
  );
}
