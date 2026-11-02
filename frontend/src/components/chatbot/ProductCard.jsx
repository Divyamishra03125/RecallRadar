import React from 'react';
import { Package, Tag, Layers, CheckCircle } from 'lucide-react';

export default function ProductCard({ product }) {
  if (!product) return null;

  return (
    <div
      style={{
        border: '1px solid var(--border)',
        borderRadius: '10px',
        padding: '12px 14px',
        background: 'var(--panel)',
        display: 'flex',
        flexDirection: 'column',
        gap: '6px',
        fontSize: '0.85rem',
        boxShadow: '0 2px 6px rgba(0, 0, 0, 0.05)',
        margin: '6px 0'
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '8px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <div
            style={{
              width: '28px',
              height: '28px',
              borderRadius: '6px',
              background: 'rgba(99, 102, 241, 0.15)',
              color: '#818cf8',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}
          >
            <Package size={16} />
          </div>
          <div>
            <strong style={{ fontSize: '0.94rem', color: 'var(--text)', display: 'block' }}>
              {product.name}
            </strong>
            <small style={{ color: 'var(--muted)', fontSize: '0.72rem' }}>
              Catalog Record #{product.id}
            </small>
          </div>
        </div>

        {product.brand && (
          <span
            style={{
              fontSize: '0.72rem',
              padding: '3px 8px',
              borderRadius: '12px',
              background: 'rgba(99, 102, 241, 0.12)',
              color: '#818cf8',
              fontWeight: 600,
              display: 'flex',
              alignItems: 'center',
              gap: '4px'
            }}
          >
            <Tag size={11} />
            {product.brand}
          </span>
        )}
      </div>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))',
          gap: '8px',
          marginTop: '6px',
          paddingTop: '8px',
          borderTop: '1px solid var(--border)',
          fontSize: '0.8rem',
          color: 'var(--muted)'
        }}
      >
        {product.modelNumber && (
          <div>
            Model:{' '}
            <code
              style={{
                background: 'rgba(0, 0, 0, 0.06)',
                padding: '1px 5px',
                borderRadius: '4px',
                color: 'var(--text)',
                fontFamily: 'monospace'
              }}
            >
              {product.modelNumber}
            </code>
          </div>
        )}

        {product.batchNumber && (
          <div>
            Batch:{' '}
            <code
              style={{
                background: 'rgba(0, 0, 0, 0.06)',
                padding: '1px 5px',
                borderRadius: '4px',
                color: 'var(--text)',
                fontFamily: 'monospace'
              }}
            >
              {product.batchNumber}
            </code>
          </div>
        )}

        {product.category && (
          <div>
            Category: <b style={{ color: 'var(--text)' }}>{product.category}</b>
          </div>
        )}
      </div>
    </div>
  );
}
