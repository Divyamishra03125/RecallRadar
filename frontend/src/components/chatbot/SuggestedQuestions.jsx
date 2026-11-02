import React from 'react';
import { Sparkles, ArrowRight } from 'lucide-react';

export default function SuggestedQuestions({ suggestions, onSelect, disabled }) {
  if (!suggestions || suggestions.length === 0) return null;

  return (
    <div
      style={{
        display: 'flex',
        flexWrap: 'wrap',
        gap: '6px',
        marginTop: '8px'
      }}
    >
      {suggestions.map((q, idx) => (
        <button
          key={idx}
          type="button"
          onClick={() => onSelect(q)}
          disabled={disabled}
          style={{
            fontSize: '0.74rem',
            padding: '4px 10px',
            borderRadius: '16px',
            border: '1px solid var(--border)',
            background: 'var(--panel)',
            color: 'var(--text)',
            cursor: disabled ? 'not-allowed' : 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '5px',
            transition: 'all 0.15s ease',
            whiteSpace: 'nowrap'
          }}
          onMouseEnter={(e) => {
            if (!disabled) {
              e.currentTarget.style.borderColor = '#6366f1';
              e.currentTarget.style.background = 'rgba(99, 102, 241, 0.08)';
            }
          }}
          onMouseLeave={(e) => {
            if (!disabled) {
              e.currentTarget.style.borderColor = 'var(--border)';
              e.currentTarget.style.background = 'var(--panel)';
            }
          }}
        >
          <Sparkles size={11} style={{ color: '#818cf8' }} />
          <span>{q}</span>
        </button>
      ))}
    </div>
  );
}
