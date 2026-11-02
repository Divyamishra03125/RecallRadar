import React from 'react';
import { ChatbotProvider, useChatbot } from './chatbot/ChatbotContext';
import ChatbotLauncher from './chatbot/ChatbotLauncher';
import ChatbotWindow from './chatbot/ChatbotWindow';

function ChatbotRoot() {
  const {
    isOpen,
    isExpanded,
    messages,
    busy,
    error,
    aiHealth,
    sessions,
    activeSessionId,
    toggleOpen,
    toggleExpand,
    minimizeChat,
    closeChat,
    sendMessage,
    regenerateMessage,
    newSession,
    selectSession,
    deleteSession
  } = useChatbot();

  return (
    <>
      {!isOpen && <ChatbotLauncher onClick={toggleOpen} />}
      <ChatbotWindow
        isOpen={isOpen}
        isExpanded={isExpanded}
        messages={messages}
        busy={busy}
        error={error}
        aiHealth={aiHealth}
        sessions={sessions}
        activeSessionId={activeSessionId}
        onSend={sendMessage}
        onSelectSuggestion={sendMessage}
        onRegenerate={regenerateMessage}
        onToggleExpand={toggleExpand}
        onMinimize={minimizeChat}
        onClose={closeChat}
        onNewSession={newSession}
        onSelectSession={selectSession}
        onDeleteSession={deleteSession}
      />
    </>
  );
}

export default function FloatingAiAssistant() {
  return (
    <ChatbotProvider>
      <ChatbotRoot />
    </ChatbotProvider>
  );
}
