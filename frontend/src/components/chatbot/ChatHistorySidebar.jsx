import React, { useState } from 'react';
import { PlusCircle, Search, MessageSquare, Trash2, Bot, ShieldCheck } from 'lucide-react';

export default function ChatHistorySidebar({
  sessions,
  activeSessionId,
  onSelectSession,
  onNewSession,
  onDeleteSession,
  onClose
}) {
  const [search, setSearch] = useState('');

  const filteredSessions = sessions.filter((s) => {
    if (!search.trim()) return true;
    const title = (s.title || 'Conversation').toLowerCase();
    return title.includes(search.toLowerCase().trim());
  });

  return (
    <aside
      style={{
        width: '280px',
        background: '#0d1527',
        borderRight: '1px solid var(--border)',
        display: 'flex',
        flexDirection: 'column',
        flexShrink: 0
      }}
    >
      {/* Brand & New Chat */}
      <div style={{ padding: '16px', borderBottom: '1px solid var(--border)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '14px' }}>
          <div
            style={{
              width: '30px',
              height: '30px',
              borderRadius: '8px',
              background: 'linear-gradient(135deg, #4f46e5 0%, #06b6d4 100%)',
              color: '#ffffff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}
          >
            <Bot size={18} />
          </div>
          <div>
            <b style={{ fontSize: '0.94rem', color: '#ffffff', display: 'block' }}>RecallRadar AI</b>
            <small style={{ fontSize: '0.72rem', color: '#94a3b8' }}>Safety Intelligence</small>
          </div>
        </div>

        <button
          type="button"
          onClick={onNewSession}
          style={{
            width: '100%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '8px',
            padding: '8px 12px',
            borderRadius: '8px',
            background: 'linear-gradient(135deg, #4f46e5 0%, #3b82f6 100%)',
            color: '#ffffff',
            border: 'none',
            fontWeight: 600,
            fontSize: '0.84rem',
            cursor: 'pointer',
            boxShadow: '0 4px 12px rgba(79, 70, 229, 0.3)',
            transition: 'opacity 0.2s ease'
          }}
          onMouseEnter={(e) => (e.currentTarget.style.opacity = '0.9')}
          onMouseLeave={(e) => (e.currentTarget.style.opacity = '1')}
        >
          <PlusCircle size={16} />
          <span>New Investigation</span>
        </button>
      </div>

      {/* Search Input */}
      <div style={{ padding: '10px 14px', borderBottom: '1px solid var(--border)' }}>
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            background: 'rgba(255, 255, 255, 0.05)',
            borderRadius: '6px',
            padding: '4px 8px',
            border: '1px solid var(--border)'
          }}
        >
          <Search size={13} style={{ color: '#94a3b8' }} />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search conversations…"
            style={{
              background: 'transparent',
              border: 'none',
              outline: 'none',
              color: '#ffffff',
              fontSize: '0.8rem',
              width: '100%'
            }}
          />
        </div>
      </div>

      {/* Sessions List */}
      <div style={{ flex: 1, overflowY: 'auto', padding: '10px 8px', display: 'flex', flexDirection: 'column', gap: '4px' }}>
        <span style={{ fontSize: '0.68rem', fontWeight: 700, color: '#64748b', padding: '4px 8px', textTransform: 'uppercase' }}>
          Recent Investigations
        </span>

        {filteredSessions.length === 0 ? (
          <div style={{ padding: '16px 8px', textAlign: 'center', color: '#64748b', fontSize: '0.78rem' }}>
            No conversations found
          </div>
        ) : (
          filteredSessions.map((s) => {
            const isActive = s.id === activeSessionId;
            return (
              <div
                key={s.id}
                onClick={() => onSelectSession(s.id)}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '8px 10px',
                  borderRadius: '6px',
                  cursor: 'pointer',
                  background: isActive ? 'rgba(79, 70, 229, 0.2)' : 'transparent',
                  border: isActive ? '1px solid rgba(79, 70, 229, 0.4)' : '1px solid transparent',
                  color: isActive ? '#ffffff' : '#cbd5e1',
                  fontSize: '0.82rem',
                  transition: 'all 0.15s ease'
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', overflow: 'hidden' }}>
                  <MessageSquare size={14} style={{ color: isActive ? '#818cf8' : '#64748b', flexShrink: 0 }} />
                  <span style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {s.title || 'Safety Investigation'}
                  </span>
                </div>

                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation();
                    onDeleteSession(s.id);
                  }}
                  style={{
                    background: 'none',
                    border: 'none',
                    color: '#64748b',
                    cursor: 'pointer',
                    padding: '2px',
                    display: 'flex',
                    alignItems: 'center'
                  }}
                  title="Delete conversation"
                >
                  <Trash2 size={13} />
                </button>
              </div>
            );
          })
        )}
      </div>

      {/* Footer Info */}
      <div style={{ padding: '12px 14px', borderTop: '1px solid var(--border)', fontSize: '0.72rem', color: '#64748b', display: 'flex', alignItems: 'center', gap: '6px' }}>
        <ShieldCheck size={14} style={{ color: '#10b981' }} />
        <span>Grounded in PostgreSQL</span>
      </div>
    </aside>
  );
}
