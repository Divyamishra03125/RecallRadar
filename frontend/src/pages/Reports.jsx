import React, { useEffect, useMemo, useState } from 'react';
import { Download, FileText, CheckCircle2, Eye, Table } from 'lucide-react';
import {
  productApi,
  recallApi,
  unwrapList,
  normalizeSeverity,
  resolveProductName,
  resolveProductEntity
} from '../services/api';
import PageHeading from '../components/PageHeading';

function exportCsv(filename, rows) {
  if (!rows || !rows.length) {
    alert('No records available to export.');
    return;
  }
  const keys = [...new Set(rows.flatMap((x) => Object.keys(x)))];
  const esc = (v) => '"' + String(v ?? '').replaceAll('"', '""') + '"';
  const header = keys.map(esc).join(',');
  const lines = rows.map((r) =>
    keys
      .map((k) =>
        esc(typeof r[k] === 'object' && r[k] !== null ? JSON.stringify(r[k]) : r[k])
      )
      .join(',')
  );
  const text = [header, ...lines].join('\r\n');
  const blob = new Blob(['\uFEFF' + text], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

function exportJson(filename, data) {
  if (!data || !data.length) {
    alert('No data available to export.');
    return;
  }
  const text = JSON.stringify(data, null, 2);
  const blob = new Blob([text], { type: 'application/json;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

export default function Reports() {
  const [p, setP] = useState([]);
  const [r, setR] = useState([]);
  const [busy, setBusy] = useState(true);
  const [preview, setPreview] = useState(null);

  useEffect(() => {
    Promise.all([
      productApi.getAll({ page: 0, size: 100 }),
      recallApi.getAll({ page: 0, size: 100 })
    ])
      .then(([prodRes, recallRes]) => {
        setP(unwrapList(prodRes.data));
        setR(unwrapList(recallRes.data));
      })
      .catch(() => {})
      .finally(() => setBusy(false));
  }, []);

  const criticalRecalls = useMemo(
    () => r.filter((x) => normalizeSeverity(x.severity) === 'CRITICAL'),
    [r]
  );

  const highRecalls = useMemo(
    () => r.filter((x) => normalizeSeverity(x.severity) === 'HIGH'),
    [r]
  );

  const criticalAndHighRecalls = useMemo(
    () =>
      r
        .filter((x) => {
          const s = normalizeSeverity(x.severity);
          return s === 'CRITICAL' || s === 'HIGH';
        })
        .map((item) => ({
          recallId: item.id,
          title: item.title,
          product: resolveProductName(item, p),
          severity: normalizeSeverity(item.severity),
          status: item.status || 'ACTIVE',
          recallDate: item.recallDate || '',
          hazardReason: item.reason || '',
          sourceUrl: item.sourceUrl || ''
        })),
    [r, p]
  );

  const activeRecalls = useMemo(
    () =>
      r
        .filter((x) => String(x.status || 'ACTIVE').toUpperCase() === 'ACTIVE')
        .map((item) => ({
          recallId: item.id,
          title: item.title,
          product: resolveProductName(item, p),
          severity: normalizeSeverity(item.severity),
          recallDate: item.recallDate || '',
          hazardReason: item.reason || '',
          sourceUrl: item.sourceUrl || ''
        })),
    [r, p]
  );

  const recallExportRows = useMemo(
    () =>
      r.map((item) => {
        const ent = resolveProductEntity(item, p);
        return {
          recallId: item.id,
          sourceRecordId: item.sourceRecordId || '',
          title: item.title,
          productName: resolveProductName(item, p),
          brand: ent?.brand || '',
          modelNumber: ent?.modelNumber || '',
          batchNumber: ent?.batchNumber || '',
          severity: normalizeSeverity(item.severity),
          status: item.status || 'ACTIVE',
          recallDate: item.recallDate || '',
          hazardReason: item.reason || '',
          description: item.description || '',
          sourceUrl: item.sourceUrl || ''
        };
      }),
    [r, p]
  );

  const productExportRows = useMemo(
    () =>
      p.map((item) => ({
        productId: item.id,
        name: item.name,
        brand: item.brand || '',
        category: item.category || '',
        modelNumber: item.modelNumber || '',
        batchNumber: item.batchNumber || '',
        createdAt: item.createdAt || ''
      })),
    [p]
  );

  const distinctProductsAtRisk = useMemo(() => {
    const registeredIds = new Set(p.map((prod) => String(prod.id)));
    const matching = activeRecalls
      .map((x) => x.product?.id ?? x.productId ?? x.product_id)
      .filter((id) => id != null && id !== '')
      .map((id) => String(id))
      .filter((id) => registeredIds.has(id));
    return new Set(matching).size;
  }, [activeRecalls, p]);

  const severityDistribution = useMemo(() => {
    const dist = { CRITICAL: 0, HIGH: 0, MEDIUM: 0, LOW: 0, UNKNOWN: 0 };
    r.forEach((item) => {
      const s = normalizeSeverity(item.severity);
      if (dist[s] !== undefined) dist[s] += 1;
      else dist.UNKNOWN += 1;
    });
    return dist;
  }, [r]);

  const pluralize = (count, singular, plural = `${singular}s`) =>
    `${count} ${count === 1 ? singular : plural}`;

  const reportItems = [
    {
      title: 'Product Catalogue Register',
      desc: 'All registered products with manufacturer brand, model identifiers, and batch numbers.',
      filename: 'recallradar-product-catalogue',
      data: productExportRows,
      badge: pluralize(productExportRows.length, 'record')
    },
    {
      title: 'Full Product Recall Register',
      desc: 'Complete recall notice register including associated product names, hazard descriptions, and official source links.',
      filename: 'recallradar-recall-register',
      data: recallExportRows,
      badge: pluralize(recallExportRows.length, 'notice')
    },
    {
      title: 'Priority Alerts (Critical & High)',
      desc: 'Urgent notices categorized under Red (Critical) and Orange (High) severity tiers.',
      filename: 'recallradar-priority-alerts',
      data: criticalAndHighRecalls,
      badge: pluralize(criticalAndHighRecalls.length, 'priority item')
    },
    {
      title: 'Active Enforcement Watchlist',
      desc: 'Recalls currently marked with ACTIVE enforcement status requiring immediate action.',
      filename: 'recallradar-active-watchlist',
      data: activeRecalls,
      badge: pluralize(activeRecalls.length, 'active notice')
    },
    {
      title: 'Executive Risk Summary',
      desc: 'High-level aggregated statistics of total products, active recalls, distinct products at risk, and severity breakdown.',
      filename: 'recallradar-risk-summary',
      data: [
        {
          totalProducts: p.length,
          totalRecalls: r.length,
          activeRecalls: activeRecalls.length,
          criticalRecalls: criticalRecalls.length,
          highSeverityRecalls: highRecalls.length,
          distinctRegisteredProductsAtRisk: distinctProductsAtRisk,
          severityCritical: severityDistribution.CRITICAL,
          severityHigh: severityDistribution.HIGH,
          severityMedium: severityDistribution.MEDIUM,
          severityLow: severityDistribution.LOW,
          severityUnknown: severityDistribution.UNKNOWN,
          reportGeneratedAt: new Date().toISOString()
        }
      ],
      badge: 'Aggregated KPI'
    }
  ];

  return (
    <>
      <PageHeading
        eyebrow="INTELLIGENCE EXPORT"
        title="Reports & Data Export"
        description="Generate, preview, and download compliance reports and datasets in CSV and JSON formats."
      />

      <div className="reportgrid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '16px' }}>
        {reportItems.map((item) => (
          <article className="panel report" key={item.title} style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
                <span className="product-id-tag">{item.badge}</span>
                <FileText size={16} className="muted" />
              </div>
              <h3 style={{ margin: '0 0 6px 0', fontSize: '1.05rem' }}>{item.title}</h3>
              <p style={{ margin: '0 0 16px 0', fontSize: '0.88rem', color: 'var(--muted)', lineHeight: 1.5 }}>
                {item.desc}
              </p>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap', paddingTop: '12px', borderTop: '1px solid var(--border)' }}>
              <button
                className="secondary"
                onClick={() => setPreview({ title: item.title, rows: item.data })}
                type="button"
                style={{ fontSize: '0.85rem', padding: '6px 12px' }}
                title="Preview records"
              >
                <Eye size={14} /> Preview
              </button>
              <button
                className="primary"
                onClick={() => exportCsv(`${item.filename}.csv`, item.data)}
                type="button"
                style={{ fontSize: '0.85rem', padding: '6px 12px' }}
              >
                <Download size={14} /> Export CSV
              </button>
              <button
                className="secondary"
                onClick={() => exportJson(`${item.filename}.json`, item.data)}
                type="button"
                style={{ fontSize: '0.85rem', padding: '6px 10px' }}
                title="Export raw JSON"
              >
                JSON
              </button>
            </div>
          </article>
        ))}
      </div>

      {/* Preview Modal */}
      {preview && (
        <div
          className="backdrop"
          onMouseDown={(e) => e.target === e.currentTarget && setPreview(null)}
        >
          <div className="modal" style={{ maxWidth: '850px' }}>
            <button className="closemodal" onClick={() => setPreview(null)} type="button">
              ×
            </button>
            <h2>Preview: {preview.title}</h2>
            <p className="muted">Showing top {Math.min(5, preview.rows.length)} of {preview.rows.length} records</p>

            <div style={{ overflowX: 'auto', maxHeight: '360px', marginTop: '12px' }}>
              {preview.rows.length === 0 ? (
                <p className="muted">No data available for this report.</p>
              ) : (
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.82rem' }}>
                  <thead>
                    <tr style={{ background: 'var(--bg)', borderBottom: '2px solid var(--border)' }}>
                      {Object.keys(preview.rows[0] || {}).map((col) => (
                        <th key={col} style={{ padding: '8px 10px', textAlign: 'left', whiteSpace: 'nowrap' }}>
                          {col}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {preview.rows.slice(0, 5).map((row, rIdx) => (
                      <tr key={rIdx} style={{ borderBottom: '1px solid var(--border)' }}>
                        {Object.values(row).map((val, cIdx) => (
                          <td
                            key={cIdx}
                            style={{
                              padding: '8px 10px',
                              maxWidth: '220px',
                              overflow: 'hidden',
                              textOverflow: 'ellipsis',
                              whiteSpace: 'nowrap'
                            }}
                            title={String(val ?? '')}
                          >
                            {String(val ?? '—')}
                          </td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>

            <div className="actions" style={{ marginTop: '16px' }}>
              <button
                className="primary"
                onClick={() => {
                  exportCsv('report-preview.csv', preview.rows);
                  setPreview(null);
                }}
                type="button"
              >
                <Download size={14} /> Download Full CSV
              </button>
              <button className="secondary" onClick={() => setPreview(null)} type="button">
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
