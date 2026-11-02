import React from 'react';
import { Bot, Sparkles } from 'lucide-react';

export default function ChatbotLauncher({ onClick, hasUnread }) {
  return (
    <div
      style={{
        position: 'fixed',
        bottom: '24px',
        right: '24px',
        zIndex: 9990
      }}
    >
      <button
        type="button"
        onClick={onClick}
        aria-label="Open RecallRadar AI Assistant"
        style={{
          width: '58px',
          height: '58px',
          borderRadius: '50%',
          background: 'linear-gradient(135deg, #4f46e5 0%, #06b6d4 100%)',
          color: '#ffffff',
          border: 'none',
          boxShadow: '0 6px 20px rgba(79, 70, 229, 0.45)',
          cursor: 'pointer',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          position: 'relative',
          transition: 'transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.25s ease'
        }}
        onMouseEnter={(e) => {
          e.currentTarget.style.transform = 'scale(1.08) translateY(-2px)';
          e.currentTarget.style.boxShadow = '0 10px 28px rgba(79, 70, 229, 0.6)';
        }}
        onMouseLeave={(e) => {
          e.currentTarget.style.transform = 'scale(1) translateY(0)';
          e.currentTarget.style.boxShadow = '0 6px 20px rgba(79, 70, 229, 0.45)';
        }}
        onMouseDown={(e) => {
          e.currentTarget.style.transform = 'scale(0.96)';
        }}
        onMouseUp={(e) => {
          e.currentTarget.style.transform = 'scale(1.08) translateY(-2px)';
        }}
      >
        <Bot size={28} />

        {/* Small AI Sparkle Indicator */}
        <span
          style={{
            position: 'absolute',
            top: '0',
            right: '0',
            width: '18px',
            height: '18px',
            borderRadius: '50%',
            background: '#10b981',
            border: '2px solid #ffffff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}
          title="Grounded AI Assistant Active"
        >
          <Sparkles size={10} color="#ffffff" />
        </span>
      </button>
    </div>
  );
}
