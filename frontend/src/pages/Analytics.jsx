import React, { useEffect, useMemo, useState } from 'react';
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
import { RefreshCw, TrendingUp, AlertTriangle, ShieldCheck, Layers } from 'lucide-react';
import {
  recallApi,
  productApi,
  unwrapList,
  normalizeSeverity,
  resolveProductCategory,
  SEVERITY_COLORS
} from '../services/api';
import PageHeading from '../components/PageHeading';
import StatCard from '../components/StatCard';

export default function Analytics() {
  const [r, setR] = useState([]);
  const [p, setP] = useState([]);
  const [busy, setBusy] = useState(true);
  const [error, setError] = useState('');

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
      .catch((e) => setError(e.message || 'Failed to load risk analytics data.'))
      .finally(() => setBusy(false));
  };

  useEffect(() => {
    loadData();
  }, []);

  // Compute normalized severity distribution
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

  // Data for the severity pie chart (excluding zero values to avoid distortion)
  const severityChartData = useMemo(() => {
    const order = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'UNKNOWN'];
    return order
      .map((name) => ({
        name,
        count: severityCounts[name] || 0
      }))
      .filter((item) => item.count > 0);
  }, [severityCounts]);

  // Aggregate recalls by product category (resolved from nested product or productId)
  const categoryData = useMemo(() => {
    const counts = {};
    r.forEach((recall) => {
      let cat = resolveProductCategory(recall, p);
      if (!cat || cat === 'Uncategorized') {
        cat = 'Unlinked / Uncategorized';
      }
      counts[cat] = (counts[cat] || 0) + 1;
    });

    return Object.entries(counts)
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => b.count - a.count);
  }, [r, p]);

  // Monthly timeline trend
  const timelineData = useMemo(() => {
    const monthly = {};
    r.forEach((item) => {
      if (!item.recallDate) return;
      const d = new Date(item.recallDate);
      if (Number.isNaN(d.getTime())) return;
      const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
      monthly[key] = (monthly[key] || 0) + 1;
    });

    return Object.keys(monthly)
      .sort()
      .slice(-6)
      .map((k) => ({
        month: k,
        recalls: monthly[k]
      }));
  }, [r]);

  const criticalCount = severityCounts.CRITICAL || 0;
  const highCount = severityCounts.HIGH || 0;
  const activeCount = r.filter(
    (x) => String(x.status || 'ACTIVE').toUpperCase() === 'ACTIVE'
  ).length;

  const totalRecalls = r.length;

  return (
    <>
      <PageHeading
        eyebrow="RISK MODELLING & INTELLIGENCE"
        title="Risk Analytics"
        description="Comprehensive analysis of product recall severity distributions, affected categories, and timeline trends."
        action={
          <button className="secondary" onClick={loadData} type="button" title="Refresh analytics">
            <RefreshCw size={15} /> Refresh
          </button>
        }
      />

      {error && <p className="error">{error}</p>}

      <div className="stats four">
        <StatCard
          label="Total recalls recorded"
          value={busy ? '…' : totalRecalls}
          note="Full dataset count"
        />
        <StatCard
          label="Critical tier notices"
          value={busy ? '…' : criticalCount}
          note="Requires immediate action"
        />
        <StatCard
          label="High severity notices"
          value={busy ? '…' : highCount}
          note="Serious safety risks"
        />
        <StatCard
          label="Active enforcements"
          value={busy ? '…' : activeCount}
          note="Currently in enforcement"
        />
      </div>

      <div className="charts">
        {/* Severity Distribution Pie Chart */}
        <section className="panel">
          <div className="toolbar">
            <div>
              <h3>Severity risk distribution</h3>
              <p>Proportion of recall notices across normalized severity tiers.</p>
            </div>
          </div>
          <div className="chart" style={{ height: '300px' }}>
            {severityChartData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={severityChartData}
                    dataKey="count"
                    nameKey="name"
                    innerRadius={55}
                    outerRadius={92}
                    paddingAngle={3}
                  >
                    {severityChartData.map((entry) => (
                      <Cell
                        key={`cell-${entry.name}`}
                        fill={SEVERITY_COLORS[entry.name] || '#94a3b8'}
                      />
                    ))}
                  </Pie>
                  <Tooltip formatter={(val, name) => [`${val} notices`, name]} />
                  <Legend verticalAlign="bottom" iconType="circle" height={36} />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div className="empty">No severity records to display.</div>
            )}
          </div>
        </section>

        {/* Recalls by Product Category Bar Chart */}
        <section className="panel">
          <div className="toolbar">
            <div>
              <h3>Recalls by product category</h3>
              <p>Risk concentration across registered product sectors.</p>
            </div>
          </div>
          <div className="chart" style={{ height: '300px' }}>
            {categoryData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={categoryData}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="name" />
                  <YAxis allowDecimals={false} />
                  <Tooltip formatter={(val) => [`${val} recalls`, 'Recalls']} />
                  <Bar
                    dataKey="count"
                    name="Recalls"
                    fill="#536fe8"
                    radius={[5, 5, 0, 0]}
                  />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div className="empty">No category data available.</div>
            )}
          </div>
        </section>
      </div>

      {/* Monthly Timeline Trend (if dates exist) */}
      {timelineData.length > 0 && (
        <section className="panel" style={{ marginTop: '20px' }}>
          <div className="toolbar">
            <div>
              <h3>Recent recall notice timeline</h3>
              <p>Notice volume by month based on verified publication dates.</p>
            </div>
          </div>
          <div className="chart" style={{ height: '260px' }}>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={timelineData}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="month" />
                <YAxis allowDecimals={false} />
                <Tooltip formatter={(val) => [`${val} notices`, 'Notices']} />
                <Bar dataKey="recalls" name="Notices" fill="#3b82f6" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </section>
      )}

      {/* Breakdown Summary Metrics Table */}
      <section className="panel" style={{ marginTop: '20px' }}>
        <div className="toolbar">
          <div>
            <h3>Severity breakdown summary</h3>
            <p>Accurate normalized distribution counts and share of total register.</p>
          </div>
        </div>
        <div className="stats" style={{ marginBottom: 0 }}>
          {['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'UNKNOWN'].map((tier) => {
            const count = severityCounts[tier] || 0;
            const pct = totalRecalls > 0 ? ((count / totalRecalls) * 100).toFixed(1) : '0.0';
            return (
              <article className="stat" key={tier}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <small style={{ fontWeight: 700, color: SEVERITY_COLORS[tier] }}>{tier}</small>
                  <small className="muted">{pct}%</small>
                </div>
                <strong>{count}</strong>
                <p>
                  {tier === 'UNKNOWN'
                    ? 'Unclassified / pending source data'
                    : `${tier.toLowerCase()} risk category`}
                </p>
              </article>
            );
          })}
        </div>
      </section>

      {/* Automated Risk Insights Card */}
      <section className="panel" style={{ marginTop: '20px' }}>
        <div className="toolbar">
          <div>
            <h3>Executive risk insights</h3>
            <p>Automated summary based on active database records.</p>
          </div>
        </div>
        <p style={{ lineHeight: 1.6, fontSize: '0.95rem' }}>
          {totalRecalls
            ? `RecallRadar is currently tracking ${totalRecalls} total product recall records, of which ${activeCount} (${((activeCount / totalRecalls) * 100).toFixed(1)}%) remain actively enforced. ` +
              `There are ${criticalCount} critical-tier and ${highCount} high-severity notices demanding priority response. ` +
              `The leading affected categories are ${categoryData
                .slice(0, 3)
                .map((c) => `${c.name} (${c.count})`)
                .join(', ') || 'Uncategorized'}.`
            : 'Populate product and recall records to generate automated intelligence insights.'}
        </p>
      </section>
    </>
  );
}
