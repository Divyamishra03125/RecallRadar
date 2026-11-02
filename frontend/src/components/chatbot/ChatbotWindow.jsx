import React, { useState } from 'react';
import ChatbotHeader from './ChatbotHeader';
import ChatMessageList from './ChatMessageList';
import ChatInput from './ChatInput';
import ChatHistorySidebar from './ChatHistorySidebar';

export default function ChatbotWindow({
  isOpen,
  isExpanded,
  messages,
  busy,
  error,
  aiHealth,
  sessions,
  activeSessionId,
  onSend,
  onSelectSuggestion,
  onRegenerate,
  onToggleExpand,
  onMinimize,
  onClose,
  onNewSession,
  onSelectSession,
  onDeleteSession
}) {
  const [showHistorySidebar, setShowHistorySidebar] = useState(true);

  if (!isOpen) return null;

  if (isExpanded) {
    return (
      <div
        className="chatbot-fullscreen-overlay"
        style={{
          position: 'fixed',
          top: 0,
          left: 0,
          width: '100vw',
          height: '100vh',
          zIndex: 10000,
          background: 'rgba(5, 10, 20, 0.75)',
          backdropFilter: 'blur(8px)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '16px'
        }}
      >
        <div
          style={{
            width: '100%',
            maxWidth: '1280px',
            height: '92vh',
            background: 'var(--panel)',
            border: '1px solid var(--border)',
            borderRadius: '16px',
            boxShadow: '0 24px 64px rgba(0, 0, 0, 0.45)',
            display: 'flex',
            overflow: 'hidden',
            position: 'relative'
          }}
        >
          {/* History Sidebar */}
          {showHistorySidebar && (
            <ChatHistorySidebar
              sessions={sessions}
              activeSessionId={activeSessionId}
              onSelectSession={onSelectSession}
              onNewSession={onNewSession}
              onDeleteSession={onDeleteSession}
            />
          )}

          {/* Main Chat Area */}
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0, background: 'var(--bg)' }}>
            <ChatbotHeader
              isExpanded={true}
              onToggleExpand={onToggleExpand}
              onMinimize={onMinimize}
              onClose={onClose}
              onNewSession={onNewSession}
              aiHealth={aiHealth}
              showSidebarToggle={true}
              onToggleSidebar={() => setShowHistorySidebar((prev) => !prev)}
            />

            {error && (
              <div
                style={{
                  padding: '8px 16px',
                  background: '#fef2f2',
                  color: '#b91c1c',
                  fontSize: '0.8rem',
                  borderBottom: '1px solid #fecaca'
                }}
              >
                {error}
              </div>
            )}

            <ChatMessageList
              messages={messages}
              busy={busy}
              onSelectSuggestion={onSelectSuggestion}
              onRegenerate={onRegenerate}
            />

            <ChatInput onSend={onSend} busy={busy} />
          </div>
        </div>
      </div>
    );
  }

  // Popup Compact Mode
  return (
    <div
      className="chatbot-popup-container"
      style={{
        position: 'fixed',
        bottom: '24px',
        right: '24px',
        width: '420px',
        maxWidth: 'calc(100vw - 32px)',
        height: '620px',
        maxHeight: 'calc(100vh - 48px)',
        zIndex: 9999,
        background: 'var(--panel)',
        border: '1px solid var(--border)',
        borderRadius: '16px',
        boxShadow: '0 16px 40px rgba(0, 0, 0, 0.28)',
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
        animation: 'popupEnter 0.25s cubic-bezier(0.16, 1, 0.3, 1)'
      }}
    >
      <ChatbotHeader
        isExpanded={false}
        onToggleExpand={onToggleExpand}
        onMinimize={onMinimize}
        onClose={onClose}
        onNewSession={onNewSession}
        aiHealth={aiHealth}
        showSidebarToggle={false}
      />

      {error && (
        <div
          style={{
            padding: '6px 12px',
            background: '#fef2f2',
            color: '#b91c1c',
            fontSize: '0.78rem',
            borderBottom: '1px solid #fecaca'
          }}
        >
          {error}
        </div>
      )}

      <ChatMessageList
        messages={messages}
        busy={busy}
        onSelectSuggestion={onSelectSuggestion}
        onRegenerate={onRegenerate}
      />

      <ChatInput onSend={onSend} busy={busy} />
    </div>
  );
}
