import type { ReactNode } from 'react';
import { SafetyCertificateOutlined } from '@ant-design/icons';
import { Card } from 'antd';

import { AuroraBackground } from '@shared/ui';
import './AuthLayout.css';

interface AuthLayoutProps {
  /** 认证表单内容 */
  children: ReactNode;
}

export function AuthLayout({ children }: AuthLayoutProps) {
  return (
    <main className="auth-layout">
      <AuroraBackground className="auth-layout__aurora" />
      <div className="auth-layout__glow auth-layout__glow--top" aria-hidden="true" />
      <div className="auth-layout__glow auth-layout__glow--bottom" aria-hidden="true" />

      <section className="auth-layout__content" aria-label="意门控制台登录">
        <header className="auth-layout__brand">
          <span className="auth-layout__brand-mark">
            <img className="auth-layout__brand-logo" src="/logo.png" alt="" aria-hidden="true" />
          </span>
          <span className="auth-layout__brand-text">
            <strong>意门</strong>
            <small>Protocol Gateway</small>
          </span>
        </header>

        <Card className="auth-layout__card">
          {children}
          <div className="auth-layout__security-note">
            <SafetyCertificateOutlined aria-hidden="true" />
            <span>凭据经加密连接传输，登录状态仅保存在当前设备</span>
          </div>
        </Card>

        <p className="auth-layout__footer">统一管理 AI 协议、模型与供应商渠道</p>
      </section>
    </main>
  );
}
