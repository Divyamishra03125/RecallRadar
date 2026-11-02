import React from 'react';
import {
  Bot,
  Plus,
  Maximize2,
  Minimize2,
  Minus,
  X,
  Activity,
  ShieldCheck,
  Menu
} from 'lucide-react';

export default function ChatbotHeader({
  isExpanded,
  onToggleExpand,
  onMinimize,
  onClose,
  onNewSession,
  aiHealth,
  onToggleSidebar,
  showSidebarToggle
}) {
  const getStatusBadge = () => {
    if (!aiHealth) {
      return { label: 'Checking connection…', color: '#3b82f6', bg: 'rgba(59, 130, 246, 0.15)' };
    }
    if (aiHealth.status === 'UP') {
      return { label: 'Connected', color: '#10b981', bg: 'rgba(16, 185, 129, 0.15)' };
    }
    if (aiHealth.status === 'DEGRADED') {
      return { label: 'AI Degraded (Database Active)', color: '#f59e0b', bg: 'rgba(245, 158, 11, 0.15)' };
    }
    return { label: 'Database Active', color: '#6366f1', bg: 'rgba(99, 102, 241, 0.15)' };
  };

  const status = getStatusBadge();

  return (
    <div
      style={{
        padding: '12px 16px',
        background: 'linear-gradient(135deg, #1e1b4b 0%, #0f172a 100%)',
        borderBottom: '1px solid var(--border)',
        color: '#ffffff',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexShrink: 0
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
        {showSidebarToggle && (
          <button
            type="button"
            onClick={onToggleSidebar}
            style={{
              background: 'none',
              border: 'none',
              color: '#94a3b8',
              cursor: 'pointer',
              padding: '4px',
              display: 'flex',
              alignItems: 'center'
            }}
            title="Toggle conversation list"
          >
            <Menu size={18} />
          </button>
        )}

        <div
          style={{
            width: '32px',
            height: '32px',
            borderRadius: '50%',
            background: 'linear-gradient(135deg, #4f46e5 0%, #06b6d4 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#ffffff',
            boxShadow: '0 2px 8px rgba(79, 70, 229, 0.4)'
          }}
        >
          <Bot size={18} />
        </div>

        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <b style={{ fontSize: '0.94rem', color: '#ffffff' }}>RecallRadar AI</b>
            <span
              style={{
                fontSize: '0.68rem',
                padding: '2px 6px',
                borderRadius: '10px',
                background: status.bg,
                color: status.color,
                fontWeight: 600,
                display: 'inline-flex',
                alignItems: 'center',
                gap: '4px'
              }}
              title={`Backend status: ${status.label}`}
            >
              <span
                style={{
                  width: '6px',
                  height: '6px',
                  borderRadius: '50%',
                  background: status.color
                }}
              />
              {status.label}
            </span>
          </div>
          <small style={{ fontSize: '0.72rem', color: '#94a3b8', display: 'block' }}>
            Product Safety Intelligence Assistant
          </small>
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
        {/* New Session Button */}
        <button
          type="button"
          onClick={onNewSession}
          style={{
            background: 'none',
            border: 'none',
            color: '#cbd5e1',
            cursor: 'pointer',
            padding: '6px',
            borderRadius: '6px',
            display: 'flex',
            alignItems: 'center',
            transition: 'background 0.15s ease'
          }}
          onMouseEnter={(e) => (e.currentTarget.style.background = 'rgba(255, 255, 255, 0.1)')}
          onMouseLeave={(e) => (e.currentTarget.style.background = 'none')}
          title="Start new conversation"
        >
          <Plus size={16} />
        </button>

        {/* Expand / Minimize Fullscreen Toggle */}
        <button
          type="button"
          onClick={onToggleExpand}
          style={{
            background: 'none',
            border: 'none',
            color: '#cbd5e1',
            cursor: 'pointer',
            padding: '6px',
            borderRadius: '6px',
            display: 'flex',
            alignItems: 'center',
            transition: 'background 0.15s ease'
          }}
          onMouseEnter={(e) => (e.currentTarget.style.background = 'rgba(255, 255, 255, 0.1)')}
          onMouseLeave={(e) => (e.currentTarget.style.background = 'none')}
          title={isExpanded ? 'Collapse to popup mode' : 'Expand to full screen workspace'}
        >
          {isExpanded ? <Minimize2 size={16} /> : <Maximize2 size={16} />}
        </button>

        {/* Minimize Button */}
        {!isExpanded && (
          <button
            type="button"
            onClick={onMinimize}
            style={{
              background: 'none',
              border: 'none',
              color: '#cbd5e1',
              cursor: 'pointer',
              padding: '6px',
              borderRadius: '6px',
              display: 'flex',
              alignItems: 'center',
              transition: 'background 0.15s ease'
            }}
            onMouseEnter={(e) => (e.currentTarget.style.background = 'rgba(255, 255, 255, 0.1)')}
            onMouseLeave={(e) => (e.currentTarget.style.background = 'none')}
            title="Minimize assistant"
          >
            <Minus size={16} />
          </button>
        )}

        {/* Close Button */}
        <button
          type="button"
          onClick={onClose}
          style={{
            background: 'none',
            border: 'none',
            color: '#cbd5e1',
            cursor: 'pointer',
            padding: '6px',
            borderRadius: '6px',
            display: 'flex',
            alignItems: 'center',
            transition: 'background 0.15s ease'
          }}
          onMouseEnter={(e) => (e.currentTarget.style.background = 'rgba(239, 68, 68, 0.2)')}
          onMouseLeave={(e) => (e.currentTarget.style.background = 'none')}
          title="Close assistant"
        >
          <X size={16} />
        </button>
      </div>
    </div>
  );
}
