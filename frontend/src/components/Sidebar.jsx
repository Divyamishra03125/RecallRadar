import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Boxes,
  Package,
  BarChart3,
  Bell,
  FileText,
  Settings,
  ShieldCheck,
  Sparkles,
  Users,
  X
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';

const baseLinks = [
  ['Dashboard', '/', LayoutDashboard],
  ['Products', '/products', Boxes],
  ['Product recalls', '/recalls', Package],
  ['Risk analytics', '/analytics', BarChart3],
  ['Recall monitoring', '/monitoring', Bell],
  ['Reports', '/reports', FileText],
  ['Settings', '/settings', Settings]
];

export default function Sidebar({ open, onClose }) {
  const { isAdmin } = useAuth();

  const handleOpenChatbot = (e) => {
    e.preventDefault();
    if (onClose) onClose();
    window.dispatchEvent(new CustomEvent('recallradar:open-chat'));
  };

  const navLinks = [...baseLinks];
  if (isAdmin) {
    // Insert User Management before Settings for Admins
    navLinks.splice(6, 0, ['User management', '/admin/users', Users]);
  }

  return (
    <>
      {open && (
        <button
          className="sidebar-scrim"
          aria-label="Close menu"
          onClick={onClose}
          type="button"
        />
      )}
      <aside className={`sidebar ${open ? 'sidebar-open' : ''}`}>
        <div className="brand">
          <span className="brand-mark">
            <ShieldCheck size={22} />
          </span>
          <span className="brand-text">
            <b>RecallRadar</b>
            <small>SAFETY INTELLIGENCE</small>
          </span>
          <button
            className="sidebar-close"
            onClick={onClose}
            aria-label="Close sidebar"
            type="button"
          >
            <X size={18} />
          </button>
        </div>

        <div className="workspace-card">
          <span className="workspace-icon">R</span>
          <span className="workspace-text">
            <b>RecallRadar Workspace</b>
            <small>Enterprise intelligence</small>
          </span>
        </div>

        <span className="sidebar-label">WORKSPACE</span>

        <nav>
          {navLinks.map(([label, path, Icon]) => (
            <NavLink
              key={path}
              to={path}
              end={path === '/'}
              onClick={onClose}
              className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
            >
              <Icon size={18} />
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-promo">
          <Sparkles size={18} />
          <b>Stay ahead of risk</b>
          <p>Investigate product recall information faster with AI.</p>
          <button
            type="button"
            onClick={handleOpenChatbot}
            style={{
              background: 'none',
              border: 'none',
              padding: 0,
              color: '#4f46e5',
              fontWeight: 600,
              fontSize: '0.84rem',
              cursor: 'pointer',
              textAlign: 'left'
            }}
          >
            Launch RecallRadar AI →
          </button>
        </div>

        <div className="sidebar-bottom">Product safety intelligence</div>
      </aside>
    </>
  );
}
