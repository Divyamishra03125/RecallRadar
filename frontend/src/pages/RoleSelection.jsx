import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  ShieldCheck,
  ShieldAlert,
  Search,
  Eye,
  ArrowRight,
  Users,
  AlertTriangle,
  FileText,
  Sparkles,
  Lock
} from 'lucide-react';

export default function RoleSelection() {
  const navigate = useNavigate();

  const portals = [
    {
      id: 'admin',
      role: 'ADMIN',
      title: 'Administrator Portal',
      subtitle: 'Complete administrative governance and user access control.',
      path: '/login/admin',
      color: '#10b981',
      bgGlow: 'rgba(16, 185, 129, 0.12)',
      borderAccent: 'rgba(16, 185, 129, 0.35)',
      icon: ShieldAlert,
      features: [
        'Full administrative access & CRUD controls',
        'Manage team accounts & role assignments',
        'Direct product catalogue & recall management',
        'Review severity audits & system settings'
      ],
      badge: 'ADMIN ACCESS',
      ctaText: 'Enter Admin Portal'
    },
    {
      id: 'analyst',
      role: 'ANALYST',
      title: 'Risk Analyst Portal',
      subtitle: 'Operational investigation, hazard evaluation, and recall authoring.',
      path: '/login/analyst',
      color: '#818cf8',
      bgGlow: 'rgba(99, 102, 241, 0.12)',
      borderAccent: 'rgba(99, 102, 241, 0.35)',
      icon: Search,
      features: [
        'Product safety investigation & monitoring',
        'Author & update regulatory recall notices',
        'Re-evaluate hazard severity with evidence audit',
        'Cross-reference candidate product matches'
      ],
      badge: 'ANALYST ACCESS',
      ctaText: 'Enter Analyst Portal'
    },
    {
      id: 'viewer',
      role: 'VIEWER',
      title: 'Catalogue Viewer Portal',
      subtitle: 'Read-only safety intelligence, recall notices, and AI assistance.',
      path: '/login/viewer',
      color: '#38bdf8',
      bgGlow: 'rgba(56, 189, 248, 0.12)',
      borderAccent: 'rgba(56, 189, 248, 0.35)',
      icon: Eye,
      features: [
        'Read-only product catalogue exploration',
        'Search verified regulatory recall notices',
        'Live risk distribution & category analytics',
        'Grounded AI Safety Assistant inquiries'
      ],
      badge: 'VIEWER ACCESS',
      ctaText: 'Enter Viewer Portal'
    }
  ];

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      flexDirection: 'column',
      justifyContent: 'center',
      alignItems: 'center',
      padding: '40px 20px',
      background: 'linear-gradient(135deg, #090d16 0%, #0f172a 50%, #1e1b4b 100%)',
      color: '#f8fafc',
      fontFamily: 'Inter, system-ui, -apple-system, sans-serif'
    }}>
      {/* Brand Header */}
      <div style={{ textAlign: 'center', marginBottom: '36px', maxWidth: '640px' }}>
        <div style={{
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          width: '56px',
          height: '56px',
          borderRadius: '16px',
          background: 'linear-gradient(135deg, #4f46e5 0%, #3730a3 100%)',
          color: '#ffffff',
          boxShadow: '0 8px 24px rgba(79, 70, 229, 0.35)',
          marginBottom: '14px'
        }}>
          <ShieldCheck size={32} />
        </div>
        <h1 style={{ fontSize: '1.9rem', fontWeight: 800, margin: '0 0 6px 0', letterSpacing: '-0.02em', color: '#ffffff' }}>
          RecallRadar Access Gateways
        </h1>
        <p style={{ fontSize: '0.92rem', color: '#94a3b8', margin: 0, lineHeight: 1.5 }}>
          Select your designated enterprise workspace portal to authenticate. Each portal enforces strict database-verified role authorization.
        </p>
      </div>

      {/* Portals Grid */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 340px))',
        gap: '24px',
        width: '100%',
        maxWidth: '1080px',
        justifyContent: 'center',
        marginBottom: '36px'
      }}>
        {portals.map((p) => {
          const IconComponent = p.icon;
          return (
            <div
              key={p.id}
              onClick={() => navigate(p.path)}
              style={{
                background: '#0f172a',
                border: `1px solid ${p.borderAccent}`,
                borderRadius: '16px',
                padding: '28px 24px',
                boxShadow: '0 12px 30px rgba(0, 0, 0, 0.35)',
                display: 'flex',
                flexDirection: 'column',
                cursor: 'pointer',
                transition: 'transform 0.2s, box-shadow 0.2s, border-color 0.2s',
                position: 'relative',
                overflow: 'hidden'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.transform = 'translateY(-4px)';
                e.currentTarget.style.boxShadow = `0 18px 36px ${p.bgGlow}`;
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.transform = 'translateY(0)';
                e.currentTarget.style.boxShadow = '0 12px 30px rgba(0, 0, 0, 0.35)';
              }}
            >
              {/* Header: Icon & Badge */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '18px' }}>
                <div style={{
                  width: '46px',
                  height: '46px',
                  borderRadius: '12px',
                  background: p.bgGlow,
                  color: p.color,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  border: `1px solid ${p.borderAccent}`
                }}>
                  <IconComponent size={24} />
                </div>
                <span style={{
                  fontSize: '0.7rem',
                  fontWeight: 700,
                  letterSpacing: '0.04em',
                  padding: '3px 8px',
                  borderRadius: '12px',
                  background: p.bgGlow,
                  color: p.color,
                  border: `1px solid ${p.borderAccent}`
                }}>
                  {p.badge}
                </span>
              </div>

              {/* Title & Subtitle */}
              <h2 style={{ fontSize: '1.25rem', fontWeight: 700, margin: '0 0 6px 0', color: '#ffffff' }}>
                {p.title}
              </h2>
              <p style={{ fontSize: '0.82rem', color: '#94a3b8', margin: '0 0 20px 0', lineHeight: 1.45, flex: '0 0 auto' }}>
                {p.subtitle}
              </p>

              {/* Feature List */}
              <div style={{
                flex: 1,
                borderTop: '1px solid rgba(255, 255, 255, 0.08)',
                paddingTop: '16px',
                marginBottom: '22px'
              }}>
                <div style={{ fontSize: '0.74rem', fontWeight: 600, color: '#cbd5e1', textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '10px' }}>
                  Workspace Capabilities
                </div>
                <ul style={{ margin: 0, paddingLeft: '16px', fontSize: '0.8rem', color: '#94a3b8', lineHeight: 1.6 }}>
                  {p.features.map((feat, idx) => (
                    <li key={idx} style={{ marginBottom: '4px' }}>
                      {feat}
                    </li>
                  ))}
                </ul>
              </div>

              {/* Enter Button */}
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  navigate(p.path);
                }}
                style={{
                  width: '100%',
                  padding: '11px 16px',
                  borderRadius: '8px',
                  border: `1px solid ${p.borderAccent}`,
                  background: p.bgGlow,
                  color: p.color,
                  fontSize: '0.88rem',
                  fontWeight: 600,
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '8px',
                  transition: 'background 0.2s'
                }}
              >
                <span>{p.ctaText}</span>
                <ArrowRight size={16} />
              </button>
            </div>
          );
        })}
      </div>

      {/* Footer / Registration Note */}
      <div style={{
        textAlign: 'center',
        fontSize: '0.86rem',
        color: '#94a3b8'
      }}>
        Need a standard catalogue account?{' '}
        <Link
          to="/register"
          style={{ color: '#818cf8', fontWeight: 600, textDecoration: 'none' }}
        >
          Create Viewer Account
        </Link>
      </div>
    </div>
  );
}
