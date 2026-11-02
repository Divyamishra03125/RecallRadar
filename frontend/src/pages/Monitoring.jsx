import React, { useEffect, useMemo, useState } from 'react';
import { Eye, Search, RefreshCw, AlertTriangle, ShieldCheck, ExternalLink } from 'lucide-react';
import {
  recallApi,
  productApi,
  unwrapList,
  normalizeSeverity,
  resolveProductName,
  resolveProductEntity
} from '../services/api';
import PageHeading from '../components/PageHeading';
import DataTable from '../components/DataTable';
import Severity from '../components/Severity';
import StatCard from '../components/StatCard';

const formatDate = (val) => {
  if (!val) return 'Date unavailable';
  try {
    const d = new Date(val);
    return Number.isNaN(d.getTime()) ? 'Date unavailable' : d.toLocaleDateString('en-IN');
  } catch {
    return 'Date unavailable';
  }
};

export default function Monitoring() {
  const [r, setR] = useState([]);
  const [p, setP] = useState([]);
  const [busy, setBusy] = useState(true);
  const [error, setError] = useState('');
  const [q, setQ] = useState('');
  const [sevFilter, setSevFilter] = useState('ALL');
  const [viewItem, setViewItem] = useState(null);

  const loadData = () => {
    setBusy(true);
    setError('');
    Promise.all([
      recallApi.getAll({ page: 0, size: 100 }),
      productApi.getAll({ page: 0, size: 100 })
    ])
      .then(([recallRes, prodRes]) => {
        setR(unwrapList(recallRes.data));
        setP(unwrapList(prodRes.data));
      })
      .catch((e) => setError(e.message || 'Failed to load monitoring watchlist.'))
      .finally(() => setBusy(false));
  };

  useEffect(() => {
    loadData();
  }, []);

  const activeRecalls = useMemo(() => {
    return r.filter((x) => String(x.status || 'ACTIVE').toUpperCase() === 'ACTIVE');
  }, [r]);

  const filtered = useMemo(() => {
    return activeRecalls.filter((x) => {
      const prodName = resolveProductName(x, p);
      const entity = resolveProductEntity(x, p);
      const normSev = normalizeSeverity(x.severity);

      const target = [
        x.id,
        x.sourceRecordId,
        x.title,
        x.reason,
        x.description,
        prodName,
        entity?.brand,
        entity?.modelNumber
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();

      const matchesQuery = !q.trim() || target.includes(q.toLowerCase().trim());
      const matchesSev = sevFilter === 'ALL' || normSev === sevFilter;

      return matchesQuery && matchesSev;
    });
  }, [activeRecalls, p, q, sevFilter]);

  const criticalAlerts = useMemo(
    () => activeRecalls.filter((x) => normalizeSeverity(x.severity) === 'CRITICAL'),
    [activeRecalls]
  );

  const highRiskRecalls = useMemo(
    () => activeRecalls.filter((x) => normalizeSeverity(x.severity) === 'HIGH'),
    [activeRecalls]
  );

  const affectedProductsCount = useMemo(() => {
    const ids = activeRecalls
      .map((x) => x.product?.id ?? x.productId ?? x.product_id)
      .filter((id) => id != null && id !== '');
    return new Set(ids).size;
  }, [activeRecalls]);

  const cols = [
    {
      key: 'id',
      label: 'ID',
      className: 'nowrap',
      render: (x) => (
        <div>
          <b>#{x.id}</b>
          {x.sourceRecordId && (
            <small style={{ display: 'block', color: 'var(--muted)', fontSize: '0.72rem' }}>
              {x.sourceRecordId}
            </small>
          )}
        </div>
      )
    },
    {
      key: 'product',
      label: 'PRODUCT',
      render: (x) => {
        const name = resolveProductName(x, p);
        const entity = resolveProductEntity(x, p);
        return (
          <div>
            <b>{name}</b>
            {entity?.brand && (
              <small style={{ display: 'block', color: 'var(--muted)' }}>
                {entity.brand} {entity.modelNumber ? `· ${entity.modelNumber}` : ''}
              </small>
            )}
          </div>
        );
      }
    },
    {
      key: 'title',
      label: 'RECALL NOTICE',
      className: 'cell-title',
      render: (x) => (
        <div>
          <b>{x.title || 'Untitled recall'}</b>
          {x.reason && <small>{x.reason}</small>}
        </div>
      )
    },
    {
      key: 'severity',
      label: 'SEVERITY',
      className: 'nowrap',
      render: (x) => <Severity value={x.severity} />
    },
    {
      key: 'recallDate',
      label: 'ISSUED DATE',
      className: 'nowrap',
      render: (x) => formatDate(x.recallDate)
    },
    {
      key: 'status',
      label: 'ENFORCEMENT',
      className: 'nowrap',
      render: (x) => (
        <span className="status-pill active" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
          <AlertTriangle size={12} />
          {x.status || 'ACTIVE'}
        </span>
      )
    },
    {
      key: 'actions',
      label: 'ACTION',
      className: 'nowrap',
      render: (x) => (
        <button
          className="icon"
          onClick={() => setViewItem(x)}
          title="Inspect Recall Notice"
          type="button"
        >
          <Eye size={16} />
        </button>
      )
    }
  ];

  return (
    <>
      <PageHeading
        eyebrow="ACTIVE SURVEILLANCE"
        title="Recall Monitoring"
        description="Continuously monitor active recall notices, affected inventory, and critical compliance alerts."
      />

      {error && <p className="error">{error}</p>}

      <div className="stats four">
        <StatCard
          label="Active recalls"
          value={busy ? '…' : activeRecalls.length}
          note="Under enforcement"
        />
        <StatCard
          label="Critical alerts"
          value={busy ? '…' : criticalAlerts.length}
          note="Priority red alerts"
        />
        <StatCard
          label="High risk notices"
          value={busy ? '…' : highRiskRecalls.length}
          note="Priority orange notices"
        />
        <StatCard
          label="Catalogue products at risk"
          value={busy ? '…' : affectedProductsCount}
          note="Linked distinct products"
        />
      </div>

      <section className="panel">
        <div className="toolbar" style={{ flexWrap: 'wrap', gap: '12px' }}>
          <div>
            <h3>Active recall watchlist ({filtered.length})</h3>
            <p>Active enforcement notices that require regulatory and customer follow-up.</p>
          </div>
          <div className="tools" style={{ flexWrap: 'wrap', gap: '8px' }}>
            <label className="search" style={{ minWidth: '220px' }}>
              <Search size={16} />
              <input
                value={q}
                onChange={(e) => setQ(e.target.value)}
                placeholder="Search watchlist by product, hazard, model…"
              />
            </label>
            <select value={sevFilter} onChange={(e) => setSevFilter(e.target.value)}>
              <option value="ALL">All severity tiers</option>
              {['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'UNKNOWN'].map((tier) => (
                <option key={tier} value={tier}>
                  {tier}
                </option>
              ))}
            </select>
            <button className="secondary" onClick={loadData} type="button" title="Refresh">
              <RefreshCw size={15} /> Refresh
            </button>
          </div>
        </div>

        <DataTable
          columns={cols}
          rows={filtered}
          loading={busy}
          error=""
          emptyMessage="No active recalls currently match the selected criteria."
        />
      </section>

      {/* Modal View */}
      {viewItem && (
        <div
          className="backdrop"
          onMouseDown={(e) => e.target === e.currentTarget && setViewItem(null)}
        >
          <div className="modal" style={{ maxWidth: '640px' }}>
            <button className="closemodal" onClick={() => setViewItem(null)} type="button">
              ×
            </button>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
              <span className="product-id-tag">Watchlist Notice #{viewItem.id}</span>
              {viewItem.sourceRecordId && (
                <span className="product-id-tag" style={{ background: 'var(--panel)' }}>
                  Ref: {viewItem.sourceRecordId}
                </span>
              )}
            </div>

            <h2 style={{ marginBottom: '12px' }}>{viewItem.title}</h2>

            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                flexWrap: 'wrap',
                gap: '12px',
                padding: '10px 14px',
                background: 'var(--bg)',
                borderRadius: '8px',
                marginBottom: '16px'
              }}
            >
              <div>
                <small className="muted" style={{ display: 'block' }}>Severity</small>
                <Severity value={viewItem.severity} />
              </div>
              <div style={{ borderLeft: '1px solid var(--border)', paddingLeft: '12px' }}>
                <small className="muted" style={{ display: 'block' }}>Enforcement</small>
                <span className="status-pill active">{viewItem.status || 'ACTIVE'}</span>
              </div>
              <div style={{ borderLeft: '1px solid var(--border)', paddingLeft: '12px' }}>
                <small className="muted" style={{ display: 'block' }}>Notice Date</small>
                <strong>{formatDate(viewItem.recallDate)}</strong>
              </div>
            </div>

            {/* Linked Product Card */}
            <div
              style={{
                border: '1px solid var(--border)',
                borderRadius: '8px',
                padding: '12px 14px',
                marginBottom: '16px'
              }}
            >
              <h4 style={{ margin: '0 0 6px 0', fontSize: '0.85rem', color: 'var(--muted)', textTransform: 'uppercase' }}>
                Affected Catalogue Product
              </h4>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '8px' }}>
                <div>
                  <small className="muted">Product Name</small>
                  <p style={{ margin: 0, fontWeight: 600 }}>{resolveProductName(viewItem, p)}</p>
                </div>
                {(() => {
                  const ent = resolveProductEntity(viewItem, p);
                  if (!ent) return null;
                  return (
                    <>
                      <div>
                        <small className="muted">Brand / Category</small>
                        <p style={{ margin: 0 }}>{ent.brand || '—'} / {ent.category || '—'}</p>
                      </div>
                      <div>
                        <small className="muted">Model & Batch</small>
                        <p style={{ margin: 0 }}>Model: {ent.modelNumber || '—'} | Batch: {ent.batchNumber || '—'}</p>
                      </div>
                    </>
                  );
                })()}
              </div>
            </div>

            {/* Hazard / Reason */}
            {viewItem.reason && (
              <div style={{ marginBottom: '14px' }}>
                <h4 style={{ margin: '0 0 4px 0', fontSize: '0.85rem', color: 'var(--muted)', textTransform: 'uppercase' }}>
                  Identified Hazard & Root Cause
                </h4>
                <p
                  style={{
                    margin: 0,
                    padding: '8px 12px',
                    background: '#fff7ed',
                    borderLeft: '3px solid #ea580c',
                    borderRadius: '4px',
                    color: '#9a3412',
                    fontSize: '0.9rem'
                  }}
                >
                  {viewItem.reason}
                </p>
              </div>
            )}

            {/* Description / Remedy */}
            <div style={{ marginBottom: '14px' }}>
              <h4 style={{ margin: '0 0 4px 0', fontSize: '0.85rem', color: 'var(--muted)', textTransform: 'uppercase' }}>
                Notice Description & Enforcement Action
              </h4>
              <p style={{ margin: 0, lineHeight: 1.5, fontSize: '0.92rem' }}>
                {viewItem.description || 'No detailed instructions recorded.'}
              </p>
            </div>

            {viewItem.sourceUrl && (
              <div style={{ marginBottom: '16px' }}>
                <a
                  href={viewItem.sourceUrl}
                  target="_blank"
                  rel="noreferrer"
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '6px',
                    color: '#536fe8',
                    fontWeight: 600,
                    textDecoration: 'none'
                  }}
                >
                  <ExternalLink size={16} /> Open official agency source notice
                </a>
              </div>
            )}

            <div className="actions">
              <button className="primary" onClick={() => setViewItem(null)} type="button">
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
