import React, { useState } from 'react';
import { Bot, User, Copy, Check, RotateCcw, AlertCircle, AlertTriangle } from 'lucide-react';
import FormattedContent from './FormattedContent';
import ProductCard from './ProductCard';
import RecallCard from './RecallCard';
import SourceReferences from './SourceReferences';
import SuggestedQuestions from './SuggestedQuestions';

export default function ChatMessage({
  message,
  onSelectSuggestion,
  onRegenerate,
  isLatest,
  busy
}) {
  const isUser = message.role === 'user';
  const [copied, setCopied] = useState(false);

  const copyText = () => {
    if (!message.content) return;
    navigator.clipboard.writeText(message.content);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignSelf: isUser ? 'flex-end' : 'flex-start',
        maxWidth: isUser ? '82%' : '90%',
        gap: '4px',
        animation: 'fadeIn 0.2s ease-out'
      }}
    >
      <div
        style={{
          display: 'flex',
          gap: '8px',
          flexDirection: isUser ? 'row-reverse' : 'row',
          alignItems: 'flex-start'
        }}
      >
        {/* Avatar */}
        <div
          style={{
            width: '28px',
            height: '28px',
            borderRadius: '50%',
            background: isUser
              ? 'linear-gradient(135deg, #1e293b 0%, #0f172a 100%)'
              : 'linear-gradient(135deg, #4f46e5 0%, #06b6d4 100%)',
            color: '#ffffff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
            marginTop: '2px',
            boxShadow: isUser ? 'none' : '0 2px 8px rgba(79, 70, 229, 0.3)'
          }}
        >
          {isUser ? <User size={15} /> : <Bot size={15} />}
        </div>

        {/* Message Bubble */}
        <div
          style={{
            background: isUser ? '#1e293b' : 'var(--panel)',
            color: isUser ? '#ffffff' : 'var(--text)',
            padding: '10px 14px',
            borderRadius: '12px',
            borderTopRightRadius: isUser ? '2px' : '12px',
            borderTopLeftRadius: isUser ? '12px' : '2px',
            border: isUser ? '1px solid #334155' : '1px solid var(--border)',
            boxShadow: '0 2px 6px rgba(0, 0, 0, 0.04)',
            wordBreak: 'break-word',
            fontSize: '0.88rem',
            lineHeight: 1.5
          }}
        >
          {isUser ? (
            <div style={{ whiteSpace: 'pre-wrap' }}>{message.content}</div>
          ) : (
            <div>
              <FormattedContent text={message.content} />

              {/* Structured Product Cards */}
              {Array.isArray(message.products) && message.products.length > 0 && (
                <div style={{ marginTop: '10px' }}>
                  {message.products.map((p, idx) => (
                    <ProductCard key={idx} product={p} />
                  ))}
                </div>
              )}

              {/* Structured Recall Cards */}
              {Array.isArray(message.recalls) && message.recalls.length > 0 && (
                <div style={{ marginTop: '10px' }}>
                  {message.recalls.map((r, idx) => (
                    <RecallCard key={idx} recall={r} />
                  ))}
                </div>
              )}

              {/* Verified Sources */}
              {Array.isArray(message.sources) && message.sources.length > 0 && (
                <SourceReferences sources={message.sources} />
              )}

              {/* Warnings */}
              {Array.isArray(message.warnings) && message.warnings.length > 0 && (
                <div style={{ marginTop: '8px' }}>
                  {message.warnings.map((w, idx) => (
                    <div
                      key={idx}
                      style={{
                        padding: '6px 10px',
                        borderRadius: '6px',
                        background: 'rgba(234, 88, 12, 0.1)',
                        border: '1px solid rgba(234, 88, 12, 0.3)',
                        color: '#ea580c',
                        fontSize: '0.76rem',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '6px',
                        marginBottom: '4px'
                      }}
                    >
                      <AlertTriangle size={12} />
                      <span>{w}</span>
                    </div>
                  ))}
                </div>
              )}

              {/* Missing Information Indicators */}
              {Array.isArray(message.missingInformation) && message.missingInformation.length > 0 && (
                <div
                  style={{
                    marginTop: '6px',
                    padding: '4px 8px',
                    borderRadius: '6px',
                    background: 'rgba(100, 116, 139, 0.08)',
                    border: '1px solid rgba(100, 116, 139, 0.2)',
                    color: 'var(--muted)',
                    fontSize: '0.72rem',
                    display: 'flex',
                    flexWrap: 'wrap',
                    gap: '4px',
                    alignItems: 'center'
                  }}
                >
                  <span style={{ fontWeight: 600 }}>Missing Regulatory Data:</span>
                  {message.missingInformation.slice(0, 3).map((item, idx) => (
                    <span
                      key={idx}
                      style={{
                        background: 'rgba(100, 116, 139, 0.15)',
                        padding: '1px 5px',
                        borderRadius: '3px'
                      }}
                    >
                      {item}
                    </span>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Message Footer Actions */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          alignSelf: isUser ? 'flex-end' : 'flex-start',
          paddingLeft: isUser ? 0 : '36px',
          paddingRight: isUser ? '36px' : 0,
          marginTop: '2px'
        }}
      >
        {message.timestamp && (
          <span style={{ fontSize: '0.68rem', color: 'var(--muted)' }}>
            {message.timestamp}
          </span>
        )}

        {!isUser && message.sourceType && (
          <span
            style={{
              fontSize: '0.66rem',
              fontWeight: 500,
              padding: '1px 6px',
              borderRadius: '4px',
              background:
                message.sourceType === 'DATABASE_DIRECT'
                  ? 'rgba(16, 185, 129, 0.12)'
                  : message.sourceType === 'GEMINI_GROUNDED'
                  ? 'rgba(99, 102, 241, 0.12)'
                  : 'rgba(245, 158, 11, 0.12)',
              color:
                message.sourceType === 'DATABASE_DIRECT'
                  ? '#059669'
                  : message.sourceType === 'GEMINI_GROUNDED'
                  ? '#4f46e5'
                  : '#d97706',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '3px'
            }}
          >
            {message.sourceType === 'DATABASE_DIRECT' && '⚡ DB-Direct'}
            {message.sourceType === 'GEMINI_GROUNDED' && '✨ AI-Grounded'}
            {message.sourceType === 'DEGRADED_DATABASE' && '⚠️ DB-Fallback'}
            {message.sourceType === 'STATIC_KNOWLEDGE' && '📘 Knowledge Base'}
          </span>
        )}

        {!isUser && message.content && (
          <button
            type="button"
            onClick={copyText}
            style={{
              background: 'none',
              border: 'none',
              cursor: 'pointer',
              color: 'var(--muted)',
              padding: '2px 4px',
              display: 'flex',
              alignItems: 'center',
              gap: '3px',
              fontSize: '0.68rem'
            }}
            title="Copy response"
          >
            {copied ? (
              <>
                <Check size={11} style={{ color: '#16a34a' }} />
                <span style={{ color: '#16a34a' }}>Copied</span>
              </>
            ) : (
              <>
                <Copy size={11} />
                <span>Copy</span>
              </>
            )}
          </button>
        )}

        {!isUser && isLatest && onRegenerate && !busy && (
          <button
            type="button"
            onClick={onRegenerate}
            style={{
              background: 'none',
              border: 'none',
              cursor: 'pointer',
              color: 'var(--muted)',
              padding: '2px 4px',
              display: 'flex',
              alignItems: 'center',
              gap: '3px',
              fontSize: '0.68rem'
            }}
            title="Regenerate answer"
          >
            <RotateCcw size={11} />
            <span>Regenerate</span>
          </button>
        )}
      </div>

      {/* Dynamic Suggested Follow-ups */}
      {!isUser && isLatest && Array.isArray(message.followUps) && message.followUps.length > 0 && (
        <div style={{ paddingLeft: '36px', marginTop: '4px' }}>
          <SuggestedQuestions
            suggestions={message.followUps}
            onSelect={onSelectSuggestion}
            disabled={busy}
          />
        </div>
      )}
    </div>
  );
}
