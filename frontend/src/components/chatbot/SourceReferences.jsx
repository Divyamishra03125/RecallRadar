import React from 'react';
import { ExternalLink, ShieldCheck, AlertCircle, ShieldAlert } from 'lucide-react';

const isVerifiedAuthorityUrl = (url) => {
  if (!url) return false;
  const lower = url.toLowerCase();
  return (
    lower.includes('cpsc.gov') ||
    lower.includes('fda.gov') ||
    lower.includes('nhtsa.gov') ||
    lower.includes('europa.eu') ||
    lower.includes('recalls.gov') ||
    lower.includes('productsafety.gov.au') ||
    lower.includes('canada.ca')
  );
};

const isPlaceholderUrl = (url) => {
  if (!url) return true;
  const lower = url.toLowerCase();
  return (
    lower.includes('example.com') ||
    lower.includes('localhost') ||
    lower.includes('test.com') ||
    lower === '#'
  );
};

export default function SourceReferences({ sources }) {
  if (!sources || sources.length === 0) return null;

  const evaluatedSources = sources.map((src, idx) => {
    const url = typeof src === 'string' ? src : src.sourceUrl || src.url;
    const label = (typeof src === 'object' && src.title) || `Notice Reference #${idx + 1}`;
    const explicitStatus = typeof src === 'object' ? src.verificationStatus : null;

    let status = 'UNAVAILABLE';
    if (!url || isPlaceholderUrl(url)) {
      status = 'UNAVAILABLE';
    } else if (explicitStatus === 'VERIFIED' || isVerifiedAuthorityUrl(url)) {
      status = 'VERIFIED';
    } else {
      status = 'AVAILABLE_UNVERIFIED';
    }

    return { url, label, status };
  });

  const hasVerified = evaluatedSources.some((s) => s.status === 'VERIFIED');
  const hasUnverified = evaluatedSources.some((s) => s.status === 'AVAILABLE_UNVERIFIED');

  return (
    <div
      style={{
        marginTop: '8px',
        padding: '10px 14px',
        borderRadius: '8px',
        background: 'var(--panel)',
        border: '1px solid var(--border)',
        fontSize: '0.78rem'
      }}
    >
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '6px',
          fontWeight: 600,
          color: hasVerified ? '#10b981' : hasUnverified ? '#f59e0b' : 'var(--muted)',
          marginBottom: '8px',
          textTransform: 'uppercase',
          letterSpacing: '0.04em',
          fontSize: '0.7rem'
        }}
      >
        {hasVerified ? (
          <>
            <ShieldCheck size={14} style={{ color: '#10b981' }} />
            <span>Verified Regulatory Sources (Official Authorities)</span>
          </>
        ) : hasUnverified ? (
          <>
            <AlertCircle size={14} style={{ color: '#f59e0b' }} />
            <span>External Source References (Unverified Portal)</span>
          </>
        ) : (
          <>
            <ShieldAlert size={14} style={{ color: '#94a3b8' }} />
            <span>Source Reference Status: Unavailable</span>
          </>
        )}
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
        {evaluatedSources.map((item, idx) => {
          if (item.status === 'UNAVAILABLE') {
            return (
              <div
                key={idx}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  color: 'var(--muted)',
                  fontSize: '0.76rem',
                  fontStyle: 'italic',
                  padding: '2px 0'
                }}
              >
                <AlertCircle size={12} style={{ color: '#94a3b8' }} />
                <span>Original source reference unavailable in database (no verified link)</span>
              </div>
            );
          }

          if (item.status === 'VERIFIED') {
            return (
              <div key={idx} style={{ display: 'flex', alignItems: 'center', gap: '6px', flexWrap: 'wrap' }}>
                <a
                  href={item.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  style={{
                    color: '#059669',
                    textDecoration: 'none',
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '4px',
                    fontWeight: 600
                  }}
                >
                  <ExternalLink size={12} />
                  <span>{item.label}</span>
                </a>
                <span
                  style={{
                    fontSize: '0.68rem',
                    padding: '1px 6px',
                    borderRadius: '10px',
                    background: '#dcfce7',
                    color: '#15803d',
                    fontWeight: 600
                  }}
                >
                  ✓ Verified Regulatory Authority
                </span>
              </div>
            );
          }

          // AVAILABLE_UNVERIFIED
          return (
            <div key={idx} style={{ display: 'flex', alignItems: 'center', gap: '6px', flexWrap: 'wrap' }}>
              <a
                href={item.url}
                target="_blank"
                rel="noopener noreferrer"
                style={{
                  color: '#d97706',
                  textDecoration: 'none',
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '4px',
                  fontWeight: 500
                }}
              >
                <ExternalLink size={12} />
                <span>{item.label}</span>
              </a>
              <span
                style={{
                  fontSize: '0.68rem',
                  padding: '1px 6px',
                  borderRadius: '10px',
                  background: '#fef3c7',
                  color: '#b45309',
                  fontWeight: 600
                }}
              >
                ⚠️ External URL (Unverified Source)
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
