import React, { useEffect, useMemo, useState } from 'react';
import {
  Plus,
  Search,
  RefreshCw,
  Eye,
  Pencil,
  Trash2,
  Calendar,
  ExternalLink,
  FilterX,
  ShieldAlert,
  History,
  Link,
  CheckCircle,
  AlertTriangle
} from 'lucide-react';
import {
  recallApi,
  productApi,
  unwrapList,
  normalizeSeverity,
  resolveProductName,
  resolveProductEntity
} from '../services/api';
import { useAuth } from '../context/AuthContext';
import PageHeading from '../components/PageHeading';
import DataTable from '../components/DataTable';
import Severity from '../components/Severity';

const formatDate = (val) => {
  if (!val) return 'Date unavailable';
  try {
    const d = new Date(val);
    return Number.isNaN(d.getTime()) ? 'Date unavailable' : d.toLocaleDateString('en-IN');
  } catch {
    return 'Date unavailable';
  }
};

export default function Recalls() {
  const [rows, setRows] = useState([]);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const { user: currentUser, isAdmin, isAnalyst, role } = useAuth();
  const canEditRecalls = isAdmin || isAnalyst;

  // Filters
  const [q, setQ] = useState('');
  const [sev, setSev] = useState('ALL');
  const [status, setStatus] = useState('ALL');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');

  // Pagination
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // Modals
  const [openCreate, setOpenCreate] = useState(false);
  const [editItem, setEditItem] = useState(null);
  const [viewItem, setViewItem] = useState(null);
  const [saving, setSaving] = useState(false);
  const [modalError, setModalError] = useState('');

  // Severity Review Modal State
  const [severityModalItem, setSeverityModalItem] = useState(null);
  const [selectedSeverity, setSelectedSeverity] = useState('MEDIUM');
  const [severityReason, setSeverityReason] = useState('');
  const [severityEvidence, setSeverityEvidence] = useState('');
  const [severitySource, setSeveritySource] = useState('CPSC Official Notice');
  const [severityUser, setSeverityUser] = useState('admin');
  const [savingSeverity, setSavingSeverity] = useState(false);

  // Severity Audit History Modal State
  const [auditModalItem, setAuditModalItem] = useState(null);
  const [auditLogs, setAuditLogs] = useState([]);
  const [loadingAudit, setLoadingAudit] = useState(false);

  // Candidate Match Modal State
  const [candidateModalItem, setCandidateModalItem] = useState(null);
  const [candidateMatches, setCandidateMatches] = useState([]);
  const [loadingCandidates, setLoadingCandidates] = useState(false);
  const [associating, setAssociating] = useState(false);
  const [adminLinkConfirmed, setAdminLinkConfirmed] = useState(false);
  const [manualSelectedProductId, setManualSelectedProductId] = useState('');

  const openSeverityModal = (item) => {
    setSeverityModalItem(item);
    setSelectedSeverity(item.severity || 'MEDIUM');
    setSeverityReason(item.reason || '');
    setSeverityEvidence(item.severityEvidence || '');
    setSeveritySource(item.severityClassificationSource || 'CPSC Official Notice');
    setSeverityUser(currentUser?.username || 'admin');
  };

  const handleUpdateSeverity = async (e) => {
    e.preventDefault();
    if (!severityModalItem) return;

    if (selectedSeverity === 'CRITICAL' || selectedSeverity === 'HIGH') {
      if (!severityEvidence.trim() && !severityReason.trim()) {
        setError('Assigning HIGH or CRITICAL severity requires documented evidence or reliable justification.');
        return;
      }
    } else if (!severityReason.trim()) {
      setError('Please provide a reason or justification for the severity update.');
      return;
    }

    setSavingSeverity(true);
    setError('');
    try {
      await recallApi.updateSeverity(severityModalItem.id, {
        severity: selectedSeverity,
        reason: severityReason.trim(),
        evidence: severityEvidence.trim(),
        classificationSource: severitySource.trim(),
        changedBy: severityUser.trim() || 'admin'
      });
      setNotice(`Severity for Recall #${severityModalItem.id} updated to ${selectedSeverity} with documented evidence.`);
      setSeverityModalItem(null);
      await loadData();
    } catch (err) {
      setError(err.message || 'Failed to update severity.');
    } finally {
      setSavingSeverity(false);
    }
  };

  const openAuditModal = async (item) => {
    setAuditModalItem(item);
    setLoadingAudit(true);
    try {
      const res = await recallApi.getSeverityAudit(item.id);
      setAuditLogs(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      setError(err.message || 'Failed to fetch severity audit history.');
    } finally {
      setLoadingAudit(false);
    }
  };

  const openCandidateModal = async (item) => {
    setCandidateModalItem(item);
    setAdminLinkConfirmed(false);
    setManualSelectedProductId('');
    setLoadingCandidates(true);
    try {
      const res = await recallApi.getCandidateMatches(item.id);
      setCandidateMatches(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      setError(err.message || 'Failed to fetch candidate matches.');
    } finally {
      setLoadingCandidates(false);
    }
  };

  const handleAssociateProduct = async (recallId, productId, status = 'LINKED') => {
    if (status === 'LINKED') {
      if (!isAdmin) {
        setError('Administrator confirmation is required to link recall records.');
        return;
      }
      if (!adminLinkConfirmed) {
        setError('Please check the Administrator Confirmation box before linking.');
        return;
      }
    }

    setAssociating(true);
    setError('');
    try {
      await recallApi.associateProduct(recallId, {
        productId,
        associationStatus: status
      });
      setNotice(`Recall #${recallId} product association confirmed and updated to ${status}.`);
      setCandidateModalItem(null);
      setAdminLinkConfirmed(false);
      setManualSelectedProductId('');
      await loadData();
    } catch (err) {
      setError(err.message || 'Failed to associate product.');
    } finally {
      setAssociating(false);
    }
  };

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const [rRes, pRes] = await Promise.all([
        recallApi.getAll({ page: 0, size: 100 }),
        productApi.getAll({ page: 0, size: 100 })
      ]);
      setRows(unwrapList(rRes.data));
      setProducts(unwrapList(pRes.data));
    } catch (e) {
      setError(e.message || 'Failed to load recall records.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleResetFilters = () => {
    setQ('');
    setSev('ALL');
    setStatus('ALL');
    setFromDate('');
    setToDate('');
    setPage(1);
  };

  const filtered = useMemo(() => {
    return rows.filter((r) => {
      const resolvedName = resolveProductName(r, products);
      const prodEntity = resolveProductEntity(r, products);
      const normSev = normalizeSeverity(r.severity);
      const normStatus = String(r.status || 'ACTIVE').toUpperCase();

      const searchTarget = [
        r.id,
        r.sourceRecordId,
        r.title,
        r.reason,
        r.description,
        resolvedName,
        prodEntity?.brand,
        prodEntity?.modelNumber,
        prodEntity?.batchNumber
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();

      const matchesQuery = !q.trim() || searchTarget.includes(q.toLowerCase().trim());
      const matchesSev = sev === 'ALL' || normSev === sev;
      const matchesStatus = status === 'ALL' || normStatus === status;

      let matchesDate = true;
      if (r.recallDate) {
        const dateStr = String(r.recallDate).substring(0, 10);
        if (fromDate && dateStr < fromDate) matchesDate = false;
        if (toDate && dateStr > toDate) matchesDate = false;
      } else if (fromDate || toDate) {
        matchesDate = false;
      }

      return matchesQuery && matchesSev && matchesStatus && matchesDate;
    });
  }, [rows, products, q, sev, status, fromDate, toDate]);

  // Adjust current page if filter shrinks data
  useEffect(() => {
    setPage(1);
  }, [q, sev, status, fromDate, toDate, pageSize]);

  // Paginated records
  const paginatedRows = useMemo(() => {
    if (pageSize === 0) return filtered;
    const startIndex = (page - 1) * pageSize;
    return filtered.slice(startIndex, startIndex + pageSize);
  }, [filtered, page, pageSize]);

  const totalPages = pageSize === 0 ? 1 : Math.ceil(filtered.length / pageSize) || 1;

  const handleSave = async (e) => {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    const prodVal = f.get('productId');

    const sourceUrl = (f.get('sourceUrl') || '').toString().trim();
    if (!sourceUrl) {
      setModalError('Source URL is required.');
      return;
    }

    const payload = {
      sourceRecordId: (f.get('sourceRecordId') || '').toString().trim() || null,
      productId: prodVal ? Number(prodVal) : null,
      title: (f.get('title') || '').toString().trim(),
      description: (f.get('description') || '').toString().trim(),
      reason: (f.get('reason') || '').toString().trim(),
      severity: (f.get('severity') || 'MEDIUM').toString().trim(),
      recallDate: f.get('recallDate') || null,
      sourceUrl,
      status: (f.get('status') || 'ACTIVE').toString().trim()
    };

    setSaving(true);
    setModalError('');
    setError('');
    setNotice('');
    try {
      if (editItem) {
        await recallApi.update(editItem.id, payload);
        setNotice(`Recall #${editItem.id} updated successfully.`);
        setEditItem(null);
      } else {
        const created = await recallApi.create(payload);
        const newId = created.data?.id || '';
        setNotice(`New recall notice ${newId ? '#' + newId : ''} created successfully.`);
        setOpenCreate(false);
      }
      await loadData();
    } catch (err) {
      setModalError(err.message || 'Operation failed.');
      setError(err.message || 'Operation failed.');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (recall) => {
    if (!window.confirm(`Are you sure you want to delete recall notice #${recall.id}: "${recall.title}"?`)) {
      return;
    }
    setError('');
    setNotice('');
    try {
      await recallApi.remove(recall.id);
      setNotice(`Recall #${recall.id} was deleted successfully.`);
      await loadData();
    } catch (err) {
      setError(err.message || 'Failed to delete recall.');
    }
  };

  const cols = [
    {
      key: 'id',
      label: 'ID',
      className: 'nowrap',
      render: (r) => (
        <div>
          <b>#{r.id}</b>
          {r.sourceRecordId && (
            <small style={{ display: 'block', color: 'var(--muted)', fontSize: '0.75rem' }}>
              {r.sourceRecordId}
            </small>
          )}
        </div>
      )
    },
    {
      key: 'title',
      label: 'RECALL TITLE',
      className: 'cell-title',
      render: (r) => (
        <div>
          <b>{r.title || 'Untitled recall'}</b>
          {r.reason && <small>{r.reason}</small>}
        </div>
      )
    },
    {
      key: 'product',
      label: 'PRODUCT & LINK',
      render: (r) => {
        const name = resolveProductName(r, products);
        const entity = resolveProductEntity(r, products);
        const assocStatus = r.associationStatus || (entity ? 'LINKED' : 'UNLINKED');
        const assocBadgeStyle =
          assocStatus === 'LINKED'
            ? { background: '#dcfce7', color: '#16a34a' }
            : assocStatus === 'PENDING_REVIEW'
            ? { background: '#fef9c3', color: '#ca8a04' }
            : { background: '#f1f5f9', color: '#64748b' };

        return (
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', flexWrap: 'wrap' }}>
              <span style={{ fontWeight: 600 }}>{name}</span>
              <span
                style={{
                  fontSize: '0.68rem',
                  padding: '1px 6px',
                  borderRadius: '10px',
                  fontWeight: 600,
                  ...assocBadgeStyle
                }}
              >
                {assocStatus}
              </span>
            </div>
            {entity?.brand && (
              <small style={{ display: 'block', color: 'var(--muted)' }}>
                {entity.brand} {entity.modelNumber ? `· ${entity.modelNumber}` : ''}
              </small>
            )}
            <div style={{ marginTop: '4px' }}>
              <button
                type="button"
                onClick={() => openCandidateModal(r)}
                style={{
                  fontSize: '0.72rem',
                  padding: '2px 8px',
                  borderRadius: '4px',
                  border: '1px solid var(--border)',
                  background: 'var(--panel)',
                  cursor: 'pointer',
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '4px'
                }}
                title="Search and verify candidate catalogue products"
              >
                <Link size={12} /> {assocStatus === 'LINKED' ? 'Change Link' : 'Match Product'}
              </button>
            </div>
          </div>
        );
      }
    },
    {
      key: 'severity',
      label: 'SEVERITY & AUDIT',
      className: 'nowrap',
      render: (r) => (
        <div>
          <Severity value={r.severity} />
          <div style={{ display: 'flex', gap: '4px', marginTop: '4px' }}>
            {canEditRecalls && (
              <button
                type="button"
                onClick={() => openSeverityModal(r)}
                style={{
                  fontSize: '0.72rem',
                  padding: '2px 6px',
                  borderRadius: '4px',
                  border: '1px solid var(--border)',
                  background: 'var(--panel)',
                  cursor: 'pointer',
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '3px'
                }}
                title="Review & update severity with audit log"
              >
                <ShieldAlert size={11} /> Review
              </button>
            )}
            <button
              type="button"
              onClick={() => openAuditModal(r)}
              style={{
                fontSize: '0.72rem',
                padding: '2px 6px',
                borderRadius: '4px',
                border: '1px solid var(--border)',
                background: 'var(--panel)',
                cursor: 'pointer',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '3px'
              }}
              title="View severity audit history"
            >
              <History size={11} /> History
            </button>
          </div>
        </div>
      )
    },
    {
      key: 'recallDate',
      label: 'DATE',
      className: 'nowrap',
      render: (r) => formatDate(r.recallDate)
    },
    {
      key: 'status',
      label: 'STATUS',
      className: 'nowrap',
      render: (r) => (
        <span className={`status-pill ${String(r.status || 'ACTIVE').toLowerCase()}`}>
          {r.status || 'ACTIVE'}
        </span>
      )
    },
    {
      key: 'actions',
      label: 'ACTIONS',
      className: 'nowrap',
      render: (r) => (
        <div className="rowactions">
          <button
            className="icon"
            onClick={() => setViewItem(r)}
            title="View Details"
            type="button"
          >
            <Eye size={16} />
          </button>
          {canEditRecalls && (
            <button
              className="icon"
              onClick={() => {
                setModalError('');
                setEditItem(r);
              }}
              title="Edit Recall"
              type="button"
            >
              <Pencil size={16} />
            </button>
          )}
          {isAdmin && (
            <button
              className="icon"
              onClick={() => handleDelete(r)}
              title="Delete Recall (Admin only)"
              type="button"
            >
              <Trash2 size={16} />
            </button>
          )}
        </div>
      )
    }
  ];

  const criticalCount = rows.filter((r) => normalizeSeverity(r.severity) === 'CRITICAL').length;
  const highCount = rows.filter((r) => normalizeSeverity(r.severity) === 'HIGH').length;
  const activeCount = rows.filter(
    (r) => String(r.status || 'ACTIVE').toUpperCase() === 'ACTIVE'
  ).length;

  return (
    <>
      <PageHeading
        eyebrow="RECALL INTELLIGENCE"
        title="Product Recalls"
        description="Track, investigate and manage product recall notices, hazards, and regulatory enforcement."
        action={
          canEditRecalls ? (
            <button
              className="primary"
              onClick={() => {
                setModalError('');
                setEditItem(null);
                setOpenCreate(true);
              }}
              type="button"
            >
              <Plus size={16} /> Add Recall
            </button>
          ) : (
            <span style={{ fontSize: '0.78rem', color: 'var(--muted)', fontStyle: 'italic', padding: '6px 12px', background: 'var(--panel)', border: '1px solid var(--border)', borderRadius: '6px' }}>
              Read-Only Access ({currentUser?.role || 'VIEWER'})
            </span>
          )
        }
      />

      {notice && <p className="success">{notice}</p>}
      {error && <p className="error">{error}</p>}

      <div className="stats four">
        <article className="stat">
          <small>Total notices</small>
          <strong>{rows.length}</strong>
        </article>
        <article className="stat">
          <small>Critical recalls</small>
          <strong>{criticalCount}</strong>
        </article>
        <article className="stat">
          <small>High severity</small>
          <strong>{highCount}</strong>
        </article>
        <article className="stat">
          <small>Active recalls</small>
          <strong>{activeCount}</strong>
        </article>
      </div>

      <section className="panel">
        <div className="toolbar" style={{ flexWrap: 'wrap', gap: '12px' }}>
          <div>
            <h3>Recall register ({filtered.length})</h3>
            <p>Filter by keyword, severity, enforcement status, or date range.</p>
          </div>
          <div className="tools" style={{ flexWrap: 'wrap', gap: '8px' }}>
            <label className="search" style={{ minWidth: '220px' }}>
              <Search size={16} />
              <input
                value={q}
                onChange={(e) => setQ(e.target.value)}
                placeholder="Search recalls, product, model…"
              />
            </label>
            <select value={sev} onChange={(e) => setSev(e.target.value)}>
              <option value="ALL">All severity</option>
              {['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'UNKNOWN'].map((x) => (
                <option key={x} value={x}>
                  {x}
                </option>
              ))}
            </select>
            <select value={status} onChange={(e) => setStatus(e.target.value)}>
              <option value="ALL">All status</option>
              <option value="ACTIVE">ACTIVE</option>
              <option value="RESOLVED">RESOLVED</option>
              <option value="CLOSED">CLOSED</option>
            </select>
            <div style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
              <Calendar size={14} className="muted" />
              <input
                type="date"
                value={fromDate}
                onChange={(e) => setFromDate(e.target.value)}
                title="From Date"
                style={{ width: '130px', padding: '6px 8px', fontSize: '0.85rem' }}
              />
              <span className="muted">to</span>
              <input
                type="date"
                value={toDate}
                onChange={(e) => setToDate(e.target.value)}
                title="To Date"
                style={{ width: '130px', padding: '6px 8px', fontSize: '0.85rem' }}
              />
            </div>
            {(q || sev !== 'ALL' || status !== 'ALL' || fromDate || toDate) && (
              <button
                className="secondary"
                onClick={handleResetFilters}
                type="button"
                title="Clear all filters"
              >
                <FilterX size={15} />
              </button>
            )}
            <button className="secondary" onClick={loadData} type="button" title="Refresh">
              <RefreshCw size={15} />
            </button>
          </div>
        </div>

        <DataTable
          columns={cols}
          rows={paginatedRows}
          loading={loading}
          error=""
          emptyMessage="No recalls match your filters. Try clearing or expanding your criteria."
        />

        {/* Pagination bar */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '12px',
            marginTop: '16px',
            paddingTop: '12px',
            borderTop: '1px solid var(--border)'
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <small className="muted">
              Showing{' '}
              {filtered.length === 0
                ? '0'
                : `${(page - 1) * pageSize + 1}–${Math.min(page * pageSize, filtered.length)}`}{' '}
              of {filtered.length} records
            </small>
            <label style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', margin: 0 }}>
              <small className="muted">Per page:</small>
              <select
                value={pageSize}
                onChange={(e) => setPageSize(Number(e.target.value))}
                style={{ padding: '4px 6px', fontSize: '0.85rem' }}
              >
                <option value={10}>10</option>
                <option value={25}>25</option>
                <option value={50}>50</option>
                <option value={0}>All</option>
              </select>
            </label>
          </div>

          {pageSize > 0 && totalPages > 1 && (
            <div style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
              <button
                className="secondary"
                disabled={page <= 1}
                onClick={() => setPage((p) => Math.max(1, p - 1))}
                type="button"
                style={{ padding: '4px 10px', fontSize: '0.85rem' }}
              >
                Previous
              </button>
              <small style={{ fontWeight: 600, padding: '0 6px' }}>
                Page {page} of {totalPages}
              </small>
              <button
                className="secondary"
                disabled={page >= totalPages}
                onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                type="button"
                style={{ padding: '4px 10px', fontSize: '0.85rem' }}
              >
                Next
              </button>
            </div>
          )}
        </div>
      </section>

      {/* Create / Edit Modal */}
      {(openCreate || editItem) && (
        <div
          className="backdrop"
          onMouseDown={(e) => {
            if (e.target === e.currentTarget) {
              setOpenCreate(false);
              setEditItem(null);
            }
          }}
        >
          <form className="modal" onSubmit={handleSave}>
            <h2>{editItem ? `Edit recall notice #${editItem.id}` : 'Add product recall'}</h2>
            <p className="muted" style={{ marginTop: '-8px', marginBottom: '16px' }}>
              {editItem
                ? 'Update recall details, hazard description, or regulatory status.'
                : 'Register a new recall notice associated with a product catalogue item.'}
            </p>

            {modalError && (
              <div
                style={{
                  color: '#b91c1c',
                  background: '#fef2f2',
                  border: '1px solid #fecaca',
                  padding: '10px 14px',
                  borderRadius: '6px',
                  marginBottom: '14px',
                  fontSize: '0.88rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px'
                }}
              >
                <AlertTriangle size={15} style={{ flexShrink: 0 }} />
                <span>{modalError}</span>
              </div>
            )}

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '12px' }}>
              <label>
                Linked product
                <select
                  name="productId"
                  defaultValue={editItem ? editItem.productId ?? editItem.product?.id ?? '' : ''}
                >
                  <option value="">No linked product</option>
                  {products.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name} {p.brand ? `(${p.brand})` : ''} — ID: #{p.id}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Source record ID
                <input
                  name="sourceRecordId"
                  defaultValue={editItem?.sourceRecordId || ''}
                  maxLength={150}
                  placeholder="e.g. CPSC-24-001 or NHTSA-19V"
                />
              </label>
            </div>

            <label>
              Recall title *
              <input
                name="title"
                required
                maxLength={250}
                defaultValue={editItem?.title || ''}
                placeholder="e.g. Lithium-Ion Battery Overheating and Fire Hazard"
              />
            </label>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '12px' }}>
              <label>
                Severity *
                <select name="severity" defaultValue={editItem?.severity || 'MEDIUM'}>
                  <option value="CRITICAL">CRITICAL</option>
                  <option value="HIGH">HIGH</option>
                  <option value="MEDIUM">MEDIUM</option>
                  <option value="LOW">LOW</option>
                  <option value="UNKNOWN">UNKNOWN</option>
                </select>
              </label>

              <label>
                Recall date
                <input
                  name="recallDate"
                  type="date"
                  defaultValue={
                    editItem?.recallDate
                      ? new Date(editItem.recallDate).toISOString().slice(0, 10)
                      : ''
                  }
                />
              </label>

              <label>
                Status *
                <select name="status" defaultValue={editItem?.status || 'ACTIVE'}>
                  <option value="ACTIVE">ACTIVE</option>
                  <option value="INACTIVE">INACTIVE</option>
                  <option value="RESOLVED">RESOLVED</option>
                  <option value="CLOSED">CLOSED</option>
                </select>
              </label>
            </div>

            <label>
              Source URL *
              <input
                name="sourceUrl"
                type="text"
                required
                defaultValue={editItem?.sourceUrl || ''}
                placeholder="https://www.cpsc.gov/recalls/..."
              />
            </label>

            <label>
              Reason / Hazard
              <textarea
                name="reason"
                rows={2}
                defaultValue={editItem?.reason || ''}
                placeholder="Describe the safety hazard, risk of injury, or root cause."
              />
            </label>

            <label>
              Description / Remedy
              <textarea
                name="description"
                rows={3}
                defaultValue={editItem?.description || ''}
                placeholder="Detailed notice information, remedy, customer instructions, or refund process."
              />
            </label>

            <div className="actions">
              <button
                type="button"
                className="secondary"
                onClick={() => {
                  setOpenCreate(false);
                  setEditItem(null);
                }}
              >
                Cancel
              </button>
              <button className="primary" disabled={saving} type="submit">
                {saving ? 'Saving…' : editItem ? 'Save changes' : 'Create recall'}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* View Details Modal */}
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
              <span className="product-id-tag">Recall #{viewItem.id}</span>
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
                <small className="muted" style={{ display: 'block' }}>Status</small>
                <span className="status-pill">{viewItem.status || 'ACTIVE'}</span>
              </div>
              <div style={{ borderLeft: '1px solid var(--border)', paddingLeft: '12px' }}>
                <small className="muted" style={{ display: 'block' }}>Recall Date</small>
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
                Associated Product
              </h4>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '8px' }}>
                <div>
                  <small className="muted">Product Name</small>
                  <p style={{ margin: 0, fontWeight: 600 }}>{resolveProductName(viewItem, products)}</p>
                </div>
                {(() => {
                  const ent = resolveProductEntity(viewItem, products);
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
                  Safety Hazard & Reason
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

            {/* Description */}
            <div style={{ marginBottom: '14px' }}>
              <h4 style={{ margin: '0 0 4px 0', fontSize: '0.85rem', color: 'var(--muted)', textTransform: 'uppercase' }}>
                Detailed Notice & Description
              </h4>
              <p style={{ margin: 0, lineHeight: 1.5, fontSize: '0.92rem' }}>
                {viewItem.description || 'No detailed description recorded.'}
              </p>
            </div>

            {/* Source URL link */}
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
                  <ExternalLink size={16} /> Open official regulatory source notice
                </a>
              </div>
            )}

            <div className="actions">
              <button
                className="secondary"
                onClick={() => {
                  setModalError('');
                  const target = viewItem;
                  setViewItem(null);
                  setEditItem(target);
                }}
                type="button"
              >
                <Pencil size={15} /> Edit
              </button>
              <button className="primary" onClick={() => setViewItem(null)} type="button">
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Severity Review Modal */}
      {severityModalItem && (
        <div
          className="backdrop"
          onMouseDown={(e) => e.target === e.currentTarget && setSeverityModalItem(null)}
        >
          <div className="modal" style={{ maxWidth: '540px' }}>
            <button className="closemodal" onClick={() => setSeverityModalItem(null)} type="button">
              ×
            </button>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '10px' }}>
              <ShieldAlert size={20} style={{ color: '#ea580c' }} />
              <h3 style={{ margin: 0 }}>Review Severity: Recall #{severityModalItem.id}</h3>
            </div>
            <p className="muted" style={{ fontSize: '0.85rem', marginBottom: '16px' }}>
              Audit-logged severity change. Updates will be recorded in the security audit trail with evidence and source.
            </p>

            <form onSubmit={handleUpdateSeverity}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
                <div>
                  <small className="muted" style={{ display: 'block' }}>Current Severity</small>
                  <Severity value={severityModalItem.severity} />
                </div>
                <span style={{ fontSize: '1.2rem', color: 'var(--muted)' }}>→</span>
                <label style={{ flex: 1, margin: 0 }}>
                  New Severity
                  <select
                    value={selectedSeverity}
                    onChange={(e) => setSelectedSeverity(e.target.value)}
                    required
                  >
                    <option value="CRITICAL">CRITICAL (Imminent Hazard / Severe Harm)</option>
                    <option value="HIGH">HIGH (Significant Risk / Injury)</option>
                    <option value="MEDIUM">MEDIUM (Moderate Risk)</option>
                    <option value="LOW">LOW (Low Risk / Minor Defect)</option>
                    <option value="UNKNOWN">UNKNOWN (Unverified / Insufficient Evidence)</option>
                  </select>
                </label>
              </div>

              {(selectedSeverity === 'CRITICAL' || selectedSeverity === 'HIGH') && (
                <div style={{ background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '6px', padding: '10px 12px', marginBottom: '14px', fontSize: '0.82rem', color: '#991b1b', display: 'flex', gap: '8px', alignItems: 'center' }}>
                  <AlertTriangle size={18} style={{ flexShrink: 0, color: '#dc2626' }} />
                  <span><strong>Policy Requirement:</strong> Never assign HIGH or CRITICAL severity without reliable documented evidence and an authorized classification source.</span>
                </div>
              )}

              <label>
                Classification Source <span style={{ color: '#dc2626' }}>*</span>
                <select
                  value={severitySource}
                  onChange={(e) => setSeveritySource(e.target.value)}
                  required
                >
                  <option value="CPSC Official Notice">CPSC Official Notice (Regulatory Docket)</option>
                  <option value="FDA Enforcement Report">FDA Enforcement Report</option>
                  <option value="NHTSA Safety Portal">NHTSA Safety Portal</option>
                  <option value="Manufacturer Safety Notice">Manufacturer Safety Notice</option>
                  <option value="Expert Risk Audit">Expert Risk Audit / Engineer Assessment</option>
                  <option value="Documented Injury Incident Report">Documented Injury Incident Report</option>
                  <option value="Internal Regulatory Review">Internal Regulatory Review</option>
                </select>
              </label>

              <label>
                Evidence & Documentation {(selectedSeverity === 'CRITICAL' || selectedSeverity === 'HIGH') && <span style={{ color: '#dc2626' }}>* (Required)</span>}
                <textarea
                  rows={2}
                  value={severityEvidence}
                  onChange={(e) => setSeverityEvidence(e.target.value)}
                  placeholder="e.g. CPSC Docket #24-105: 14 verified consumer burn incidents, hospital records, engineering test lab failure report."
                  required={selectedSeverity === 'CRITICAL' || selectedSeverity === 'HIGH'}
                />
              </label>

              <label>
                Reason & Justification <span style={{ color: '#dc2626' }}>*</span>
                <textarea
                  rows={2}
                  value={severityReason}
                  onChange={(e) => setSeverityReason(e.target.value)}
                  placeholder="Explain why this severity tier is being assigned (e.g., reviewed injury reports, official regulatory reclassification)."
                  required
                />
              </label>

              <label>
                Reviewer / Changed By
                <input
                  type="text"
                  value={severityUser}
                  onChange={(e) => setSeverityUser(e.target.value)}
                  placeholder="admin"
                  required
                />
              </label>

              <div className="actions" style={{ marginTop: '16px' }}>
                <button
                  type="button"
                  className="secondary"
                  onClick={() => setSeverityModalItem(null)}
                >
                  Cancel
                </button>
                <button className="primary" disabled={savingSeverity} type="submit">
                  {savingSeverity ? 'Saving…' : 'Record Severity Change'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Severity Audit History Modal */}
      {auditModalItem && (
        <div
          className="backdrop"
          onMouseDown={(e) => e.target === e.currentTarget && setAuditModalItem(null)}
        >
          <div className="modal" style={{ maxWidth: '680px' }}>
            <button className="closemodal" onClick={() => setAuditModalItem(null)} type="button">
              ×
            </button>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
              <History size={20} style={{ color: '#536fe8' }} />
              <h3 style={{ margin: 0 }}>Severity Audit Trail: Recall #{auditModalItem.id}</h3>
            </div>
            <p className="muted" style={{ fontSize: '0.85rem', marginBottom: '14px' }}>
              Complete chronological audit history of verified severity changes.
            </p>

            {loadingAudit ? (
              <p className="muted">Loading audit history…</p>
            ) : auditLogs.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '24px', background: 'var(--bg)', borderRadius: '8px' }}>
                <p className="muted" style={{ margin: 0 }}>
                  No historical severity changes recorded for this recall notice. Current severity: <strong>{auditModalItem.severity}</strong>.
                </p>
              </div>
            ) : (
              <div style={{ maxHeight: '340px', overflowY: 'auto' }}>
                <table style={{ width: '100%', fontSize: '0.88rem', borderCollapse: 'collapse' }}>
                  <thead>
                    <tr style={{ borderBottom: '1px solid var(--border)', textAlign: 'left' }}>
                      <th style={{ padding: '8px 6px' }}>Date</th>
                      <th style={{ padding: '8px 6px' }}>User</th>
                      <th style={{ padding: '8px 6px' }}>Transition</th>
                      <th style={{ padding: '8px 6px' }}>Source</th>
                      <th style={{ padding: '8px 6px' }}>Reason & Evidence</th>
                    </tr>
                  </thead>
                  <tbody>
                    {auditLogs.map((log) => (
                      <tr key={log.id} style={{ borderBottom: '1px solid var(--border)' }}>
                        <td style={{ padding: '8px 6px', whiteSpace: 'nowrap' }}>
                          <small>{formatDate(log.createdAt)}</small>
                        </td>
                        <td style={{ padding: '8px 6px', fontWeight: 600 }}>
                          {log.changedBy || 'System'}
                        </td>
                        <td style={{ padding: '8px 6px', whiteSpace: 'nowrap' }}>
                          <span style={{ color: 'var(--muted)' }}>{log.oldSeverity || '—'}</span>
                          {' → '}
                          <strong>{log.newSeverity}</strong>
                        </td>
                        <td style={{ padding: '8px 6px', whiteSpace: 'nowrap' }}>
                          <span style={{ fontSize: '0.75rem', background: '#eff6ff', color: '#1e40af', padding: '2px 6px', borderRadius: '4px' }}>
                            {log.classificationSource || 'Official Notice'}
                          </span>
                        </td>
                        <td style={{ padding: '8px 6px' }}>
                          <div>{log.reason || 'No reason specified'}</div>
                          {log.evidence && (
                            <small style={{ color: 'var(--muted)', display: 'block', marginTop: '2px' }}>
                              <strong>Evidence:</strong> {log.evidence}
                            </small>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            <div className="actions" style={{ marginTop: '16px' }}>
              <button className="primary" onClick={() => setAuditModalItem(null)} type="button">
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Candidate Product Matching Modal */}
      {candidateModalItem && (
        <div
          className="backdrop"
          onMouseDown={(e) => e.target === e.currentTarget && setCandidateModalItem(null)}
        >
          <div className="modal" style={{ maxWidth: '720px' }}>
            <button className="closemodal" onClick={() => setCandidateModalItem(null)} type="button">
              ×
            </button>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
              <Link size={20} style={{ color: '#536fe8' }} />
              <h3 style={{ margin: 0 }}>Candidate Product Matching: Recall #{candidateModalItem.id}</h3>
            </div>
            <p className="muted" style={{ fontSize: '0.85rem', marginBottom: '14px' }}>
              Recall Notice: <strong>"{candidateModalItem.title}"</strong>
            </p>

            {loadingCandidates ? (
              <p className="muted">Analyzing catalogue products for candidate matches…</p>
            ) : candidateMatches.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '24px', background: 'var(--bg)', borderRadius: '8px' }}>
                <AlertTriangle size={24} style={{ color: '#ea580c', marginBottom: '8px' }} />
                <p style={{ margin: '0 0 6px 0', fontWeight: 600 }}>No Strong Candidate Matches Found</p>
                <p className="muted" style={{ margin: 0, fontSize: '0.85rem' }}>
                  No catalogue products matched the recall notice title, brand, model, or batch numbers.
                </p>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', maxHeight: '360px', overflowY: 'auto' }}>
                {candidateMatches.map((cand) => (
                  <div
                    key={cand.productId}
                    style={{
                      border: '1px solid var(--border)',
                      borderRadius: '8px',
                      padding: '12px 14px',
                      background: 'var(--panel)',
                      display: 'flex',
                      flexDirection: 'column',
                      gap: '6px'
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                      <div>
                        <strong style={{ fontSize: '0.95rem' }}>{cand.productName}</strong>
                        <small style={{ display: 'block', color: 'var(--muted)' }}>
                          Brand: {cand.brand || '—'} | Model: {cand.modelNumber || '—'} | Batch: {cand.batchNumber || '—'}
                        </small>
                      </div>
                      <div style={{ textAlign: 'right' }}>
                        <span
                          style={{
                            fontSize: '0.85rem',
                            fontWeight: 700,
                            color: cand.confidenceScore >= 0.7 ? '#16a34a' : cand.confidenceScore >= 0.4 ? '#ca8a04' : '#64748b'
                          }}
                        >
                          {Math.round(cand.confidenceScore * 100)}% Match
                        </span>
                      </div>
                    </div>

                    {cand.matchReasons && cand.matchReasons.length > 0 && (
                      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px', marginTop: '2px' }}>
                        {cand.matchReasons.map((reason, rIdx) => (
                          <span
                            key={rIdx}
                            style={{
                              fontSize: '0.72rem',
                              padding: '2px 6px',
                              background: '#eff6ff',
                              color: '#1d4ed8',
                              borderRadius: '4px'
                            }}
                          >
                            ✓ {reason}
                          </span>
                        ))}
                      </div>
                    )}

                    {cand.soleBrandMatch && (
                      <small style={{ color: '#ea580c', fontSize: '0.75rem' }}>
                        ⚠️ Matched solely by brand name. Verified confirmation required before linking.
                      </small>
                    )}

                    <div style={{ display: 'flex', gap: '8px', marginTop: '6px', justifyContent: 'flex-end' }}>
                      <button
                        type="button"
                        className="secondary"
                        disabled={associating}
                        onClick={() => handleAssociateProduct(candidateModalItem.id, cand.productId, 'PENDING_REVIEW')}
                        style={{ fontSize: '0.75rem', padding: '4px 10px' }}
                      >
                        Mark Pending Review
                      </button>
                      <button
                        type="button"
                        className="primary"
                        disabled={associating || !adminLinkConfirmed}
                        title={!adminLinkConfirmed ? 'Administrator confirmation checkbox must be checked below' : ''}
                        onClick={() => handleAssociateProduct(candidateModalItem.id, cand.productId, 'LINKED')}
                        style={{ fontSize: '0.75rem', padding: '4px 10px', opacity: adminLinkConfirmed ? 1 : 0.6 }}
                      >
                        <CheckCircle size={13} /> Link This Product
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}

            <div style={{ marginTop: '14px', padding: '12px', border: '1px dashed var(--border)', borderRadius: '6px', background: 'var(--bg)' }}>
              <small style={{ fontWeight: 600, display: 'block', marginBottom: '6px' }}>Or select from Product Catalogue manually:</small>
              <div style={{ display: 'flex', gap: '8px' }}>
                <select
                  value={manualSelectedProductId}
                  onChange={(e) => setManualSelectedProductId(e.target.value)}
                  style={{ flex: 1, fontSize: '0.85rem' }}
                >
                  <option value="">-- Choose Catalogue Product --</option>
                  {products.map((p) => (
                    <option key={p.id} value={p.id}>
                      #{p.id} - {p.name} ({p.brand || 'No brand'}) - {p.modelNumber || 'No model'} [{p.category || 'General'}]
                    </option>
                  ))}
                </select>
                <button
                  type="button"
                  className="secondary"
                  disabled={!manualSelectedProductId || associating || !adminLinkConfirmed}
                  onClick={() => handleAssociateProduct(candidateModalItem.id, Number(manualSelectedProductId), 'LINKED')}
                  style={{ fontSize: '0.8rem', whiteSpace: 'nowrap', opacity: adminLinkConfirmed && manualSelectedProductId ? 1 : 0.6 }}
                >
                  Link Selected Product
                </button>
              </div>
            </div>

            <div style={{ marginTop: '14px', padding: '10px 12px', background: adminLinkConfirmed ? '#f0fdf4' : '#fffbeb', border: `1px solid ${adminLinkConfirmed ? '#bbf7d0' : '#fde68a'}`, borderRadius: '6px' }}>
              <label style={{ display: 'flex', alignItems: 'flex-start', gap: '10px', cursor: 'pointer', margin: 0, fontSize: '0.85rem' }}>
                <input
                  type="checkbox"
                  checked={adminLinkConfirmed}
                  onChange={(e) => setAdminLinkConfirmed(e.target.checked)}
                  style={{ marginTop: '3px' }}
                />
                <div>
                  <strong style={{ color: adminLinkConfirmed ? '#166534' : '#92400e' }}>
                    Administrator Confirmation Required
                  </strong>
                  <p style={{ margin: '2px 0 0 0', fontSize: '0.78rem', color: 'var(--muted)' }}>
                    I confirm that I have reviewed the recall notice details against the product catalogue entry (brand, model, category, batch) and certify that this product association is accurate.
                  </p>
                </div>
              </label>
            </div>

            <div className="actions" style={{ marginTop: '16px' }}>
              {candidateModalItem.productId && (
                <button
                  type="button"
                  className="secondary"
                  disabled={associating}
                  onClick={() => handleAssociateProduct(candidateModalItem.id, null, 'UNLINKED')}
                  style={{ color: '#dc2626' }}
                >
                  Unlink Current Product
                </button>
              )}
              <button className="primary" onClick={() => setCandidateModalItem(null)} type="button">
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
