import type { ReactNode } from 'react';
import {
  ApiOutlined,
  BarChartOutlined,
  BranchesOutlined,
  CheckCircleOutlined,
  CloudServerOutlined,
  ControlOutlined,
  DeploymentUnitOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons';
import { Card, Typography } from 'antd';

import { AuroraBackground, GradientText, SpotlightCard } from '@shared/ui';
import './AuthLayout.css';

interface AuthLayoutProps {
  /** 认证表单内容 */
  children: ReactNode;
}

const protocolItems = ['Claude Messages', 'OpenAI Responses', 'Chat Completions'];

const capabilityItems = [
  {
    icon: <ApiOutlined />,
    title: '协议转换',
    description: '统一接入主流网关协议，按转换定义执行直通或转换。',
  },
  {
    icon: <BranchesOutlined />,
    title: '渠道路由',
    description: '按模型、优先级与渠道状态生成可靠的候选计划。',
  },
  {
    icon: <BarChartOutlined />,
    title: '用量观测',
    description: '集中查看请求趋势、Token 用量与渠道运行状态。',
  },
  {
    icon: <SafetyCertificateOutlined />,
    title: '故障切换',
    description: '为渠道异常和模型不可用提供清晰的切换策略。',
  },
];

export function AuthLayout({ children }: AuthLayoutProps) {
  return (
    <main className="auth-layout">
      <AuroraBackground className="auth-layout__aurora" />

      <div className="auth-layout__shell">
        <nav className="auth-layout__nav" aria-label="登录页导航">
          <div className="auth-layout__brand" aria-label="意门">
            <span className="auth-layout__brand-mark">
              <img className="auth-layout__brand-logo" src="/logo.png" alt="" aria-hidden="true" />
            </span>
            <span className="auth-layout__brand-text">
              <strong>意门</strong>
              <small>Protocol Gateway</small>
            </span>
          </div>
          <span className="auth-layout__nav-status">
            <CheckCircleOutlined aria-hidden="true" />
            Unified AI Gateway
          </span>
        </nav>

        <div className="auth-layout__hero-grid">
          <section className="auth-layout__intro" aria-labelledby="auth-landing-title">
            <p className="auth-layout__eyebrow">
              <CloudServerOutlined aria-hidden="true" /> Enterprise API Gateway
            </p>
            <Typography.Title id="auth-landing-title" className="auth-layout__title">
              <GradientText>统一协议入口</GradientText>
              <br />
              管理多供应商 AI API 渠道
            </Typography.Title>
            <Typography.Paragraph className="auth-layout__desc">
              面向 Claude Messages、OpenAI Responses 与 Chat Completions 的协议转换、渠道路由、用量观测与故障切换平台。
            </Typography.Paragraph>

            <div className="auth-layout__protocols" aria-label="支持的协议">
              {protocolItems.map((item) => (
                <span key={item}>{item}</span>
              ))}
            </div>

            <SpotlightCard className="auth-layout__gateway-card" spotlightColor="rgba(198, 87, 70, 0.14)">
              <div className="auth-layout__gateway-head">
                <span>
                  <DeploymentUnitOutlined aria-hidden="true" /> Live routing plan
                </span>
                <em>Healthy</em>
              </div>
              <div className="auth-layout__gateway-flow" aria-label="网关路由拓扑">
                <div className="auth-layout__flow-node">Client Apps</div>
                <span className="auth-layout__flow-arrow" aria-hidden="true">→</span>
                <div className="auth-layout__flow-node auth-layout__flow-node--gateway">
                  <ControlOutlined aria-hidden="true" /> 意门 Gateway
                </div>
                <span className="auth-layout__flow-arrow" aria-hidden="true">→</span>
                <div className="auth-layout__flow-providers">
                  <span>Claude</span>
                  <span>OpenAI</span>
                  <span>Provider X</span>
                </div>
              </div>
            </SpotlightCard>
          </section>

          <aside className="auth-layout__panel" aria-label="登录控制台">
            <Card className="auth-layout__card">
              {children}
              <div className="auth-layout__security-note">
                <SafetyCertificateOutlined aria-hidden="true" />
                <span>凭据经加密连接传输，登录状态仅保存在当前设备</span>
              </div>
            </Card>
          </aside>
        </div>

        <section className="auth-layout__proof" aria-label="平台能力概览">
          <div className="auth-layout__metrics">
            <div className="auth-layout__metric">
              <strong>3+</strong>
              <span>协议入口</span>
            </div>
            <div className="auth-layout__metric">
              <strong>1 个</strong>
              <span>统一管理台</span>
            </div>
            <div className="auth-layout__metric">
              <strong>4 类</strong>
              <span>核心能力</span>
            </div>
          </div>

          <div className="auth-layout__capabilities">
            {capabilityItems.map((item) => (
              <SpotlightCard key={item.title} className="auth-layout__capability-card">
                <span className="auth-layout__capability-icon" aria-hidden="true">{item.icon}</span>
                <div>
                  <h2>{item.title}</h2>
                  <p>{item.description}</p>
                </div>
              </SpotlightCard>
            ))}
          </div>
        </section>
      </div>
    </main>
  );
}
