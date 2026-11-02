import React from 'react';
import { Search, ShieldAlert, BarChart3, Database, Bot } from 'lucide-react';

const STARTER_CARDS = [
  {
    icon: Search,
    title: 'Investigate a Product',
    desc: 'Verify if a smartphone, appliance, or toy has recorded recall notices.',
    query: 'Check if Samsung Galaxy S24 has any recalls',
    accent: '#4f46e5'
  },
  {
    icon: ShieldAlert,
    title: 'Search Product Recalls',
    desc: 'Retrieve active regulatory safety warnings and hazard causes.',
    query: 'Show all active recalls',
    accent: '#ea580c'
  },
  {
    icon: BarChart3,
    title: 'Analyze Safety Risks',
    desc: 'Examine high-severity alerts, brand comparisons, and trends.',
    query: 'Are there any critical recalls in the database?',
    accent: '#dc2626'
  },
  {
    icon: Database,
    title: 'Explore Recall Statistics',
    desc: 'Get a live breakdown of total products, active notices, and severity tiers.',
    query: 'How many recalls are in the database?',
    accent: '#06b6d4'
  }
];

export default function WelcomeScreen({ onSelectQuery }) {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        textAlign: 'center',
        padding: '24px 16px',
        maxWidth: '560px',
        margin: '0 auto'
      }}
    >
      <div
        style={{
          width: '54px',
          height: '54px',
          borderRadius: '16px',
          background: 'linear-gradient(135deg, #4f46e5 0%, #06b6d4 100%)',
          color: '#ffffff',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: '0 8px 20px rgba(79, 70, 229, 0.3)',
          marginBottom: '14px'
        }}
      >
        <Bot size={30} />
      </div>

      <h3
        style={{
          margin: '0 0 6px 0',
          fontSize: '1.25rem',
          fontWeight: 700,
          color: 'var(--text)'
        }}
      >
        Hello! I'm RecallRadar AI.
      </h3>

      <p
        style={{
          margin: '0 0 20px 0',
          fontSize: '0.86rem',
          color: 'var(--muted)',
          lineHeight: 1.5,
          maxWidth: '460px'
        }}
      >
        Your intelligent Product Safety Assistant. I can help you investigate products, analyze recall
        notices, understand safety risks and explore your RecallRadar data.
      </p>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
          gap: '10px',
          width: '100%',
          textAlign: 'left'
        }}
      >
        {STARTER_CARDS.map((card, i) => {
          const Icon = card.icon;
          return (
            <button
              key={i}
              type="button"
              onClick={() => onSelectQuery(card.query)}
              style={{
                border: '1px solid var(--border)',
                borderRadius: '10px',
                padding: '12px 14px',
                background: 'var(--panel)',
                cursor: 'pointer',
                display: 'flex',
                flexDirection: 'column',
                gap: '4px',
                transition: 'all 0.2s ease',
                boxShadow: '0 2px 4px rgba(0, 0, 0, 0.04)'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.borderColor = card.accent;
                e.currentTarget.style.transform = 'translateY(-2px)';
                e.currentTarget.style.boxShadow = `0 6px 16px ${card.accent}22`;
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.borderColor = 'var(--border)';
                e.currentTarget.style.transform = 'none';
                e.currentTarget.style.boxShadow = '0 2px 4px rgba(0, 0, 0, 0.04)';
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: card.accent }}>
                <Icon size={16} />
                <strong style={{ fontSize: '0.86rem', color: 'var(--text)' }}>{card.title}</strong>
              </div>
              <span style={{ fontSize: '0.74rem', color: 'var(--muted)', lineHeight: 1.35 }}>
                {card.desc}
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
