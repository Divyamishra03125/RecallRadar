import React, { useState } from 'react';
import { Moon, Sun, Server, Info, Activity, CheckCircle2, XCircle, RefreshCw, Database, Cpu } from 'lucide-react';
import { getApiBaseUrl, systemApi, assistantApi } from '../services/api';
import PageHeading from '../components/PageHeading';

export default function Settings({ dark, toggle }) {
  const [testing, setTesting] = useState(false);
  const [apiStatus, setApiStatus] = useState(null); // { online: boolean, latency: number, timestamp: string, error?: string }
  const [aiStatus, setAiStatus] = useState(null);

  const testConnection = async () => {
    setTesting(true);
    const start = performance.now();
    try {
      await systemApi.ping();
      const latency = Math.round(performance.now() - start);
      setApiStatus({
        online: true,
        latency,
        timestamp: new Date().toLocaleTimeString(),
        error: null
      });
    } catch (err) {
      setApiStatus({
        online: false,
        latency: 0,
        timestamp: new Date().toLocaleTimeString(),
        error: err.message || 'Connection refused or backend offline.'
      });
    }

    // Also check AI health
    try {
      const aiRes = await assistantApi.checkHealth();
      setAiStatus({
        status: aiRes.data?.status || 'UP',
        geminiConfigured: aiRes.data?.geminiConfigured ?? true
      });
    } catch {
      setAiStatus({
        status: 'DOWN',
        geminiConfigured: false
      });
    }

    setTesting(false);
  };

  return (
    <>
      <PageHeading
        eyebrow="PREFERENCES & DIAGNOSTICS"
        title="Settings & System Status"
        description="Manage workspace preferences, inspect runtime configuration, and verify backend API connectivity."
      />

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))', gap: '20px' }}>
        {/* Appearance Section */}
        <section className="panel setting">
          <h3>
            <Sun size={18} /> Interface Appearance
          </h3>
          <p>Switch between the enterprise light theme and the high-contrast dark theme.</p>
          <div style={{ marginTop: '12px' }}>
            <button className="secondary" onClick={toggle} type="button">
              {dark ? <Sun size={16} /> : <Moon size={16} />}
              {dark ? 'Switch to light mode' : 'Switch to dark mode'}
            </button>
          </div>
          <small className="muted" style={{ display: 'block', marginTop: '12px' }}>
            Theme preference persists across your browser session.
          </small>
        </section>

        {/* Backend Connectivity Section */}
        <section className="panel setting">
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <h3 style={{ margin: 0 }}>
              <Server size={18} /> Backend API Diagnostics
            </h3>
            <button
              className="secondary"
              onClick={testConnection}
              disabled={testing}
              type="button"
              style={{ fontSize: '0.82rem', padding: '4px 10px' }}
            >
              <RefreshCw size={14} className={testing ? 'spin' : ''} />
              {testing ? 'Testing…' : 'Test API'}
            </button>
          </div>

          <p style={{ marginTop: '8px' }}>Spring Boot REST API base endpoint:</p>
          <code style={{ display: 'block', padding: '6px 10px', background: 'var(--bg)', borderRadius: '4px', fontSize: '0.85rem' }}>
            {getApiBaseUrl()}
          </code>

          {apiStatus && (
            <div
              style={{
                marginTop: '12px',
                padding: '10px 14px',
                borderRadius: '6px',
                background: apiStatus.online ? '#f0fdf4' : '#fef2f2',
                border: `1px solid ${apiStatus.online ? '#bbf7d0' : '#fecaca'}`,
                color: apiStatus.online ? '#15803d' : '#b91c1c'
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontWeight: 600 }}>
                {apiStatus.online ? <CheckCircle2 size={16} /> : <XCircle size={16} />}
                <span>
                  {apiStatus.online
                    ? `Backend Online (Latency: ${apiStatus.latency} ms)`
                    : 'Backend Offline / Unreachable'}
                </span>
              </div>
              {apiStatus.error && (
                <small style={{ display: 'block', marginTop: '4px', color: '#b91c1c' }}>
                  {apiStatus.error}
                </small>
              )}
              <small style={{ display: 'block', marginTop: '4px', color: 'var(--muted)', fontSize: '0.75rem' }}>
                Tested at {apiStatus.timestamp}
              </small>
            </div>
          )}

          {aiStatus && (
            <div
              style={{
                marginTop: '10px',
                padding: '8px 12px',
                borderRadius: '6px',
                background: aiStatus.status === 'UP' ? '#f0fdf4' : '#fffbeb',
                border: `1px solid ${aiStatus.status === 'UP' ? '#bbf7d0' : '#fef3c7'}`,
                color: aiStatus.status === 'UP' ? '#15803d' : '#b45309',
                fontSize: '0.85rem'
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <Cpu size={15} />
                <b>AI Diagnostics:</b> Status {aiStatus.status}
              </div>
            </div>
          )}
        </section>

        {/* Database & Ingestion Section */}
        <section className="panel setting">
          <h3>
            <Database size={18} /> Data Model & Standards
          </h3>
          <p>Compliant with product safety taxonomy and regulatory guidelines:</p>
          <ul style={{ margin: '8px 0 0 18px', padding: 0, fontSize: '0.88rem', lineHeight: 1.6, color: 'var(--muted)' }}>
            <li>Entity Relationships: Products (1) to Recalls (N) foreign key mapping</li>
            <li>Flyway Database Migrations: Versioned schemas & repeatable seeds</li>
            <li>Severity Normalization: Red/Critical, Orange/High, Yellow/Medium, Green/Low, Grey/Unknown</li>
            <li>Source Traceability: CPSC & regulatory notice URL attribution</li>
          </ul>
        </section>

        {/* Architecture Specs */}
        <section className="panel setting">
          <h3>
            <Info size={18} /> Technology Architecture Specs
          </h3>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', marginTop: '10px', fontSize: '0.85rem' }}>
            <div>
              <small className="muted">Frontend Stack</small>
              <p style={{ margin: 0, fontWeight: 600 }}>React 18 · Vite · Recharts</p>
            </div>
            <div>
              <small className="muted">Backend Framework</small>
              <p style={{ margin: 0, fontWeight: 600 }}>Spring Boot 3 · Java 24</p>
            </div>
            <div>
              <small className="muted">Database Engine</small>
              <p style={{ margin: 0, fontWeight: 600 }}>PostgreSQL · Hibernate JPA</p>
            </div>
            <div>
              <small className="muted">AI Intelligence</small>
              <p style={{ margin: 0, fontWeight: 600 }}>Google Gemini 2.5 Flash REST API</p>
            </div>
          </div>
          <small className="muted" style={{ display: 'block', marginTop: '14px', borderTop: '1px solid var(--border)', paddingTop: '8px' }}>
            RecallRadar Enterprise Edition · Version 1.0.0 · Full Stack Platform Verified
          </small>
        </section>
      </div>
    </>
  );
}
