import React, { useEffect, useMemo, useState } from 'react';
import { Plus, Search, Eye, Pencil, Trash2, RefreshCw, ShieldAlert } from 'lucide-react';
import { productApi, unwrapList } from '../services/api';
import { useAuth } from '../context/AuthContext';
import PageHeading from '../components/PageHeading';
import DataTable from '../components/DataTable';
import ProductForm from '../components/ProductForm';

export default function Products() {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [q, setQ] = useState('');
  const [open, setOpen] = useState(false);
  const [edit, setEdit] = useState(null);
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState('');
  const [view, setView] = useState(null);

  const { user: currentUser, isAdmin, role } = useAuth();

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await productApi.getAll({ page: 0, size: 100 });
      setRows(unwrapList(response.data));
    } catch (e) {
      setError(e.message || 'Failed to load products.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const filtered = useMemo(() => {
    return rows.filter((p) =>
      [p.name, p.brand, p.category, p.modelNumber, p.batchNumber].some((v) =>
        String(v || '').toLowerCase().includes(q.toLowerCase().trim())
      )
    );
  }, [rows, q]);

  const handleSave = async (p) => {
    setSaving(true);
    setError('');
    try {
      if (edit) {
        await productApi.update(edit.id, p);
        setNotice(`Product "${p.name}" updated successfully.`);
      } else {
        await productApi.create(p);
        setNotice(`Product "${p.name}" created successfully.`);
      }
      setOpen(false);
      setEdit(null);
      await loadData();
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  };

  const handleRemove = async (p) => {
    if (!window.confirm(`Are you sure you want to delete product "${p.name}" (ID #${p.id})?`)) {
      return;
    }
    setError('');
    try {
      await productApi.remove(p.id);
      setNotice(`Product "${p.name}" deleted successfully.`);
      await loadData();
    } catch (e) {
      setError(e.message);
    }
  };

  const columns = [
    {
      key: 'name',
      label: 'PRODUCT',
      className: 'cell-title',
      render: (p) => (
        <div className="primary-cell">
          <b className="product-title">{p.name}</b>
          <small className="muted">ID: #{p.id}</small>
        </div>
      )
    },
    { key: 'brand', label: 'BRAND', render: (p) => p.brand || '—' },
    { key: 'category', label: 'CATEGORY', render: (p) => p.category || '—' },
    { key: 'modelNumber', label: 'MODEL', render: (p) => p.modelNumber || '—' },
    { key: 'batchNumber', label: 'BATCH', render: (p) => p.batchNumber || '—' },
    {
      key: 'actions',
      label: 'ACTIONS',
      className: 'nowrap',
      render: (p) => (
        <div className="rowactions">
          <button
            className="icon"
            onClick={() => setView(p)}
            title="View Details"
            type="button"
          >
            <Eye size={16} />
          </button>
          {isAdmin && (
            <>
              <button
                className="icon"
                onClick={() => {
                  setError('');
                  setEdit(p);
                  setOpen(true);
                }}
                title="Edit Product (Admin only)"
                type="button"
              >
                <Pencil size={16} />
              </button>
              <button
                className="icon"
                onClick={() => handleRemove(p)}
                title="Delete Product (Admin only)"
                type="button"
              >
                <Trash2 size={16} />
              </button>
            </>
          )}
        </div>
      )
    }
  ];

  return (
    <>
      <PageHeading
        eyebrow="CATALOGUE"
        title="Products"
        description="Manage your product catalogue, identifiers and registered records."
        action={
          isAdmin ? (
            <button
              className="primary"
              onClick={() => {
                setError('');
                setEdit(null);
                setOpen(true);
              }}
              type="button"
            >
              <Plus size={16} /> Add Product
            </button>
          ) : (
            <span style={{ fontSize: '0.78rem', color: 'var(--muted)', fontStyle: 'italic', padding: '6px 12px', background: 'var(--panel)', border: '1px solid var(--border)', borderRadius: '6px' }}>
              Read-Only Access ({currentUser?.role || 'VIEWER'})
            </span>
          )
        }
      />

      <section className="panel">
        <div className="toolbar">
          <div>
            <h3>
              Product catalogue <span className="count">{rows.length}</span>
            </h3>
            <p>Registered products available for recall tracking.</p>
          </div>
          <div className="tools">
            <label className="search">
              <Search size={16} />
              <input
                value={q}
                onChange={(e) => setQ(e.target.value)}
                placeholder="Search name, brand, model, batch…"
              />
            </label>
            <button className="secondary" onClick={loadData} type="button">
              <RefreshCw size={15} /> Refresh
            </button>
          </div>
        </div>

        {notice && <p className="success">{notice}</p>}
        {error && <p className="error">{error}</p>}

        <DataTable
          columns={columns}
          rows={filtered}
          loading={loading}
          error=""
          emptyMessage="No products found in catalogue. Add your first product."
        />
      </section>

      <ProductForm
        open={open}
        initialValue={edit}
        onClose={() => {
          setOpen(false);
          setError('');
        }}
        onSubmit={handleSave}
        saving={saving}
        error={error}
      />

      {view && (
        <div className="backdrop" onMouseDown={(e) => e.target === e.currentTarget && setView(null)}>
          <div className="modal">
            <button className="closemodal" onClick={() => setView(null)} type="button">
              ×
            </button>
            <h2>{view.name}</h2>
            <div style={{ display: 'grid', gap: '8px', marginTop: '10px' }}>
              <p><b>Product ID:</b> #{view.id}</p>
              <p><b>Brand:</b> {view.brand || '—'}</p>
              <p><b>Category:</b> {view.category || '—'}</p>
              <p><b>Model Number:</b> {view.modelNumber || '—'}</p>
              <p><b>Batch Number:</b> {view.batchNumber || '—'}</p>
              <p><b>Registered Date:</b> {view.createdAt ? new Date(view.createdAt).toLocaleString() : '—'}</p>
            </div>
            <div className="actions">
              <button
                className="secondary"
                onClick={() => {
                  setEdit(view);
                  setView(null);
                  setOpen(true);
                }}
                type="button"
              >
                <Pencil size={15} /> Edit Product
              </button>
              <button className="primary" onClick={() => setView(null)} type="button">
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
