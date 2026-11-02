import React, { useState, useRef, useEffect } from 'react';
import { Menu, Moon, Sun, Shield, LogOut, User, Users } from 'lucide-react';
import { useLocation, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const names = {
  '/': 'Dashboard',
  '/products': 'Products',
  '/recalls': 'Product recalls',
  '/analytics': 'Risk analytics',
  '/monitoring': 'Recall monitoring',
  '/reports': 'Reports',
  '/settings': 'Settings',
  '/profile': 'User Profile',
  '/admin/users': 'User Management'
};

export default function Header({ dark, toggle, onToggleTheme, menu, onMenu }) {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, role, isAdmin, logout } = useAuth();
  const handleToggle = onToggleTheme || toggle;
  const handleMenu = onMenu || menu;

  const [menuOpen, setMenuOpen] = useState(false);
  const dropdownRef = useRef(null);

  useEffect(() => {
    function handleClickOutside(event) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setMenuOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleLogout = () => {
    setMenuOpen(false);
    logout();
    navigate('/login', { replace: true });
  };

  const getRoleBadgeStyle = (r) => {
    const roleUpper = (r || 'VIEWER').toUpperCase();
    if (roleUpper === 'ADMIN') {
      return { background: 'rgba(16, 185, 129, 0.15)', color: '#10b981', border: '1px solid rgba(16, 185, 129, 0.3)' };
    }
    if (roleUpper === 'ANALYST') {
      return { background: 'rgba(99, 102, 241, 0.15)', color: '#818cf8', border: '1px solid rgba(99, 102, 241, 0.3)' };
    }
    return { background: 'rgba(148, 163, 184, 0.15)', color: '#94a3b8', border: '1px solid rgba(148, 163, 184, 0.3)' };
  };

  const roleBadgeStyle = getRoleBadgeStyle(role);

  return (
    <header className="topbar">
      <button
        className="mobile-menu icon-button"
        onClick={handleMenu}
        aria-label="Open navigation"
        type="button"
      >
        <Menu size={20} />
      </button>

      <div className="breadcrumb">
        <span>Workspace</span>
        <span className="separator">/</span>
        <b>{names[location.pathname] || 'Workspace'}</b>
      </div>

      <div className="topbar-actions" style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
        {/* Read-Only Verified Role Badge (Cannot be switched arbitrarily by client) */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            fontSize: '0.78rem',
            padding: '3px 10px',
            borderRadius: '16px',
            fontWeight: 700,
            letterSpacing: '0.02em',
            ...roleBadgeStyle
          }}
          title={`Verified server authorization role: ${role}`}
        >
          <Shield size={12} />
          <span>{role || 'VIEWER'}</span>
        </div>

        {/* Theme Toggle */}
        <button
          className="icon-button theme-toggle"
          onClick={handleToggle}
          aria-label="Toggle theme"
          title={dark ? 'Switch to light mode' : 'Switch to dark mode'}
          type="button"
        >
          {dark ? <Sun size={18} /> : <Moon size={18} />}
        </button>

        {/* User Profile Dropdown Menu */}
        <div style={{ position: 'relative' }} ref={dropdownRef}>
          <button
            type="button"
            onClick={() => setMenuOpen((v) => !v)}
            className="avatar"
            style={{
              cursor: 'pointer',
              border: 'none',
              background: '#4f46e5',
              color: '#ffffff',
              fontWeight: 700,
              fontSize: '0.8rem',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              outline: 'none'
            }}
            title={`${user?.fullName || user?.username || 'User'} (${role})`}
            aria-expanded={menuOpen}
          >
            {user?.username ? user.username.slice(0, 2).toUpperCase() : 'RR'}
          </button>

          {menuOpen && (
            <div
              style={{
                position: 'absolute',
                right: 0,
                top: 'calc(100% + 8px)',
                width: '230px',
                background: 'var(--panel)',
                border: '1px solid var(--border)',
                borderRadius: '12px',
                boxShadow: '0 12px 30px rgba(0,0,0,0.25)',
                padding: '12px 0',
                zIndex: 500,
                display: 'flex',
                flexDirection: 'column'
              }}
            >
              {/* User Identity Header */}
              <div style={{ padding: '0 16px 10px 16px', borderBottom: '1px solid var(--border)' }}>
                <div style={{ fontWeight: 700, fontSize: '0.88rem', color: 'var(--text)' }}>
                  {user?.fullName || user?.username}
                </div>
                <div style={{ fontSize: '0.76rem', color: 'var(--muted)', wordBreak: 'break-all' }}>
                  {user?.email || `@${user?.username}`}
                </div>
                <div style={{ marginTop: '6px' }}>
                  <span style={{
                    display: 'inline-block',
                    padding: '2px 8px',
                    borderRadius: '10px',
                    fontSize: '0.7rem',
                    fontWeight: 700,
                    ...roleBadgeStyle
                  }}>
                    {role} ACCESS
                  </span>
                </div>
              </div>

              {/* Menu Items */}
              <div style={{ padding: '6px 0' }}>
                <Link
                  to="/profile"
                  onClick={() => setMenuOpen(false)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '10px',
                    padding: '8px 16px',
                    color: 'var(--text)',
                    textDecoration: 'none',
                    fontSize: '0.84rem'
                  }}
                  className="dropdown-item"
                >
                  <User size={15} style={{ color: 'var(--muted)' }} />
                  <span>My Profile</span>
                </Link>

                {isAdmin && (
                  <Link
                    to="/admin/users"
                    onClick={() => setMenuOpen(false)}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '10px',
                      padding: '8px 16px',
                      color: 'var(--text)',
                      textDecoration: 'none',
                      fontSize: '0.84rem'
                    }}
                    className="dropdown-item"
                  >
                    <Users size={15} style={{ color: '#10b981' }} />
                    <span>User Management</span>
                  </Link>
                )}
              </div>

              {/* Logout Button */}
              <div style={{ borderTop: '1px solid var(--border)', paddingTop: '6px' }}>
                <button
                  type="button"
                  onClick={handleLogout}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '10px',
                    width: '100%',
                    padding: '8px 16px',
                    background: 'none',
                    border: 'none',
                    color: '#ef4444',
                    textAlign: 'left',
                    fontSize: '0.84rem',
                    fontWeight: 600,
                    cursor: 'pointer'
                  }}
                  className="dropdown-item"
                >
                  <LogOut size={15} />
                  <span>Sign Out</span>
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
