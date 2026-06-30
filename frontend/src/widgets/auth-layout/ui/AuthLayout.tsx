import type { ReactNode } from 'react';
import {
  ApiOutlined,
  BarChartOutlined,
  BranchesOutlined,
  CheckCircleOutlined,
  CloudServerOutlined,
  ControlOutlined,
  DashboardOutlined,
  DeploymentUnitOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons';
import { Card, Typography } from 'antd';

import {
  AuroraBackground,
  GradientText,
  SpotlightCard,
} from '@shared/ui';
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
    description: '统一接入三类主流网关协议，按数据库转换定义执行直通或转换。',
  },
  {
    icon: <BranchesOutlined />,
    title: '渠道路由',
    description: '按模型、优先级与渠道状态生成候选计划，降低上游切换成本。',
  },
  {
    icon: <BarChartOutlined />,
    title: '统一路由',
    description: '在一个管理台内维护协议、模型与渠道配置。',
  },
  {
    icon: <SafetyCertificateOutlined />,
    title: 'Failover 策略',
    description: '面向渠道异常、模型不可用等场景预留故障切换决策。',
  },
];

export function AuthLayout({ children }: AuthLayoutProps) {
  return (
    <main className="auth-layout">
      <AuroraBackground className="auth-layout__aurora" />
      <section className="auth-layout__landing" aria-labelledby="auth-landing-title">
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
          <a className="auth-layout__nav-cta" href="#login-panel">
            登录控制台
          </a>
        </nav>

        <div className="auth-layout__hero-grid">
          <div className="auth-layout__hero-copy">
            <p className="auth-layout__eyebrow">
              <CloudServerOutlined /> Enterprise API Gateway
            </p>
            <Typography.Title id="auth-landing-title" className="auth-layout__title">
              <GradientText>统一协议入口</GradientText>
              <br />
              管理多供应商 AI API 渠道
            </Typography.Title>
            <Typography.Paragraph className="auth-layout__desc">
              面向 Claude Messages、OpenAI Responses 与 Chat Completions 的协议转换、渠道路由、用量观测与故障切换平台。
            </Typography.Paragraph>

            <div className="auth-layout__hero-actions">
              <a className="auth-layout__primary-link" href="#login-panel">
                进入管理台
                <DashboardOutlined />
              </a>
              <span className="auth-layout__action-note">MVP 环境仅需用户名即可体验</span>
            </div>

            <div className="auth-layout__protocols">
              {protocolItems.map((item) => (
                <span key={item}>{item}</span>
              ))}
            </div>
          </div>

          <div className="auth-layout__gateway-wrap">
            <SpotlightCard className="auth-layout__gateway-card" spotlightColor="rgba(198, 87, 70, 0.16)">
              <div className="auth-layout__gateway-head">
                <span>
                  <DeploymentUnitOutlined /> Live routing plan
                </span>
                <em>Healthy</em>
              </div>
              <div className="auth-layout__gateway-flow" aria-label="网关路由拓扑">
                <div className="auth-layout__flow-node auth-layout__flow-node--client">Client Apps</div>
                <div className="auth-layout__flow-line" />
                <div className="auth-layout__flow-node auth-layout__flow-node--gateway">
                  <ControlOutlined /> 意门 Gateway
                </div>
                <div className="auth-layout__flow-grid">
                  <span>Claude</span>
                  <span>OpenAI</span>
                  <span>Provider X</span>
                </div>
              </div>
              <div className="auth-layout__gateway-status">
                <span>
                  <CheckCircleOutlined /> Routed by priority
                </span>
                <span>Observed usage</span>
              </div>
            </SpotlightCard>
          </div>
        </div>

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

        <div className="auth-layout__capabilities" aria-label="核心能力">
          {capabilityItems.map((item) => (
            <SpotlightCard key={item.title} className="auth-layout__capability-card">
              <span className="auth-layout__capability-icon" aria-hidden="true">
                {item.icon}
              </span>
              <div>
                <h2>{item.title}</h2>
                <p>{item.description}</p>
              </div>
            </SpotlightCard>
          ))}
        </div>
      </section>

      <aside id="login-panel" className="auth-layout__panel" aria-label="登录控制台">
        <Card className="auth-layout__card">{children}</Card>
      </aside>
    </main>
  );
}
