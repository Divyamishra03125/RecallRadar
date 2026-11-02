import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { assistantApi } from '../../services/api';

const SESSIONS_STORAGE_KEY = 'rr_centralized_chat_sessions_v1';
const ACTIVE_SESSION_STORAGE_KEY = 'rr_centralized_active_session_id_v1';

const INITIAL_WELCOME_MESSAGE = {
  id: 'init-msg-1',
  role: 'assistant',
  content:
    "Hello! I am RecallRadar AI, your intelligent Product Safety & Recall Analysis assistant. I cross-reference active regulatory notices, model numbers, batch codes, and hazard severity directly from your PostgreSQL database.\n\nHow can I help you investigate product safety today?",
  timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
  followUps: [
    'Check if Samsung Galaxy S24 has any recalls',
    'Are there any critical recalls in the database?',
    'Show active recalls',
    'How many recalls are in the database?'
  ],
  products: [],
  recalls: [],
  sources: [],
  warnings: []
};

const createDefaultSession = () => ({
  id: `session-${Date.now()}`,
  title: 'Safety Investigation',
  createdAt: new Date().toISOString(),
  messages: [INITIAL_WELCOME_MESSAGE]
});

const ChatbotContext = createContext(null);

export function ChatbotProvider({ children }) {
  const [isOpen, setIsOpen] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);

  // Sessions state
  const [sessions, setSessions] = useState(() => {
    try {
      const saved = localStorage.getItem(SESSIONS_STORAGE_KEY);
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed) && parsed.length > 0) return parsed;
      }
    } catch {}
    return [createDefaultSession()];
  });

  const [activeSessionId, setActiveSessionId] = useState(() => {
    try {
      const savedId = localStorage.getItem(ACTIVE_SESSION_STORAGE_KEY);
      if (savedId) return savedId;
    } catch {}
    return sessions[0]?.id || `session-${Date.now()}`;
  });

  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [aiHealth, setAiHealth] = useState(null);

  // Persist sessions
  useEffect(() => {
    try {
      localStorage.setItem(SESSIONS_STORAGE_KEY, JSON.stringify(sessions));
    } catch {}
  }, [sessions]);

  // Persist active session ID
  useEffect(() => {
    try {
      localStorage.setItem(ACTIVE_SESSION_STORAGE_KEY, activeSessionId);
    } catch {}
  }, [activeSessionId]);

  // Periodic AI health check
  const checkHealth = useCallback(async () => {
    try {
      const res = await assistantApi.health();
      setAiHealth(res.data);
    } catch (err) {
      setAiHealth({ status: 'DEGRADED', message: err.message });
    }
  }, []);

  useEffect(() => {
    checkHealth();
    const timer = setInterval(checkHealth, 60000);
    return () => clearInterval(timer);
  }, [checkHealth]);

  // Listen for global open events (e.g. from Sidebar promo button)
  useEffect(() => {
    const handleGlobalOpen = () => {
      setIsOpen(true);
    };
    window.addEventListener('recallradar:open-chat', handleGlobalOpen);
    return () => window.removeEventListener('recallradar:open-chat', handleGlobalOpen);
  }, []);

  const currentSession = sessions.find((s) => s.id === activeSessionId) || sessions[0];
  const messages = currentSession?.messages || [INITIAL_WELCOME_MESSAGE];

  const updateCurrentSessionMessages = useCallback(
    (newMessages, titleCandidate = null) => {
      setSessions((prev) =>
        prev.map((s) => {
          if (s.id === activeSessionId) {
            let title = s.title;
            if (titleCandidate && (!title || title === 'Safety Investigation')) {
              title = titleCandidate.slice(0, 36);
            }
            return {
              ...s,
              title,
              messages: newMessages,
              updatedAt: new Date().toISOString()
            };
          }
          return s;
        })
      );
    },
    [activeSessionId]
  );

  const newSession = useCallback(() => {
    const fresh = createDefaultSession();
    setSessions((prev) => [fresh, ...prev]);
    setActiveSessionId(fresh.id);
    setError('');
  }, []);

  const selectSession = useCallback((id) => {
    setActiveSessionId(id);
    setError('');
  }, []);

  const deleteSession = useCallback(
    (id) => {
      setSessions((prev) => {
        const filtered = prev.filter((s) => s.id !== id);
        if (filtered.length === 0) {
          const fresh = createDefaultSession();
          setActiveSessionId(fresh.id);
          return [fresh];
        }
        if (activeSessionId === id) {
          setActiveSessionId(filtered[0].id);
        }
        return filtered;
      });
    },
    [activeSessionId]
  );

  const sendMessage = useCallback(
    async (textToSend) => {
      const q = textToSend.trim();
      if (!q || busy) return;

      const timeStr = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      const userMessage = {
        id: `msg-${Date.now()}`,
        role: 'user',
        content: q,
        timestamp: timeStr
      };

      const updatedHistory = [...messages, userMessage];
      updateCurrentSessionMessages(updatedHistory, q);

      setBusy(true);
      setError('');

      const historyPayload = updatedHistory
        .filter((m) => m.role === 'user' || m.role === 'assistant')
        .map((m) => ({ role: m.role, content: m.content }));

      try {
        const response = await assistantApi.ask(q, historyPayload, activeSessionId);
        const data = response.data || {};

        const assistantMessage = {
          id: `msg-${Date.now() + 1}`,
          role: 'assistant',
          content: data.answer || 'No response from intelligence engine.',
          intent: data.intent,
          matchStatus: data.matchStatus,
          products: Array.isArray(data.products) ? data.products : [],
          recalls: Array.isArray(data.recalls) ? data.recalls : [],
          sources: Array.isArray(data.sources) ? data.sources : [],
          warnings: Array.isArray(data.warnings) ? data.warnings : [],
          followUps: Array.isArray(data.suggestedFollowUps) ? data.suggestedFollowUps : [],
          sourceType: data.sourceType || 'DATABASE_DIRECT',
          missingInformation: Array.isArray(data.missingInformation) ? data.missingInformation : [],
          aiStatus: data.aiStatus || 'ONLINE',
          timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        };

        updateCurrentSessionMessages([...updatedHistory, assistantMessage]);
      } catch (err) {
        setError(err.message || 'Request failed. Check backend connection.');
      } finally {
        setBusy(false);
      }
    },
    [busy, messages, updateCurrentSessionMessages]
  );

  const regenerateMessage = useCallback(() => {
    const lastUserIndex = [...messages].map((m) => m.role).lastIndexOf('user');
    if (lastUserIndex !== -1) {
      const lastUserQuestion = messages[lastUserIndex].content;
      // Retain messages up to the user question
      const truncated = messages.slice(0, lastUserIndex + 1);
      updateCurrentSessionMessages(truncated);
      sendMessage(lastUserQuestion);
    }
  }, [messages, updateCurrentSessionMessages, sendMessage]);

  const value = {
    isOpen,
    isExpanded,
    sessions,
    activeSessionId,
    messages,
    busy,
    error,
    aiHealth,
    openChat: () => setIsOpen(true),
    closeChat: () => {
      setIsOpen(false);
      setIsExpanded(false);
    },
    minimizeChat: () => setIsOpen(false),
    toggleOpen: () => setIsOpen((prev) => !prev),
    toggleExpand: () => setIsExpanded((prev) => !prev),
    sendMessage,
    regenerateMessage,
    newSession,
    selectSession,
    deleteSession
  };

  return <ChatbotContext.Provider value={value}>{children}</ChatbotContext.Provider>;
}

export function useChatbot() {
  const context = useContext(ChatbotContext);
  if (!context) {
    throw new Error('useChatbot must be used within a ChatbotProvider');
  }
  return context;
}
