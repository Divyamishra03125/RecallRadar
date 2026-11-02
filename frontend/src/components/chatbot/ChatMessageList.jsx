import React, { useEffect, useRef } from 'react';
import { Bot } from 'lucide-react';
import ChatMessage from './ChatMessage';
import WelcomeScreen from './WelcomeScreen';

export default function ChatMessageList({
  messages,
  busy,
  onSelectSuggestion,
  onRegenerate
}) {
  const bottomRef = useRef(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, busy]);

  const isEmpty = !messages || messages.length === 0;

  return (
    <div
      style={{
        flex: 1,
        overflowY: 'auto',
        padding: '16px',
        display: 'flex',
        flexDirection: 'column',
        gap: '16px',
        background: 'var(--bg)'
      }}
    >
      {isEmpty ? (
        <WelcomeScreen onSelectQuery={onSelectSuggestion} />
      ) : (
        messages.map((m, idx) => (
          <ChatMessage
            key={m.id || idx}
            message={m}
            onSelectSuggestion={onSelectSuggestion}
            onRegenerate={onRegenerate}
            isLatest={idx === messages.length - 1}
            busy={busy}
          />
        ))
      )}

      {/* Typing Indicator */}
      {busy && (
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            color: 'var(--muted)',
            fontSize: '0.82rem',
            paddingLeft: '4px',
            animation: 'pulse 1.5s infinite'
          }}
        >
          <div
            style={{
              width: '24px',
              height: '24px',
              borderRadius: '50%',
              background: 'linear-gradient(135deg, #4f46e5 0%, #06b6d4 100%)',
              color: '#ffffff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}
          >
            <Bot size={13} />
          </div>
          <span>RecallRadar AI is querying verified PostgreSQL records…</span>
        </div>
      )}

      <div ref={bottomRef} style={{ height: '4px' }} />
    </div>
  );
}
