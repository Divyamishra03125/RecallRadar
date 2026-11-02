import React from 'react';
import { AlertTriangle, ExternalLink, Calendar, ShieldAlert, Link2, Link2Off } from 'lucide-react';
import { normalizeSeverity, SEVERITY_COLORS } from '../../services/api';

const formatDate = (val) => {
  if (!val) return 'Date not available';
  try {
    const d = new Date(val);
    return Number.isNaN(d.getTime()) ? String(val) : d.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  } catch {
    return String(val);
  }
};

export default function RecallCard({ recall }) {
  if (!recall) return null;

  const sev = normalizeSeverity(recall.severity);
  const sevColor = SEVERITY_COLORS[sev] || '#64748b';
  const isActive = String(recall.status || 'ACTIVE').toUpperCase() === 'ACTIVE';

  const isLinked = recall.productId || recall.product?.id || recall.associationStatus === 'LINKED';
  const productName = recall.productName || recall.product?.name || (isLinked ? 'Linked Product' : 'Product not linked');

  return (
    <div
      style={{
        border: '1px solid var(--border)',
        borderLeft: `4px solid ${sevColor}`,
        borderRadius: '10px',
        padding: '12px 14px',
        background: 'var(--panel)',
        display: 'flex',
        flexDirection: 'column',
        gap: '8px',
        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.06)',
        margin: '8px 0',
        fontSize: '0.85rem'
      }}
    >
      {/* Card Header */}
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: '8px' }}>
        <div>
          <span style={{ fontSize: '0.72rem', color: 'var(--muted)', fontWeight: 600 }}>
            RECALL NOTICE #{recall.id}
          </span>
          <h4
            style={{
              margin: '2px 0 0 0',
              fontSize: '0.92rem',
              fontWeight: 600,
              color: 'var(--text)',
              lineHeight: 1.35
            }}
          >
            {recall.title}
          </h4>
        </div>

        {/* Severity Badge */}
        <span
          style={{
            fontSize: '0.72rem',
            padding: '3px 8px',
            borderRadius: '12px',
            background: `${sevColor}22`,
            color: sevColor,
            fontWeight: 700,
            whiteSpace: 'nowrap',
            border: `1px solid ${sevColor}55`
          }}
        >
          {sev}
        </span>
      </div>

      {/* Metadata Row */}
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          gap: '10px',
          fontSize: '0.78rem',
          color: 'var(--muted)',
          padding: '4px 0'
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
          {isLinked ? (
            <Link2 size={13} style={{ color: '#16a34a' }} />
          ) : (
            <Link2Off size={13} style={{ color: 'var(--muted)' }} />
          )}
          <span style={{ color: isLinked ? 'var(--text)' : 'var(--muted)' }}>
            {productName}
          </span>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
          <Calendar size={13} />
          <span>{formatDate(recall.recallDate)}</span>
        </div>

        <span
          style={{
            padding: '1px 6px',
            borderRadius: '4px',
            background: isActive ? '#dcfce7' : '#f1f5f9',
            color: isActive ? '#15803d' : '#64748b',
            fontSize: '0.7rem',
            fontWeight: 600
          }}
        >
          {isActive ? 'ACTIVE' : 'INACTIVE'}
        </span>
      </div>

      {/* Hazard / Reason */}
      {(recall.reason || recall.description) && (
        <div
          style={{
            background: 'var(--bg)',
            padding: '8px 10px',
            borderRadius: '6px',
            border: '1px solid var(--border)',
            fontSize: '0.8rem',
            lineHeight: 1.45,
            color: 'var(--text)'
          }}
        >
          <strong style={{ color: sevColor, marginRight: '4px' }}>Hazard:</strong>
          {recall.reason || recall.description}
        </div>
      )}

      {/* Official Source Link */}
      {(() => {
        const url = recall.sourceUrl;
        const isPlaceholder = !url || url.includes('example.com') || url.includes('localhost') || url.includes('test.com') || url === '#';
        if (isPlaceholder) {
          return (
            <small style={{ fontSize: '0.72rem', color: 'var(--muted)', fontStyle: 'italic' }}>
              Original regulatory source link unavailable
            </small>
          );
        }

        const isVerified =
          recall.sourceVerificationStatus === 'VERIFIED' ||
          url.toLowerCase().includes('cpsc.gov') ||
          url.toLowerCase().includes('fda.gov') ||
          url.toLowerCase().includes('nhtsa.gov') ||
          url.toLowerCase().includes('europa.eu') ||
          url.toLowerCase().includes('recalls.gov') ||
          url.toLowerCase().includes('canada.ca');

        if (isVerified) {
          return (
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginTop: '2px', flexWrap: 'wrap' }}>
              <a
                href={url}
                target="_blank"
                rel="noopener noreferrer"
                style={{
                  fontSize: '0.76rem',
                  color: '#059669',
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '4px',
                  textDecoration: 'none',
                  fontWeight: 600
                }}
              >
                <ExternalLink size={12} />
                View Official Regulatory Notice
              </a>
              <span
                style={{
                  fontSize: '0.66rem',
                  padding: '1px 5px',
                  borderRadius: '10px',
                  background: '#dcfce7',
                  color: '#15803d',
                  fontWeight: 600
                }}
              >
                ✓ Verified Authority
              </span>
            </div>
          );
        }

        return (
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginTop: '2px', flexWrap: 'wrap' }}>
            <a
              href={url}
              target="_blank"
              rel="noopener noreferrer"
              style={{
                fontSize: '0.76rem',
                color: '#d97706',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '4px',
                textDecoration: 'none',
                fontWeight: 500
              }}
            >
              <ExternalLink size={12} />
              View External Reference Link
            </a>
            <span
              style={{
                fontSize: '0.66rem',
                padding: '1px 5px',
                borderRadius: '10px',
                background: '#fef3c7',
                color: '#b45309',
                fontWeight: 600
              }}
            >
              ⚠️ Unverified Source
            </span>
          </div>
        );
      })()}
    </div>
  );
}
