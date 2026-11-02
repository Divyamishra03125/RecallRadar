import React, { useState, useEffect, useRef } from 'react';
import { Send, Mic, MicOff, Languages, Globe } from 'lucide-react';

export default function ChatInput({ onSend, busy, placeholder = 'Ask RecallRadar AI about products, models, batches, or recalls…' }) {
  const [text, setText] = useState('');
  const [listening, setListening] = useState(false);
  const [lang, setLang] = useState('en-US'); // 'en-US' or 'hi-IN'
  const [speechSupported, setSpeechSupported] = useState(false);
  const recognitionRef = useRef(null);
  const inputRef = useRef(null);

  // Check Web Speech API support
  useEffect(() => {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (SpeechRecognition) {
      setSpeechSupported(true);
      const recognizer = new SpeechRecognition();
      recognizer.continuous = false;
      recognizer.interimResults = true;
      recognizer.lang = lang;

      recognizer.onresult = (event) => {
        let transcript = '';
        for (let i = event.resultIndex; i < event.results.length; i++) {
          transcript += event.results[i][0].transcript;
        }
        setText(transcript);
      };

      recognizer.onerror = () => {
        setListening(false);
      };

      recognizer.onend = () => {
        setListening(false);
      };

      recognitionRef.current = recognizer;
    }
  }, [lang]);

  const toggleListen = () => {
    if (!speechSupported || !recognitionRef.current) return;
    if (listening) {
      recognitionRef.current.stop();
      setListening(false);
    } else {
      try {
        recognitionRef.current.lang = lang;
        recognitionRef.current.start();
        setListening(true);
      } catch {
        setListening(false);
      }
    }
  };

  const submit = (e) => {
    if (e) e.preventDefault();
    if (!text.trim() || busy) return;
    onSend(text.trim());
    setText('');
    if (listening && recognitionRef.current) {
      recognitionRef.current.stop();
      setListening(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      submit();
    }
  };

  return (
    <div
      style={{
        padding: '10px 14px',
        background: 'var(--panel)',
        borderTop: '1px solid var(--border)'
      }}
    >
      <form
        onSubmit={submit}
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          background: 'var(--bg)',
          borderRadius: '10px',
          border: listening ? '1px solid #ef4444' : '1px solid var(--border)',
          padding: '4px 8px',
          boxShadow: listening ? '0 0 10px rgba(239, 68, 68, 0.3)' : 'none',
          transition: 'all 0.2s ease'
        }}
      >
        <input
          ref={inputRef}
          type="text"
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={handleKeyDown}
          disabled={busy}
          placeholder={listening ? 'Listening… speak now' : placeholder}
          style={{
            flex: 1,
            border: 'none',
            outline: 'none',
            background: 'transparent',
            padding: '8px',
            fontSize: '0.88rem',
            color: 'var(--text)',
            fontFamily: 'inherit'
          }}
        />

        {/* Language selector toggle if speech supported */}
        {speechSupported && (
          <button
            type="button"
            onClick={() => setLang((prev) => (prev === 'en-US' ? 'hi-IN' : 'en-US'))}
            style={{
              background: 'none',
              border: 'none',
              cursor: 'pointer',
              color: 'var(--muted)',
              fontSize: '0.72rem',
              fontWeight: 600,
              padding: '4px',
              borderRadius: '4px',
              display: 'flex',
              alignItems: 'center',
              gap: '2px'
            }}
            title={`Speech Language: ${lang === 'en-US' ? 'English (click for Hindi)' : 'Hindi (click for English)'}`}
          >
            <Globe size={13} />
            <span>{lang === 'en-US' ? 'EN' : 'HI'}</span>
          </button>
        )}

        {/* Microphone Button */}
        {speechSupported && (
          <button
            type="button"
            onClick={toggleListen}
            disabled={busy}
            style={{
              background: listening ? '#ef4444' : 'transparent',
              color: listening ? '#ffffff' : 'var(--muted)',
              border: 'none',
              borderRadius: '50%',
              width: '30px',
              height: '30px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              transition: 'all 0.2s ease'
            }}
            title={listening ? 'Stop voice listening' : 'Start voice input (Speech to Text)'}
          >
            {listening ? <MicOff size={15} /> : <Mic size={15} />}
          </button>
        )}

        {/* Send Button */}
        <button
          type="submit"
          disabled={busy || !text.trim()}
          style={{
            background: text.trim() && !busy ? '#4f46e5' : 'var(--panel)',
            color: text.trim() && !busy ? '#ffffff' : 'var(--muted)',
            border: 'none',
            borderRadius: '8px',
            width: '32px',
            height: '32px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: text.trim() && !busy ? 'pointer' : 'not-allowed',
            transition: 'background 0.2s ease, transform 0.1s ease'
          }}
          title="Send query"
        >
          <Send size={14} />
        </button>
      </form>
    </div>
  );
}
