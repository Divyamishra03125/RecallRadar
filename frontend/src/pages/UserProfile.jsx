import React from 'react';
import { useAuth } from '../context/AuthContext';
import PageHeading from '../components/PageHeading';
import { Shield, User, Mail, Calendar, Clock, CheckCircle2, XCircle, ShieldCheck } from 'lucide-react';

export default function UserProfile() {
  const { user, role, isAdmin, isAnalyst, isViewer } = useAuth();

  const permissions = [
    { name: 'Browse Product Catalogue', admin: true, analyst: true, viewer: true },
    { name: 'Search & Inspect Recall Notices', admin: true, analyst: true, viewer: true },
    { name: 'Grounded AI Safety Assistant', admin: true, analyst: true, viewer: true },
    { name: 'View Real-time Risk Analytics', admin: true, analyst: true, viewer: true },
    { name: 'Generate & Export Risk Reports', admin: true, analyst: true, viewer: true },
    { name: 'Register New Products in Catalogue', admin: true, analyst: false, viewer: false },
    { name: 'Edit & Update Product Details', admin: true, analyst: false, viewer: false },
    { name: 'Delete Registered Products', admin: true, analyst: false, viewer: false },
    { name: 'Publish New Recall Notices', admin: true, analyst: true, viewer: false },
    { name: 'Edit Recall Details & Hazards', admin: true, analyst: true, viewer: false },
    { name: 'Reclassify Recall Severity & Evidence', admin: true, analyst: true, viewer: false },
    { name: 'Link / Associate Products with Recalls', admin: true, analyst: true, viewer: false },
    { name: 'Delete Recall Records', admin: true, analyst: false, viewer: false },
    { name: 'Trigger CPSC Feed Ingestion', admin: true, analyst: false, viewer: false },
    { name: 'Admin User Management & Role Assignment', admin: true, analyst: false, viewer: false },
    { name: 'Enable / Disable User Accounts', admin: true, analyst: false, viewer: false }
  ];

  return (
    <div className="page-container" style={{ padding: '0 0 40px 0' }}>
      <PageHeading
        title="User Profile & Security Information"
        description="Inspect authenticated enterprise credentials, account lifecycle metadata, and active role-based permission tiers."
      />

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '20px', marginBottom: '24px' }}>
        {/* Account Metadata Card */}
        <div className="card" style={{ padding: '24px', background: 'var(--panel)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '20px' }}>
            <div style={{
              width: '54px',
              height: '54px',
              borderRadius: '50%',
              background: 'linear-gradient(135deg, #4f46e5 0%, #3730a3 100%)',
              color: '#ffffff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '1.25rem',
              fontWeight: 800,
              boxShadow: '0 4px 14px rgba(79, 70, 229, 0.3)'
            }}>
              {user?.username ? user.username.slice(0, 2).toUpperCase() : 'RR'}
            </div>
            <div>
              <h3 style={{ margin: '0 0 4px 0', fontSize: '1.2rem', fontWeight: 700, color: 'var(--text)' }}>
                {user?.fullName || user?.username}
              </h3>
              <div style={{ fontSize: '0.84rem', color: 'var(--muted)' }}>@{user?.username}</div>
            </div>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', fontSize: '0.86rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', color: 'var(--text)' }}>
              <Mail size={16} style={{ color: 'var(--muted)' }} />
              <span style={{ color: 'var(--muted)' }}>Email:</span>
              <span style={{ fontWeight: 600 }}>{user?.email || '—'}</span>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', color: 'var(--text)' }}>
              <ShieldCheck size={16} style={{ color: '#4f46e5' }} />
              <span style={{ color: 'var(--muted)' }}>Assigned Role:</span>
              <span style={{
                padding: '2px 8px',
                borderRadius: '12px',
                fontSize: '0.76rem',
                fontWeight: 700,
                background: isAdmin ? 'rgba(16, 185, 129, 0.15)' : isAnalyst ? 'rgba(99, 102, 241, 0.15)' : 'rgba(148, 163, 184, 0.15)',
                color: isAdmin ? '#10b981' : isAnalyst ? '#818cf8' : '#94a3b8'
              }}>
                {role}
              </span>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', color: 'var(--text)' }}>
              <Calendar size={16} style={{ color: 'var(--muted)' }} />
              <span style={{ color: 'var(--muted)' }}>Member Since:</span>
              <span style={{ fontWeight: 500 }}>
                {user?.createdAt ? new Date(user.createdAt).toLocaleDateString() : 'Active session'}
              </span>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', color: 'var(--text)' }}>
              <Clock size={16} style={{ color: 'var(--muted)' }} />
              <span style={{ color: 'var(--muted)' }}>Last Login:</span>
              <span style={{ fontWeight: 500 }}>
                {user?.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString() : 'Current Session'}
              </span>
            </div>
          </div>
        </div>

        {/* Security Tier Overview Card */}
        <div className="card" style={{ padding: '24px', background: 'var(--panel)' }}>
          <h4 style={{ margin: '0 0 12px 0', fontSize: '1rem', fontWeight: 700, color: 'var(--text)' }}>
            Role Tier Description
          </h4>
          <p style={{ fontSize: '0.84rem', color: 'var(--muted)', lineHeight: 1.5, marginBottom: '16px' }}>
            {isAdmin && 'You possess full administrative credentials. You are authorized to manage product catalogues, publish/delete recall notices, adjust risk severity evaluations, and oversee team user authorization levels.'}
            {isAnalyst && 'You possess Risk Analyst operational credentials. You can publish and update recall notices, re-evaluate hazard severities with audit justification, and link candidate products.'}
            {isViewer && 'You possess standard Catalogue Viewer credentials. You have read-only access to browse products, review recall notices, explore risk analytics, export reports, and consult RecallRadar AI.'}
          </p>

          <div style={{
            padding: '12px 14px',
            borderRadius: '8px',
            background: 'var(--bg)',
            border: '1px solid var(--border)',
            fontSize: '0.82rem',
            lineHeight: 1.5
          }}>
            <b>Security Note:</b> Role assignments are strictly enforced at the backend REST API layer via Spring Security and Java method authorization. Privilege elevation can only be executed by an active Administrator.
          </div>
        </div>
      </div>

      {/* Role Permission Matrix Table */}
      <div className="card" style={{ padding: 0, overflow: 'hidden', background: 'var(--panel)' }}>
        <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--border)' }}>
          <h3 style={{ margin: 0, fontSize: '1rem', fontWeight: 700, color: 'var(--text)' }}>
            Role-Based Authorization Matrix
          </h3>
          <span style={{ fontSize: '0.78rem', color: 'var(--muted)' }}>
            Comparative capability matrix across all three enterprise security roles
          </span>
        </div>

        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.84rem' }}>
            <thead>
              <tr style={{ background: 'var(--subtle-bg, rgba(0,0,0,0.03))', borderBottom: '1px solid var(--border)' }}>
                <th style={{ padding: '10px 18px', textAlign: 'left', fontWeight: 600, color: 'var(--muted)' }}>Feature / Permission</th>
                <th style={{ padding: '10px 18px', textAlign: 'center', fontWeight: 600, color: '#10b981' }}>ADMIN</th>
                <th style={{ padding: '10px 18px', textAlign: 'center', fontWeight: 600, color: '#818cf8' }}>ANALYST</th>
                <th style={{ padding: '10px 18px', textAlign: 'center', fontWeight: 600, color: '#94a3b8' }}>VIEWER</th>
              </tr>
            </thead>
            <tbody>
              {permissions.map((p, idx) => (
                <tr key={idx} style={{ borderBottom: '1px solid var(--border)' }}>
                  <td style={{ padding: '10px 18px', color: 'var(--text)', fontWeight: 500 }}>
                    {p.name}
                  </td>
                  <td style={{ padding: '10px 18px', textAlign: 'center' }}>
                    {p.admin ? <CheckCircle2 size={16} color="#10b981" style={{ margin: 'auto' }} /> : <XCircle size={16} color="#ef4444" style={{ margin: 'auto' }} />}
                  </td>
                  <td style={{ padding: '10px 18px', textAlign: 'center' }}>
                    {p.analyst ? <CheckCircle2 size={16} color="#818cf8" style={{ margin: 'auto' }} /> : <XCircle size={16} color="#ef4444" style={{ margin: 'auto' }} />}
                  </td>
                  <td style={{ padding: '10px 18px', textAlign: 'center' }}>
                    {p.viewer ? <CheckCircle2 size={16} color="#94a3b8" style={{ margin: 'auto' }} /> : <XCircle size={16} color="#ef4444" style={{ margin: 'auto' }} />}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
