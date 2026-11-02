import { useEffect, useState } from 'react';
import { X, AlertCircle } from 'lucide-react';

const empty = { name: '', brand: '', category: '', modelNumber: '', batchNumber: '' };

export default function ProductForm({ open, initialValue, onClose, onSubmit, saving, error }) {
  const [form, setForm] = useState(empty);
  const [validation, setValidation] = useState('');

  useEffect(() => {
    if (open) {
      setForm(initialValue ? {
        name: initialValue.name || '',
        brand: initialValue.brand || '',
        category: initialValue.category || '',
        modelNumber: initialValue.modelNumber || '',
        batchNumber: initialValue.batchNumber || ''
      } : empty);
      setValidation('');
    }
  }, [open, initialValue]);

  if (!open) return null;

  const change = (event) => {
    setValidation('');
    setForm(current => ({ ...current, [event.target.name]: event.target.value }));
  };

  const submit = (event) => {
    event.preventDefault();
    if (!form.name.trim()) {
      setValidation('Product name is required.');
      return;
    }
    setValidation('');
    onSubmit(Object.fromEntries(Object.entries(form).map(([key, value]) => [key, value.trim()])));
  };

  return (
    <div
      className="modal-backdrop"
      role="presentation"
      onMouseDown={(event) => event.target === event.currentTarget && onClose()}
    >
      <section className="modal" role="dialog" aria-modal="true" aria-labelledby="product-modal-title">
        <div className="modal-header">
          <div>
            <span className="eyebrow">PRODUCT CATALOGUE</span>
            <h2 id="product-modal-title">{initialValue ? 'Edit product' : 'Add product'}</h2>
          </div>
          <button className="icon-button" onClick={onClose} type="button" aria-label="Close">
            <X size={18} />
          </button>
        </div>

        <form className="form-grid" onSubmit={submit}>
          <label>
            Product name *
            <input
              name="name"
              value={form.name}
              onChange={change}
              required
              maxLength="160"
              placeholder="e.g. Galaxy S24"
            />
          </label>
          <label>
            Brand
            <input
              name="brand"
              value={form.brand}
              onChange={change}
              maxLength="100"
              placeholder="e.g. Samsung"
            />
          </label>
          <label>
            Category
            <input
              name="category"
              value={form.category}
              onChange={change}
              maxLength="100"
              placeholder="e.g. Smartphone"
            />
          </label>
          <label>
            Model number
            <input
              name="modelNumber"
              value={form.modelNumber}
              onChange={change}
              maxLength="100"
              placeholder="e.g. SM-S921B"
            />
          </label>
          <label>
            Batch number
            <input
              name="batchNumber"
              value={form.batchNumber}
              onChange={change}
              maxLength="100"
              placeholder="e.g. SAM2026-001"
            />
          </label>

          {validation && (
            <p className="alert alert-error form-full" style={{ color: '#dc2626', background: '#fee2e2', padding: '10px 12px', borderRadius: '6px', fontSize: '13px' }}>
              {validation}
            </p>
          )}

          {error && (
            <div
              className="alert alert-error form-full"
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                color: '#b91c1c',
                background: '#fef2f2',
                border: '1px solid #fecaca',
                padding: '10px 14px',
                borderRadius: '6px',
                fontSize: '13px'
              }}
            >
              <AlertCircle size={16} />
              <span>{error}</span>
            </div>
          )}

          <div className="modal-actions form-full">
            <button type="button" className="button button-secondary" onClick={onClose}>
              Cancel
            </button>
            <button
              type="submit"
              className="button button-primary"
              disabled={saving}
            >
              {saving ? 'Saving...' : initialValue ? 'Save changes' : 'Create product'}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}
