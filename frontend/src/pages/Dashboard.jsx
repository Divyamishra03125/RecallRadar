import React, { useEffect, useMemo, useState } from 'react';
import {
  Package,
  PackageSearch,
  AlertTriangle,
  ShieldAlert,
  Activity
} from 'lucide-react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Legend
} from 'recharts';
import {
  productApi,
  recallApi,
  unwrapList,
  normalizeSeverity,
  resolveProductName,
  SEVERITY_COLORS
} from '../services/api';
import PageHeading from '../components/PageHeading';
import StatCard from '../components/StatCard';
import Severity from '../components/Severity';
import DataTable from '../components/DataTable';

const formatDate = (val) => {
  if (!val) return 'Date unavailable';
  try {
    const d = new Date(val);
    return Number.isNaN(d.getTime()) ? 'Date unavailable' : d.toLocaleDateString('en-IN');
  } catch {
    return 'Date unavailable';
  }
};

export default function Dashboard() {
  const [p, setP] = useState([]);
  const [r, setR] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const [prodRes, recallRes] = await Promise.all([
        productApi.getAll({ page: 0, size: 100 }),
        recallApi.getAll({ page: 0, size: 100 })
      ]);
      setP(unwrapList(prodRes.data));
      setR(unwrapList(recallRes.data));
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const activeRecalls = useMemo(
    () => r.filter((x) => String(x.status || 'ACTIVE').toUpperCase() === 'ACTIVE'),
    [r]
  );

  const criticalRecalls = useMemo(
    () => r.filter((x) => normalizeSeverity(x.severity) === 'CRITICAL'),
    [r]
  );

  const productsAtRiskCount = useMemo(() => {
    // Only count distinct registered products in `p` that have at least one active recall
    const registeredProductIds = new Set(p.map((prod) => String(prod.id)));
    const matchingIds = activeRecalls
      .map((x) => x.product?.id ?? x.productId ?? x.product_id)
      .filter((id) => id != null && id !== '')
      .map((id) => String(id))
      .filter((id) => registeredProductIds.has(id));

    return new Set(matchingIds).size;
  }, [activeRecalls, p]);

  // Compute normalized severity counts
  const severityCounts = useMemo(() => {
    const counts = {
      CRITICAL: 0,
      HIGH: 0,
      MEDIUM: 0,
      LOW: 0,
      UNKNOWN: 0
    };

    r.forEach((item) => {
      const norm = normalizeSeverity(item.severity);
      if (counts[norm] !== undefined) {
        counts[norm] += 1;
      } else {
        counts.UNKNOWN += 1;
      }
    });

    return counts;
  }, [r]);

  // Filter out zero-value categories so empty slices do not distort the pie chart
  const severityChartData = useMemo(() => {
    const order = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'UNKNOWN'];
    return order
      .map((name) => ({
        name,
        value: severityCounts[name] || 0
      }))
      .filter((entry) => entry.value > 0);
  }, [severityCounts]);

  const monthly = useMemo(() => {
    return Array.from({ length: 6 }, (_, i) => {
      const d = new Date();
      d.setDate(1);
      d.setMonth(d.getMonth() - (5 - i));
      const month = d.toLocaleString('en', { month: 'short' });
      // Group strictly by verified recall publication date
      const count = r.filter((x) => {
        if (!x.recallDate) return false;
        const dt = new Date(x.recallDate);
        return (
          !Number.isNaN(dt.getTime()) &&
          dt.getMonth() === d.getMonth() &&
          dt.getFullYear() === d.getFullYear()
        );
      }).length;
      return { month, count };
    });
  }, [r]);

  const cols = [
    { key: 'id', label: 'ID', className: 'nowrap' },
    {
      key: 'title',
      label: 'RECALL',
      className: 'cell-title',
      render: (x) => (
        <div>
          <b>{x.title || 'Untitled recall'}</b>
          {x.reason && <small>{x.reason}</small>}
        </div>
      )
    },
    {
      key: 'product',
      label: 'PRODUCT',
      render: (x) => <span>{resolveProductName(x, p)}</span>
    },
    {
      key: 'severity',
      label: 'SEVERITY',
      className: 'nowrap',
      render: (x) => <Severity value={x.severity} />
    },
    {
      key: 'recallDate',
      label: 'DATE',
      className: 'nowrap',
      render: (x) => formatDate(x.recallDate)
    },
    {
      key: 'status',
      label: 'STATUS',
      className: 'nowrap',
      render: (x) => (
        <span className="status-pill">{x.status || 'ACTIVE'}</span>
      )
    }
  ];

  return (
    <>
      <PageHeading
        eyebrow="OVERVIEW"
        title="Dashboard"
        description="Monitor product safety, recall activity and emerging risks across your workspace."
      />

      {error && <p className="error">{error}</p>}

      <div className="stats">
        {[
          [Package, 'Total products', p.length, 'Registered products'],
          [PackageSearch, 'Total recalls', r.length, 'Loaded recall records'],
          [AlertTriangle, 'Active recalls', activeRecalls.length, 'Requires monitoring'],
          [ShieldAlert, 'Critical recalls', criticalRecalls.length, 'Urgent action required'],
          [Activity, 'Products at risk', productsAtRiskCount, 'Active recall associations']
        ].map(([Icon, label, val, note]) => (
          <StatCard
            key={label}
            label={label}
            value={loading ? '…' : val}
            note={note}
          />
        ))}
      </div>

      <div className="charts">
        <section className="panel">
          <div className="toolbar">
            <div>
              <h3>Recall severity distribution</h3>
              <p>Severity breakdown across all loaded recall records.</p>
            </div>
          </div>
          <div className="chart">
            {severityChartData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={severityChartData}
                    dataKey="value"
                    nameKey="name"
                    innerRadius={55}
                    outerRadius={88}
                    paddingAngle={3}
                  >
                    {severityChartData.map((entry) => (
                      <Cell
                        key={`cell-${entry.name}`}
                        fill={SEVERITY_COLORS[entry.name] || '#94a3b8'}
                      />
                    ))}
                  </Pie>
                  <Tooltip
                    formatter={(val, name) => [`${val} recalls`, name]}
                  />
                  <Legend
                    verticalAlign="bottom"
                    iconType="circle"
                    height={36}
                  />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div className="empty">No severity data available.</div>
            )}
          </div>
        </section>

        <section className="panel">
          <div className="toolbar">
            <div>
              <h3>Monthly recall trends</h3>
              <p>Recall publication activity over the last 6 months.</p>
            </div>
          </div>
          <div className="chart">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={monthly}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="month" />
                <YAxis allowDecimals={false} />
                <Tooltip />
                <Bar
                  dataKey="count"
                  name="Recalls"
                  fill="#536fe8"
                  radius={[5, 5, 0, 0]}
                />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </section>
      </div>

      <section className="panel">
        <div className="toolbar">
          <div>
            <h3>Recent recalls</h3>
            <p>Latest recall notices tracked in the database.</p>
          </div>
        </div>
        <DataTable
          columns={cols}
          rows={[...r].slice(-6).reverse()}
          loading={loading}
          error=""
          emptyMessage="No recall records available."
        />
      </section>
    </>
  );
}
