import React from 'react';
import { Navigate, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ShieldAlert, ArrowLeft } from 'lucide-react';

export default function ProtectedRoute({ children, allowedRoles }) {
  const { isAuthenticated, role, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '60vh',
        gap: '12px'
      }}>
        <div className="spinner" style={{
          width: '36px',
          height: '36px',
          border: '3px solid var(--border)',
          borderTopColor: '#4f46e5',
          borderRadius: '50%',
          animation: 'spin 0.8s linear infinite'
        }} />
        <span style={{ fontSize: '0.9rem', color: 'var(--muted)', fontWeight: 500 }}>
          Verifying security credentials...
        </span>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (allowedRoles && allowedRoles.length > 0) {
    const normalizedAllowed = allowedRoles.map((r) =>
      r.startsWith('ROLE_') ? r.substring(5).toUpperCase() : r.toUpperCase()
    );
    const userRole = role ? role.toUpperCase() : 'VIEWER';

    if (!normalizedAllowed.includes(userRole)) {
      return (
        <div style={{
          maxWidth: '560px',
          margin: '60px auto',
          padding: '32px',
          background: 'var(--panel)',
          border: '1px solid var(--border)',
          borderRadius: '12px',
          textAlign: 'center',
          boxShadow: '0 4px 20px rgba(0,0,0,0.1)'
        }}>
          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: '56px',
            height: '56px',
            borderRadius: '50%',
            background: 'rgba(239, 68, 68, 0.12)',
            color: '#dc2626',
            marginBottom: '16px'
          }}>
            <ShieldAlert size={28} />
          </div>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 700, margin: '0 0 8px 0', color: 'var(--text)' }}>
            Access Restricted (403 Forbidden)
          </h2>
          <p style={{ fontSize: '0.88rem', color: 'var(--muted)', lineHeight: 1.5, marginBottom: '24px' }}>
            Your account role (<b>{userRole}</b>) does not have sufficient administrative permissions to access this view.
            Contact an organization administrator if you require elevated privileges.
          </p>
          <div style={{ display: 'flex', justifyContent: 'center', gap: '12px' }}>
            <Link
              to="/"
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
                padding: '9px 16px',
                background: '#4f46e5',
                color: '#fff',
                borderRadius: '8px',
                fontSize: '0.85rem',
                fontWeight: 600,
                textDecoration: 'none'
              }}
            >
              <ArrowLeft size={16} /> Return to Dashboard
            </Link>
          </div>
        </div>
      );
    }
  }

  return children;
}
