import React, { useState, useEffect, useCallback } from 'react';
import { adminApi } from '../services/api';
import { useAuth } from '../context/AuthContext';
import PageHeading from '../components/PageHeading';
import {
  Users,
  Search,
  Filter,
  Shield,
  ShieldCheck,
  ShieldAlert,
  UserCheck,
  UserX,
  History,
  AlertTriangle,
  X,
  RefreshCw,
  Check,
  UserPlus,
  Edit2,
  Eye,
  Lock,
  Mail,
  User,
  Activity,
  Layers,
  CheckCircle2,
  XCircle,
  Clock,
  Building2,
  Briefcase,
  ThumbsUp,
  ThumbsDown,
  FileText
} from 'lucide-react';

export default function AdminUsers() {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Live Statistics
  const [stats, setStats] = useState({
    totalUsers: 0,
    activeUsers: 0,
    disabledUsers: 0,
    adminCount: 0,
    analystCount: 0,
    viewerCount: 0,
    pendingRequestsCount: 0
  });

  // Active Tab: 'DIRECTORY' | 'ANALYST_REQUESTS'
  const [activeTab, setActiveTab] = useState('DIRECTORY');

  // Analyst Access Requests State
  const [analystRequests, setAnalystRequests] = useState([]);
  const [requestsLoading, setRequestsLoading] = useState(false);
  const [requestStatusFilter, setRequestStatusFilter] = useState('PENDING_APPROVAL');
  const [selectedRequestDetails, setSelectedRequestDetails] = useState(null);
  const [rejectingRequest, setRejectingRequest] = useState(null);
  const [rejectionReason, setRejectionReason] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  // Create User Modal
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [createForm, setCreateForm] = useState({
    fullName: '',
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
    role: 'ANALYST',
    enabled: true
  });
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState('');

  // Edit User Modal
  const [editUser, setEditUser] = useState(null);
  const [editForm, setEditForm] = useState({
    fullName: '',
    email: '',
    role: 'ANALYST',
    enabled: true,
    password: ''
  });
  const [editing, setEditing] = useState(false);
  const [editError, setEditError] = useState('');

  // Details Modal
  const [detailsUser, setDetailsUser] = useState(null);

  // Role change modal
  const [roleModalUser, setRoleModalUser] = useState(null);
  const [newRoleSelection, setNewRoleSelection] = useState('');
  const [roleUpdating, setRoleUpdating] = useState(false);

  // Status toggle confirmation modal
  const [statusModalUser, setStatusModalUser] = useState(null);
  const [statusUpdating, setStatusUpdating] = useState(false);

  // Audit history modal
  const [auditUser, setAuditUser] = useState(null);
  const [auditLogs, setAuditLogs] = useState([]);
  const [auditLoading, setAuditLoading] = useState(false);

  // Fetch Stats from PostgreSQL
  const fetchStats = async () => {
    try {
      const res = await adminApi.getStats();
      if (res?.data) {
        setStats(res.data);
      }
    } catch (err) {
      console.error('Failed to load user statistics', err);
    }
  };

  // Fetch Users from PostgreSQL
  const fetchUsers = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await adminApi.getUsers({
        search: search.trim() || undefined,
        role: roleFilter !== 'ALL' ? roleFilter : undefined,
        status: statusFilter !== 'ALL' ? statusFilter : undefined,
        page,
        size: 15
      });
      const data = res.data;
      setUsers(data?.content || []);
      setTotalPages(data?.totalPages || 1);
      setTotalElements(data?.totalElements || 0);
    } catch (err) {
      setError(err.message || 'Failed to load user directory.');
    } finally {
      setLoading(false);
    }
  }, [search, roleFilter, statusFilter, page]);

  // Fetch Analyst Requests from PostgreSQL
  const fetchAnalystRequests = useCallback(async () => {
    setRequestsLoading(true);
    try {
      const res = await adminApi.getAnalystRequests({
        status: requestStatusFilter === 'ALL' ? undefined : requestStatusFilter,
        page: 0,
        size: 50
      });
      setAnalystRequests(res.data?.content || []);
    } catch (err) {
      console.error('Failed to load analyst requests', err);
    } finally {
      setRequestsLoading(false);
    }
  }, [requestStatusFilter]);

  // Handle Approve Request
  const handleApproveRequest = async (id, fullName) => {
    setActionLoading(true);
    setError('');
    setSuccess('');
    try {
      await adminApi.approveAnalystRequest(id);
      setSuccess(`Analyst access request approved for ${fullName}. Account is now ACTIVE and can sign in.`);
      fetchAnalystRequests();
      fetchStats();
      fetchUsers();
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to approve request');
    } finally {
      setActionLoading(false);
    }
  };

  // Handle Reject Request Submit
  const handleRejectSubmit = async (e) => {
    e.preventDefault();
    if (!rejectingRequest) return;
    setActionLoading(true);
    setError('');
    setSuccess('');
    try {
      await adminApi.rejectAnalystRequest(rejectingRequest.id, {
        rejectionReason: rejectionReason.trim() || undefined
      });
      setSuccess(`Analyst access request for ${rejectingRequest.fullName} was rejected.`);
      setRejectingRequest(null);
      setRejectionReason('');
      fetchAnalystRequests();
      fetchStats();
      fetchUsers();
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to reject request');
    } finally {
      setActionLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
    fetchStats();
  }, [fetchUsers]);

  useEffect(() => {
    if (activeTab === 'ANALYST_REQUESTS') {
      fetchAnalystRequests();
    }
  }, [activeTab, fetchAnalystRequests]);

  // Handle Create User Submit
  const handleCreateSubmit = async (e) => {
    e.preventDefault();
    setCreateError('');

    if (!createForm.fullName.trim()) {
      setCreateError('Full name is required.');
      return;
    }
    if (!createForm.username.trim() || createForm.username.trim().length < 3) {
      setCreateError('Username must be at least 3 characters.');
      return;
    }
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!createForm.email.trim() || !emailRegex.test(createForm.email.trim())) {
      setCreateError('Please enter a valid email address.');
      return;
    }
    if (!createForm.password || createForm.password.length < 8) {
      setCreateError('Password must be at least 8 characters long.');
      return;
    }
    if (createForm.password !== createForm.confirmPassword) {
      setCreateError('Passwords do not match.');
      return;
    }

    setCreating(true);
    try {
      await adminApi.createUser({
        fullName: createForm.fullName.trim(),
        username: createForm.username.trim(),
        email: createForm.email.trim().toLowerCase(),
        password: createForm.password,
        confirmPassword: createForm.confirmPassword,
        role: createForm.role,
        enabled: createForm.enabled
      });

      setSuccess(`Account created successfully for ${createForm.fullName.trim()} (@${createForm.username.trim()}) with role ${createForm.role}.`);
      setShowCreateModal(false);
      setCreateForm({
        fullName: '',
        username: '',
        email: '',
        password: '',
        confirmPassword: '',
        role: 'ANALYST',
        enabled: true
      });
      fetchUsers();
      fetchStats();
    } catch (err) {
      setCreateError(err.response?.data?.message || err.message || 'Failed to create user account.');
    } finally {
      setCreating(false);
    }
  };

  // Open Edit Modal
  const openEditModal = (u) => {
    setEditUser(u);
    setEditForm({
      fullName: u.fullName || '',
      email: u.email || '',
      role: u.role || 'VIEWER',
      enabled: u.enabled,
      password: ''
    });
    setEditError('');
  };

  // Handle Edit Submit
  const handleEditSubmit = async (e) => {
    e.preventDefault();
    if (!editUser) return;
    setEditError('');

    if (!editForm.fullName.trim()) {
      setEditError('Full name cannot be blank.');
      return;
    }
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!editForm.email.trim() || !emailRegex.test(editForm.email.trim())) {
      setEditError('Valid email address is required.');
      return;
    }
    if (editForm.password && editForm.password.length < 8) {
      setEditError('If changing password, it must be at least 8 characters long.');
      return;
    }

    setEditing(true);
    try {
      await adminApi.updateUser(editUser.id, {
        fullName: editForm.fullName.trim(),
        email: editForm.email.trim().toLowerCase(),
        role: editForm.role,
        enabled: editForm.enabled,
        password: editForm.password.trim() || undefined
      });
      setSuccess(`Account updated successfully for ${editUser.username}.`);
      setEditUser(null);
      fetchUsers();
      fetchStats();
    } catch (err) {
      setEditError(err.response?.data?.message || err.message || 'Failed to update user account.');
    } finally {
      setEditing(false);
    }
  };

  // Handle Quick Role Change
  const handleRoleChangeConfirm = async () => {
    if (!roleModalUser || !newRoleSelection) return;
    setRoleUpdating(true);
    setError('');
    setSuccess('');
    try {
      await adminApi.updateRole(roleModalUser.id, newRoleSelection);
      setSuccess(`Successfully updated role for ${roleModalUser.username} to ${newRoleSelection}.`);
      setRoleModalUser(null);
      fetchUsers();
      fetchStats();
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to update user role.');
    } finally {
      setRoleUpdating(false);
    }
  };

  // Handle Quick Status Toggle
  const handleStatusToggleConfirm = async () => {
    if (!statusModalUser) return;
    setStatusUpdating(true);
    setError('');
    setSuccess('');
    try {
      const newStatus = !statusModalUser.enabled;
      await adminApi.updateStatus(statusModalUser.id, newStatus);
      setSuccess(`Successfully ${newStatus ? 'enabled' : 'disabled'} account for ${statusModalUser.username}.`);
      setStatusModalUser(null);
      fetchUsers();
      fetchStats();
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to update account status.');
    } finally {
      setStatusUpdating(false);
    }
  };

  // Open Audit Modal
  const handleViewAudit = async (u) => {
    setAuditUser(u);
    setAuditLoading(true);
    try {
      const res = await adminApi.getAuditLogs(u.id);
      setAuditLogs(res.data || []);
    } catch {
      setAuditLogs([]);
    } finally {
      setAuditLoading(false);
    }
  };

  const getRoleBadgeStyle = (r) => {
    const roleUpper = (r || 'VIEWER').toUpperCase();
    if (roleUpper === 'ADMIN') {
      return { background: 'rgba(16, 185, 129, 0.15)', color: '#10b981', border: '1px solid rgba(16, 185, 129, 0.3)' };
    }
    if (roleUpper === 'ANALYST') {
      return { background: 'rgba(99, 102, 241, 0.15)', color: '#818cf8', border: '1px solid rgba(99, 102, 241, 0.3)' };
    }
    return { background: 'rgba(56, 189, 248, 0.15)', color: '#38bdf8', border: '1px solid rgba(56, 189, 248, 0.3)' };
  };

  return (
    <div className="page-container" style={{ padding: '0 0 40px 0' }}>
      {/* Page Title & Create User Button */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px', marginBottom: '20px' }}>
        <PageHeading
          title="Admin User Management"
          description="Enterprise directory administration, role assignments (ADMIN, ANALYST, VIEWER), account lifecycle governance, and security audit logs."
        />
        <button
          onClick={() => {
            setCreateError('');
            setShowCreateModal(true);
          }}
          className="button button-primary"
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '8px',
            padding: '10px 18px',
            fontSize: '0.88rem',
            fontWeight: 600,
            borderRadius: '8px',
            background: 'linear-gradient(135deg, #4f46e5 0%, #4338ca 100%)',
            color: '#fff',
            boxShadow: '0 4px 12px rgba(79, 70, 229, 0.35)',
            border: 'none',
            cursor: 'pointer'
          }}
        >
          <UserPlus size={16} />
          <span>Create New User</span>
        </button>
      </div>

      {/* Metric Cards Grid */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
        gap: '14px',
        marginBottom: '24px'
      }}>
        {/* Total Users */}
        <div className="card" style={{ padding: '16px 18px', background: 'var(--panel)', display: 'flex', alignItems: 'center', gap: '14px' }}>
          <div style={{
            width: '42px',
            height: '42px',
            borderRadius: '10px',
            background: 'rgba(99, 102, 241, 0.15)',
            color: '#818cf8',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <Users size={20} />
          </div>
          <div>
            <div style={{ fontSize: '0.74rem', color: 'var(--muted)', fontWeight: 600, textTransform: 'uppercase' }}>Total Users</div>
            <div style={{ fontSize: '1.45rem', fontWeight: 800, color: 'var(--text)' }}>{stats.totalUsers}</div>
          </div>
        </div>

        {/* Active Users */}
        <div className="card" style={{ padding: '16px 18px', background: 'var(--panel)', display: 'flex', alignItems: 'center', gap: '14px' }}>
          <div style={{
            width: '42px',
            height: '42px',
            borderRadius: '10px',
            background: 'rgba(16, 185, 129, 0.15)',
            color: '#10b981',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <CheckCircle2 size={20} />
          </div>
          <div>
            <div style={{ fontSize: '0.74rem', color: 'var(--muted)', fontWeight: 600, textTransform: 'uppercase' }}>Active Accounts</div>
            <div style={{ fontSize: '1.45rem', fontWeight: 800, color: '#10b981' }}>{stats.activeUsers}</div>
          </div>
        </div>

        {/* Disabled Users */}
        <div className="card" style={{ padding: '16px 18px', background: 'var(--panel)', display: 'flex', alignItems: 'center', gap: '14px' }}>
          <div style={{
            width: '42px',
            height: '42px',
            borderRadius: '10px',
            background: 'rgba(239, 68, 68, 0.15)',
            color: '#ef4444',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <XCircle size={20} />
          </div>
          <div>
            <div style={{ fontSize: '0.74rem', color: 'var(--muted)', fontWeight: 600, textTransform: 'uppercase' }}>Disabled</div>
            <div style={{ fontSize: '1.45rem', fontWeight: 800, color: '#ef4444' }}>{stats.disabledUsers}</div>
          </div>
        </div>

        {/* Admin Count */}
        <div className="card" style={{ padding: '16px 18px', background: 'var(--panel)', display: 'flex', alignItems: 'center', gap: '14px' }}>
          <div style={{
            width: '42px',
            height: '42px',
            borderRadius: '10px',
            background: 'rgba(16, 185, 129, 0.15)',
            color: '#10b981',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <ShieldCheck size={20} />
          </div>
          <div>
            <div style={{ fontSize: '0.74rem', color: 'var(--muted)', fontWeight: 600, textTransform: 'uppercase' }}>Admins</div>
            <div style={{ fontSize: '1.45rem', fontWeight: 800, color: 'var(--text)' }}>{stats.adminCount}</div>
          </div>
        </div>

        {/* Analyst Count */}
        <div className="card" style={{ padding: '16px 18px', background: 'var(--panel)', display: 'flex', alignItems: 'center', gap: '14px' }}>
          <div style={{
            width: '42px',
            height: '42px',
            borderRadius: '10px',
            background: 'rgba(99, 102, 241, 0.15)',
            color: '#818cf8',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <Activity size={20} />
          </div>
          <div>
            <div style={{ fontSize: '0.74rem', color: 'var(--muted)', fontWeight: 600, textTransform: 'uppercase' }}>Analysts</div>
            <div style={{ fontSize: '1.45rem', fontWeight: 800, color: 'var(--text)' }}>{stats.analystCount}</div>
          </div>
        </div>

        {/* Viewer Count */}
        <div className="card" style={{ padding: '16px 18px', background: 'var(--panel)', display: 'flex', alignItems: 'center', gap: '14px' }}>
          <div style={{
            width: '42px',
            height: '42px',
            borderRadius: '10px',
            background: 'rgba(56, 189, 248, 0.15)',
            color: '#38bdf8',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <Eye size={20} />
          </div>
          <div>
            <div style={{ fontSize: '0.74rem', color: 'var(--muted)', fontWeight: 600, textTransform: 'uppercase' }}>Viewers</div>
            <div style={{ fontSize: '1.45rem', fontWeight: 800, color: 'var(--text)' }}>{stats.viewerCount}</div>
          </div>
        </div>

        {/* Pending Requests */}
        <div
          className="card"
          onClick={() => {
            setActiveTab('ANALYST_REQUESTS');
            fetchAnalystRequests();
          }}
          style={{
            padding: '16px 18px',
            background: 'var(--panel)',
            display: 'flex',
            alignItems: 'center',
            gap: '14px',
            cursor: 'pointer',
            border: activeTab === 'ANALYST_REQUESTS' ? '1px solid #f59e0b' : undefined
          }}
          title="Click to view and manage Analyst access requests"
        >
          <div style={{
            width: '42px',
            height: '42px',
            borderRadius: '10px',
            background: 'rgba(245, 158, 11, 0.15)',
            color: '#fbbf24',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <Clock size={20} />
          </div>
          <div>
            <div style={{ fontSize: '0.74rem', color: 'var(--muted)', fontWeight: 600, textTransform: 'uppercase' }}>Pending Requests</div>
            <div style={{ fontSize: '1.45rem', fontWeight: 800, color: (stats.pendingRequestsCount > 0 ? '#fbbf24' : 'var(--text)') }}>
              {stats.pendingRequestsCount || 0}
            </div>
          </div>
        </div>
      </div>

      {/* Notifications */}
      {success && (
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '10px',
          padding: '12px 16px',
          borderRadius: '8px',
          background: 'rgba(16, 185, 129, 0.12)',
          border: '1px solid rgba(16, 185, 129, 0.3)',
          color: '#34d399',
          fontSize: '0.85rem',
          marginBottom: '16px'
        }}>
          <Check size={18} />
          <span>{success}</span>
          <button
            onClick={() => setSuccess('')}
            style={{ marginLeft: 'auto', background: 'none', border: 'none', color: '#34d399', cursor: 'pointer' }}
          >
            <X size={16} />
          </button>
        </div>
      )}

      {error && (
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '10px',
          padding: '12px 16px',
          borderRadius: '8px',
          background: 'rgba(239, 68, 68, 0.12)',
          border: '1px solid rgba(239, 68, 68, 0.3)',
          color: '#fca5a5',
          fontSize: '0.85rem',
          marginBottom: '16px'
        }}>
          <AlertTriangle size={18} />
          <span>{error}</span>
          <button
            onClick={() => setError('')}
            style={{ marginLeft: 'auto', background: 'none', border: 'none', color: '#fca5a5', cursor: 'pointer' }}
          >
            <X size={16} />
          </button>
        </div>
      )}

      {/* Navigation Tabs */}
      <div style={{
        display: 'flex',
        gap: '10px',
        marginBottom: '20px',
        borderBottom: '1px solid var(--border)',
        paddingBottom: '12px'
      }}>
        <button
          type="button"
          onClick={() => setActiveTab('DIRECTORY')}
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '8px',
            padding: '9px 18px',
            borderRadius: '8px',
            border: 'none',
            background: activeTab === 'DIRECTORY' ? 'var(--primary, #4f46e5)' : 'rgba(255, 255, 255, 0.05)',
            color: activeTab === 'DIRECTORY' ? '#ffffff' : 'var(--text)',
            fontWeight: 600,
            fontSize: '0.88rem',
            cursor: 'pointer',
            transition: 'all 0.2s'
          }}
        >
          <Users size={16} />
          <span>User Directory</span>
          <span style={{
            background: activeTab === 'DIRECTORY' ? 'rgba(255,255,255,0.2)' : 'rgba(0,0,0,0.2)',
            padding: '2px 8px',
            borderRadius: '10px',
            fontSize: '0.74rem'
          }}>
            {stats.totalUsers || 0}
          </span>
        </button>

        <button
          type="button"
          onClick={() => {
            setActiveTab('ANALYST_REQUESTS');
            fetchAnalystRequests();
          }}
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '8px',
            padding: '9px 18px',
            borderRadius: '8px',
            border: 'none',
            background: activeTab === 'ANALYST_REQUESTS' ? '#d97706' : 'rgba(255, 255, 255, 0.05)',
            color: activeTab === 'ANALYST_REQUESTS' ? '#ffffff' : 'var(--text)',
            fontWeight: 600,
            fontSize: '0.88rem',
            cursor: 'pointer',
            transition: 'all 0.2s'
          }}
        >
          <Clock size={16} />
          <span>Pending Analyst Access Requests</span>
          <span style={{
            background: (stats.pendingRequestsCount > 0 ? '#ef4444' : (activeTab === 'ANALYST_REQUESTS' ? 'rgba(255,255,255,0.2)' : 'rgba(0,0,0,0.2)')),
            color: '#ffffff',
            padding: '2px 8px',
            borderRadius: '10px',
            fontSize: '0.74rem',
            fontWeight: 700
          }}>
            Pending: {stats.pendingRequestsCount || 0}
          </span>
        </button>
      </div>

      {activeTab === 'DIRECTORY' && (
        <>
      {/* Control Header: Search and Filters */}
      <div className="card" style={{ padding: '16px 20px', marginBottom: '20px', background: 'var(--panel)' }}>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '12px', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '10px', flex: '1 1 450px' }}>
            {/* Search Input */}
            <div style={{ position: 'relative', flex: '1 1 220px' }}>
              <Search size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--muted)' }} />
              <input
                type="text"
                placeholder="Search by name, username, or email..."
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setPage(0);
                }}
                style={{
                  width: '100%',
                  padding: '9px 12px 9px 36px',
                  borderRadius: '8px',
                  border: '1px solid var(--border)',
                  background: 'var(--bg)',
                  color: 'var(--text)',
                  fontSize: '0.85rem',
                  outline: 'none',
                  boxSizing: 'border-box'
                }}
              />
            </div>

            {/* Role Filter */}
            <div style={{ position: 'relative', minWidth: '140px' }}>
              <Filter size={15} style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: 'var(--muted)' }} />
              <select
                value={roleFilter}
                onChange={(e) => {
                  setRoleFilter(e.target.value);
                  setPage(0);
                }}
                style={{
                  width: '100%',
                  padding: '9px 12px 9px 32px',
                  borderRadius: '8px',
                  border: '1px solid var(--border)',
                  background: 'var(--bg)',
                  color: 'var(--text)',
                  fontSize: '0.85rem',
                  outline: 'none',
                  cursor: 'pointer',
                  boxSizing: 'border-box'
                }}
              >
                <option value="ALL">All Roles</option>
                <option value="ADMIN">ADMIN</option>
                <option value="ANALYST">ANALYST</option>
                <option value="VIEWER">VIEWER</option>
              </select>
            </div>

            {/* Status Filter */}
            <div style={{ position: 'relative', minWidth: '140px' }}>
              <select
                value={statusFilter}
                onChange={(e) => {
                  setStatusFilter(e.target.value);
                  setPage(0);
                }}
                style={{
                  width: '100%',
                  padding: '9px 12px',
                  borderRadius: '8px',
                  border: '1px solid var(--border)',
                  background: 'var(--bg)',
                  color: 'var(--text)',
                  fontSize: '0.85rem',
                  outline: 'none',
                  cursor: 'pointer',
                  boxSizing: 'border-box'
                }}
              >
                <option value="ALL">All Statuses</option>
                <option value="ACTIVE">Active Only</option>
                <option value="DISABLED">Disabled Only</option>
              </select>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <span style={{ fontSize: '0.82rem', color: 'var(--muted)' }}>
              Matching: <b>{totalElements}</b>
            </span>
            <button
              onClick={() => {
                fetchUsers();
                fetchStats();
              }}
              className="button button-secondary"
              style={{ display: 'flex', alignItems: 'center', gap: '6px', padding: '8px 12px', fontSize: '0.82rem' }}
              title="Refresh users"
            >
              <RefreshCw size={14} className={loading ? 'spin' : ''} />
              <span>Refresh</span>
            </button>
          </div>
        </div>
      </div>

      {/* Users Table */}
      <div className="card" style={{ padding: 0, overflow: 'hidden', background: 'var(--panel)' }}>
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.85rem' }}>
            <thead>
              <tr style={{ background: 'var(--subtle-bg, rgba(0,0,0,0.03))', borderBottom: '1px solid var(--border)' }}>
                <th style={{ padding: '12px 18px', fontWeight: 600, color: 'var(--muted)' }}>User</th>
                <th style={{ padding: '12px 18px', fontWeight: 600, color: 'var(--muted)' }}>Email</th>
                <th style={{ padding: '12px 18px', fontWeight: 600, color: 'var(--muted)' }}>Role</th>
                <th style={{ padding: '12px 18px', fontWeight: 600, color: 'var(--muted)' }}>Status</th>
                <th style={{ padding: '12px 18px', fontWeight: 600, color: 'var(--muted)' }}>Created</th>
                <th style={{ padding: '12px 18px', fontWeight: 600, color: 'var(--muted)' }}>Last Login</th>
                <th style={{ padding: '12px 18px', fontWeight: 600, color: 'var(--muted)', textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={7} style={{ padding: '40px', textAlign: 'center', color: 'var(--muted)' }}>
                    Loading user directory...
                  </td>
                </tr>
              ) : users.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ padding: '40px', textAlign: 'center', color: 'var(--muted)' }}>
                    No users found matching current filters.
                  </td>
                </tr>
              ) : (
                users.map((u) => {
                  const isCurrent = currentUser?.username === u.username;
                  const roleStyle = getRoleBadgeStyle(u.role);
                  return (
                    <tr
                      key={u.id}
                      style={{
                        borderBottom: '1px solid var(--border)',
                        background: isCurrent ? 'rgba(79, 70, 229, 0.04)' : 'transparent'
                      }}
                    >
                      <td style={{ padding: '12px 18px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                          <div style={{
                            width: '32px',
                            height: '32px',
                            borderRadius: '50%',
                            background: u.role === 'ADMIN' ? '#10b981' : u.role === 'ANALYST' ? '#6366f1' : '#0284c7',
                            color: '#ffffff',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            fontWeight: 700,
                            fontSize: '0.78rem'
                          }}>
                            {u.username.slice(0, 2).toUpperCase()}
                          </div>
                          <div>
                            <div style={{ fontWeight: 600, color: 'var(--text)' }}>
                              {u.fullName || u.username} {isCurrent && <span style={{ fontSize: '0.72rem', color: '#4f46e5', fontWeight: 700 }}>(You)</span>}
                            </div>
                            <div style={{ fontSize: '0.76rem', color: 'var(--muted)' }}>@{u.username}</div>
                          </div>
                        </div>
                      </td>

                      <td style={{ padding: '12px 18px', color: 'var(--text)' }}>
                        {u.email}
                      </td>

                      <td style={{ padding: '12px 18px' }}>
                        <span style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '5px',
                          padding: '3px 8px',
                          borderRadius: '12px',
                          fontSize: '0.74rem',
                          fontWeight: 700,
                          ...roleStyle
                        }}>
                          <Shield size={11} />
                          {u.role}
                        </span>
                      </td>

                      <td style={{ padding: '12px 18px' }}>
                        <span style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '5px',
                          padding: '3px 8px',
                          borderRadius: '12px',
                          fontSize: '0.74rem',
                          fontWeight: 600,
                          background: u.enabled ? 'rgba(16, 185, 129, 0.12)' : 'rgba(239, 68, 68, 0.12)',
                          color: u.enabled ? '#10b981' : '#ef4444'
                        }}>
                          {u.enabled ? 'Active' : 'Disabled'}
                        </span>
                      </td>

                      <td style={{ padding: '12px 18px', color: 'var(--muted)', fontSize: '0.78rem' }}>
                        {u.createdAt ? new Date(u.createdAt).toLocaleDateString() : '—'}
                      </td>

                      <td style={{ padding: '12px 18px', color: 'var(--muted)', fontSize: '0.78rem' }}>
                        {u.lastLoginAt ? new Date(u.lastLoginAt).toLocaleString() : 'Never'}
                      </td>

                      <td style={{ padding: '12px 18px', textAlign: 'right' }}>
                        <div style={{ display: 'inline-flex', gap: '5px', alignItems: 'center' }}>
                          {/* View Details */}
                          <button
                            onClick={() => setDetailsUser(u)}
                            className="button button-secondary"
                            style={{ padding: '5px 8px', fontSize: '0.75rem', borderRadius: '6px' }}
                            title="Inspect user details"
                          >
                            <Eye size={13} />
                          </button>

                          {/* Edit User */}
                          <button
                            onClick={() => openEditModal(u)}
                            className="button button-secondary"
                            style={{ padding: '5px 8px', fontSize: '0.75rem', borderRadius: '6px' }}
                            title="Edit user profile & permissions"
                          >
                            <Edit2 size={13} />
                          </button>

                          {/* Quick Role Change */}
                          <button
                            onClick={() => {
                              setRoleModalUser(u);
                              setNewRoleSelection(u.role);
                            }}
                            className="button button-secondary"
                            style={{ padding: '5px 9px', fontSize: '0.75rem', borderRadius: '6px' }}
                            title="Modify user role"
                          >
                            Role
                          </button>

                          {/* Quick Status Toggle */}
                          <button
                            onClick={() => setStatusModalUser(u)}
                            style={{
                              padding: '5px 9px',
                              fontSize: '0.75rem',
                              borderRadius: '6px',
                              border: '1px solid var(--border)',
                              background: 'transparent',
                              cursor: 'pointer',
                              color: u.enabled ? '#ef4444' : '#10b981'
                            }}
                            title={u.enabled ? 'Disable user' : 'Enable user'}
                          >
                            {u.enabled ? 'Disable' : 'Enable'}
                          </button>

                          {/* Audit Logs */}
                          <button
                            onClick={() => handleViewAudit(u)}
                            style={{
                              padding: '5px 8px',
                              fontSize: '0.75rem',
                              borderRadius: '6px',
                              border: '1px solid var(--border)',
                              background: 'transparent',
                              cursor: 'pointer',
                              color: 'var(--muted)'
                            }}
                            title="View audit history"
                          >
                            <History size={13} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Bar */}
        {totalPages > 1 && (
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '12px 18px',
            borderTop: '1px solid var(--border)',
            fontSize: '0.82rem',
            color: 'var(--muted)'
          }}>
            <span>
              Page {page + 1} of {totalPages} ({totalElements} users)
            </span>
            <div style={{ display: 'flex', gap: '8px' }}>
              <button
                disabled={page === 0}
                onClick={() => setPage((p) => p - 1)}
                className="button button-secondary"
                style={{ padding: '5px 10px', fontSize: '0.78rem' }}
              >
                Previous
              </button>
              <button
                disabled={page + 1 >= totalPages}
                onClick={() => setPage((p) => p + 1)}
                className="button button-secondary"
                style={{ padding: '5px 10px', fontSize: '0.78rem' }}
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>
      </>
      )}

      {activeTab === 'ANALYST_REQUESTS' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', marginBottom: '24px' }}>
          {/* Analyst Requests Control Bar */}
          <div className="card" style={{ padding: '16px 20px', background: 'var(--panel)' }}>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '12px', alignItems: 'center', justifyContent: 'space-between' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <span style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text)' }}>
                  Filter Status:
                </span>
                <select
                  value={requestStatusFilter}
                  onChange={(e) => setRequestStatusFilter(e.target.value)}
                  style={{
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: '1px solid var(--border)',
                    background: 'var(--bg)',
                    color: 'var(--text)',
                    fontSize: '0.84rem',
                    outline: 'none'
                  }}
                >
                  <option value="PENDING_APPROVAL">Pending Approval Only</option>
                  <option value="ALL">All Requests</option>
                  <option value="ACTIVE">Approved / Active Only</option>
                  <option value="REJECTED">Rejected Only</option>
                </select>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <button
                  onClick={() => {
                    fetchAnalystRequests();
                    fetchStats();
                  }}
                  className="button button-secondary"
                  disabled={requestsLoading}
                  style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: '0.82rem', padding: '8px 14px' }}
                >
                  <RefreshCw size={14} className={requestsLoading ? 'spin' : ''} />
                  <span>Refresh Requests</span>
                </button>
              </div>
            </div>
          </div>

          {/* Analyst Requests Table */}
          <div className="card" style={{ padding: 0, overflow: 'hidden', background: 'var(--panel)' }}>
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.86rem' }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid var(--border)', background: 'rgba(255, 255, 255, 0.02)' }}>
                    <th style={{ padding: '12px 16px', color: 'var(--muted)', fontWeight: 600 }}>Applicant</th>
                    <th style={{ padding: '12px 16px', color: 'var(--muted)', fontWeight: 600 }}>Contact Email</th>
                    <th style={{ padding: '12px 16px', color: 'var(--muted)', fontWeight: 600 }}>Organization / Dept</th>
                    <th style={{ padding: '12px 16px', color: 'var(--muted)', fontWeight: 600 }}>Submitted Date</th>
                    <th style={{ padding: '12px 16px', color: 'var(--muted)', fontWeight: 600 }}>Status</th>
                    <th style={{ padding: '12px 16px', color: 'var(--muted)', fontWeight: 600, textAlign: 'right' }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {requestsLoading ? (
                    <tr>
                      <td colSpan="6" style={{ padding: '40px', textAlign: 'center', color: 'var(--muted)' }}>
                        <RefreshCw size={24} className="spin" style={{ marginBottom: '8px' }} />
                        <div>Loading Analyst requests...</div>
                      </td>
                    </tr>
                  ) : analystRequests.length === 0 ? (
                    <tr>
                      <td colSpan="6" style={{ padding: '40px', textAlign: 'center', color: 'var(--muted)' }}>
                        <Clock size={32} style={{ marginBottom: '8px', opacity: 0.5 }} />
                        <div style={{ fontWeight: 600, fontSize: '0.95rem', color: 'var(--text)' }}>
                          No Analyst access requests found
                        </div>
                        <div style={{ fontSize: '0.8rem', marginTop: '4px' }}>
                          {requestStatusFilter === 'PENDING_APPROVAL'
                            ? 'There are currently no pending access requests awaiting administrator review.'
                            : 'No requests match the selected status filter.'}
                        </div>
                      </td>
                    </tr>
                  ) : (
                    analystRequests.map((req) => {
                      const isPending = req.accountStatus === 'PENDING_APPROVAL';
                      const isApproved = req.accountStatus === 'ACTIVE';
                      const isRejected = req.accountStatus === 'REJECTED';

                      return (
                        <tr key={req.id} style={{ borderBottom: '1px solid var(--border)' }}>
                          <td style={{ padding: '14px 16px' }}>
                            <div style={{ fontWeight: 600, color: 'var(--text)' }}>
                              {req.fullName || req.username}
                            </div>
                            <div style={{ fontSize: '0.76rem', color: 'var(--muted)' }}>
                              @{req.username}
                            </div>
                          </td>
                          <td style={{ padding: '14px 16px', color: 'var(--text)' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                              <Mail size={14} color="var(--muted)" />
                              <span>{req.email}</span>
                            </div>
                          </td>
                          <td style={{ padding: '14px 16px' }}>
                            <div style={{ fontSize: '0.84rem', color: 'var(--text)', fontWeight: 500 }}>
                              {req.organization || '—'}
                            </div>
                            <div style={{ fontSize: '0.76rem', color: 'var(--muted)' }}>
                              {[req.department, req.jobTitle].filter(Boolean).join(' • ') || 'No title provided'}
                            </div>
                          </td>
                          <td style={{ padding: '14px 16px', fontSize: '0.8rem', color: 'var(--muted)' }}>
                            {req.createdAt ? new Date(req.createdAt).toLocaleString() : '—'}
                          </td>
                          <td style={{ padding: '14px 16px' }}>
                            {isPending && (
                              <span style={{
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: '5px',
                                padding: '3px 8px',
                                borderRadius: '12px',
                                fontSize: '0.74rem',
                                fontWeight: 600,
                                background: 'rgba(245, 158, 11, 0.15)',
                                color: '#fbbf24',
                                border: '1px solid rgba(245, 158, 11, 0.3)'
                              }}>
                                <Clock size={12} />
                                <span>Pending Approval</span>
                              </span>
                            )}
                            {isApproved && (
                              <span style={{
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: '5px',
                                padding: '3px 8px',
                                borderRadius: '12px',
                                fontSize: '0.74rem',
                                fontWeight: 600,
                                background: 'rgba(16, 185, 129, 0.15)',
                                color: '#10b981',
                                border: '1px solid rgba(16, 185, 129, 0.3)'
                              }}>
                                <CheckCircle2 size={12} />
                                <span>Approved & Active</span>
                              </span>
                            )}
                            {isRejected && (
                              <span style={{
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: '5px',
                                padding: '3px 8px',
                                borderRadius: '12px',
                                fontSize: '0.74rem',
                                fontWeight: 600,
                                background: 'rgba(239, 68, 68, 0.15)',
                                color: '#f87171',
                                border: '1px solid rgba(239, 68, 68, 0.3)'
                              }}>
                                <XCircle size={12} />
                                <span>Rejected</span>
                              </span>
                            )}
                          </td>
                          <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                            <div style={{ display: 'inline-flex', gap: '8px', alignItems: 'center' }}>
                              {/* View Details */}
                              <button
                                onClick={() => setSelectedRequestDetails(req)}
                                style={{
                                  padding: '5px 9px',
                                  fontSize: '0.76rem',
                                  borderRadius: '6px',
                                  border: '1px solid var(--border)',
                                  background: 'transparent',
                                  cursor: 'pointer',
                                  color: 'var(--text)',
                                  display: 'inline-flex',
                                  alignItems: 'center',
                                  gap: '4px'
                                }}
                                title="View full applicant details"
                              >
                                <Eye size={13} />
                                <span>Details</span>
                              </button>

                              {isPending && (
                                <>
                                  {/* Approve Button */}
                                  <button
                                    onClick={() => handleApproveRequest(req.id, req.fullName || req.username)}
                                    disabled={actionLoading}
                                    style={{
                                      padding: '5px 12px',
                                      fontSize: '0.76rem',
                                      fontWeight: 600,
                                      borderRadius: '6px',
                                      border: 'none',
                                      background: '#10b981',
                                      color: '#ffffff',
                                      cursor: actionLoading ? 'not-allowed' : 'pointer',
                                      display: 'inline-flex',
                                      alignItems: 'center',
                                      gap: '4px'
                                    }}
                                    title="Approve Analyst access"
                                  >
                                    <ThumbsUp size={13} />
                                    <span>Approve</span>
                                  </button>

                                  {/* Reject Button */}
                                  <button
                                    onClick={() => {
                                      setRejectingRequest(req);
                                      setRejectionReason('');
                                    }}
                                    disabled={actionLoading}
                                    style={{
                                      padding: '5px 12px',
                                      fontSize: '0.76rem',
                                      fontWeight: 600,
                                      borderRadius: '6px',
                                      background: 'rgba(239, 68, 68, 0.2)',
                                      color: '#ef4444',
                                      border: '1px solid rgba(239, 68, 68, 0.4)',
                                      cursor: actionLoading ? 'not-allowed' : 'pointer',
                                      display: 'inline-flex',
                                      alignItems: 'center',
                                      gap: '4px'
                                    }}
                                    title="Reject Analyst access"
                                  >
                                    <ThumbsDown size={13} />
                                    <span>Reject</span>
                                  </button>
                                </>
                              )}
                            </div>
                          </td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* CREATE USER MODAL */}
      {showCreateModal && (
        <div style={{
          position: 'fixed',
          inset: 0,
          background: 'rgba(0,0,0,0.7)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '16px',
          zIndex: 1000,
          backdropFilter: 'blur(3px)'
        }}>
          <div style={{
            width: '100%',
            maxWidth: '520px',
            background: 'var(--panel)',
            border: '1px solid var(--border)',
            borderRadius: '14px',
            padding: '26px',
            boxShadow: '0 20px 45px rgba(0,0,0,0.4)',
            maxHeight: '90vh',
            overflowY: 'auto'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <div style={{ width: '36px', height: '36px', borderRadius: '8px', background: 'rgba(79, 70, 229, 0.15)', color: '#818cf8', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <UserPlus size={18} />
                </div>
                <div>
                  <h3 style={{ margin: 0, fontSize: '1.15rem', fontWeight: 700, color: 'var(--text)' }}>
                    Create New Enterprise User
                  </h3>
                  <div style={{ fontSize: '0.78rem', color: 'var(--muted)' }}>
                    Provision an account directly into PostgreSQL
                  </div>
                </div>
              </div>
              <button
                onClick={() => setShowCreateModal(false)}
                style={{ background: 'none', border: 'none', color: 'var(--muted)', cursor: 'pointer' }}
              >
                <X size={18} />
              </button>
            </div>

            {createError && (
              <div style={{
                display: 'flex',
                alignItems: 'flex-start',
                gap: '10px',
                padding: '11px 14px',
                borderRadius: '8px',
                background: 'rgba(239, 68, 68, 0.12)',
                border: '1px solid rgba(239, 68, 68, 0.3)',
                color: '#fca5a5',
                fontSize: '0.84rem',
                lineHeight: 1.45,
                marginBottom: '16px'
              }}>
                <AlertTriangle size={16} style={{ flexShrink: 0, marginTop: '2px', color: '#ef4444' }} />
                <span>{createError}</span>
              </div>
            )}

            <form onSubmit={handleCreateSubmit} noValidate>
              {/* Full Name */}
              <div style={{ marginBottom: '13px' }}>
                <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 600, color: 'var(--muted)', marginBottom: '5px' }}>
                  Full Name *
                </label>
                <input
                  type="text"
                  placeholder="e.g. Sarah Jenkins"
                  value={createForm.fullName}
                  onChange={(e) => setCreateForm({ ...createForm, fullName: e.target.value })}
                  style={{
                    width: '100%',
                    padding: '9px 12px',
                    borderRadius: '8px',
                    border: '1px solid var(--border)',
                    background: 'var(--bg)',
                    color: 'var(--text)',
                    fontSize: '0.86rem',
                    outline: 'none',
                    boxSizing: 'border-box'
                  }}
                  required
                />
              </div>

              {/* Username */}
              <div style={{ marginBottom: '13px' }}>
                <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 600, color: 'var(--muted)', marginBottom: '5px' }}>
                  Username *
                </label>
                <input
                  type="text"
                  placeholder="e.g. sjenkins"
                  value={createForm.username}
                  onChange={(e) => setCreateForm({ ...createForm, username: e.target.value })}
                  style={{
                    width: '100%',
                    padding: '9px 12px',
                    borderRadius: '8px',
                    border: '1px solid var(--border)',
                    background: 'var(--bg)',
                    color: 'var(--text)',
                    fontSize: '0.86rem',
                    outline: 'none',
                    boxSizing: 'border-box'
                  }}
                  required
                />
              </div>

              {/* Corporate Email */}
              <div style={{ marginBottom: '13px' }}>
                <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 600, color: 'var(--muted)', marginBottom: '5px' }}>
                  Corporate Email Address *
                </label>
                <input
                  type="email"
                  placeholder="e.g. sjenkins@recallradar.com"
                  value={createForm.email}
                  onChange={(e) => setCreateForm({ ...createForm, email: e.target.value })}
                  style={{
                    width: '100%',
                    padding: '9px 12px',
                    borderRadius: '8px',
                    border: '1px solid var(--border)',
                    background: 'var(--bg)',
                    color: 'var(--text)',
                    fontSize: '0.86rem',
                    outline: 'none',
                    boxSizing: 'border-box'
                  }}
                  required
                />
              </div>

              {/* Password & Confirm Password */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '14px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 600, color: 'var(--muted)', marginBottom: '5px' }}>
                    Password (min 8 chars) *
                  </label>
                  <input
                    type="password"
                    placeholder="Enter password"
                    value={createForm.password}
                    onChange={(e) => setCreateForm({ ...createForm, password: e.target.value })}
                    style={{
                      width: '100%',
                      padding: '9px 12px',
                      borderRadius: '8px',
                      border: '1px solid var(--border)',
                      background: 'var(--bg)',
                      color: 'var(--text)',
                      fontSize: '0.86rem',
                      outline: 'none',
                      boxSizing: 'border-box'
                    }}
                    required
                  />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 600, color: 'var(--muted)', marginBottom: '5px' }}>
                    Confirm Password *
                  </label>
                  <input
                    type="password"
                    placeholder="Confirm password"
                    value={createForm.confirmPassword}
                    onChange={(e) => setCreateForm({ ...createForm, confirmPassword: e.target.value })}
                    style={{
                      width: '100%',
                      padding: '9px 12px',
                      borderRadius: '8px',
                      border: '1px solid var(--border)',
                      background: 'var(--bg)',
                      color: 'var(--text)',
                      fontSize: '0.86rem',
                      outline: 'none',
                      boxSizing: 'border-box'
                    }}
                    required
                  />
                </div>
              </div>

              {/* Role Selection */}
              <div style={{ marginBottom: '14px' }}>
                <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 600, color: 'var(--muted)', marginBottom: '5px' }}>
                  Assigned Authorization Role *
                </label>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '8px' }}>
                  {['ADMIN', 'ANALYST', 'VIEWER'].map((r) => (
                    <button
                      type="button"
                      key={r}
                      onClick={() => setCreateForm({ ...createForm, role: r })}
                      style={{
                        padding: '9px 8px',
                        borderRadius: '8px',
                        border: `1px solid ${createForm.role === r ? '#4f46e5' : 'var(--border)'}`,
                        background: createForm.role === r ? 'rgba(79, 70, 229, 0.12)' : 'var(--bg)',
                        color: createForm.role === r ? '#818cf8' : 'var(--text)',
                        fontWeight: 600,
                        fontSize: '0.82rem',
                        cursor: 'pointer'
                      }}
                    >
                      {r}
                    </button>
                  ))}
                </div>
              </div>

              {/* Initial Status */}
              <div style={{ marginBottom: '22px' }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: '10px', cursor: 'pointer' }}>
                  <input
                    type="checkbox"
                    checked={createForm.enabled}
                    onChange={(e) => setCreateForm({ ...createForm, enabled: e.target.checked })}
                    style={{ accentColor: '#10b981', width: '16px', height: '16px' }}
                  />
                  <span style={{ fontSize: '0.84rem', color: 'var(--text)', fontWeight: 500 }}>
                    Enable account immediately (ACTIVE status)
                  </span>
                </label>
              </div>

              {/* Form Buttons */}
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="button button-secondary"
                  disabled={creating}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="button button-primary"
                  disabled={creating}
                  style={{
                    background: 'linear-gradient(135deg, #4f46e5 0%, #4338ca 100%)',
                    color: '#fff',
                    padding: '9px 18px',
                    fontWeight: 600,
                    borderRadius: '8px',
                    border: 'none',
                    cursor: creating ? 'not-allowed' : 'pointer'
                  }}
                >
                  {creating ? 'Creating User...' : 'Create Account'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* EDIT USER MODAL */}
      {editUser && (
        <div style={{
          position: 'fixed',
          inset: 0,
          background: 'rgba(0,0,0,0.7)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '16px',
          zIndex: 1000,
          backdropFilter: 'blur(3px)'
        }}>
          <div style={{
            width: '100%',
            maxWidth: '500px',
            background: 'var(--panel)',
            border: '1px solid var(--border)',
            borderRadius: '14px',
            padding: '26px',
            boxShadow: '0 20px 45px rgba(0,0,0,0.4)',
            maxHeight: '90vh',
            overflowY: 'auto'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <div>
                <h3 style={{ margin: 0, fontSize: '1.15rem', fontWeight: 700, color: 'var(--text)' }}>
                  Edit User: @{editUser.username}
                </h3>
                <div style={{ fontSize: '0.78rem', color: 'var(--muted)' }}>
                  Modify user details, role permissions, or reset password
                </div>
              </div>
              <button
                onClick={() => setEditUser(null)}
                style={{ background: 'none', border: 'none', color: 'var(--muted)', cursor: 'pointer' }}
              >
                <X size={18} />
              </button>
            </div>

            {editError && (
              <div style={{
                display: 'flex',
                alignItems: 'flex-start',
                gap: '10px',
                padding: '11px 14px',
                borderRadius: '8px',
                background: 'rgba(239, 68, 68, 0.12)',
                border: '1px solid rgba(239, 68, 68, 0.3)',
                color: '#fca5a5',
                fontSize: '0.84rem',
                lineHeight: 1.45,
                marginBottom: '16px'
              }}>
                <AlertTriangle size={16} style={{ flexShrink: 0, marginTop: '2px', color: '#ef4444' }} />
                <span>{editError}</span>
              </div>
            )}

            <form onSubmit={handleEditSubmit} noValidate>
              <div style={{ marginBottom: '13px' }}>
                <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 600, color: 'var(--muted)', marginBottom: '5px' }}>
                  Full Name *
                </label>
                <input
                  type="text"
                  value={editForm.fullName}
                  onChange={(e) => setEditForm({ ...editForm, fullName: e.target.value })}
                  style={{
                    width: '100%',
                    padding: '9px 12px',
                    borderRadius: '8px',
                    border: '1px solid var(--border)',
                    background: 'var(--bg)',
                    color: 'var(--text)',
                    fontSize: '0.86rem',
                    outline: 'none',
                    boxSizing: 'border-box'
                  }}
                  required
                />
              </div>

              <div style={{ marginBottom: '13px' }}>
                <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 600, color: 'var(--muted)', marginBottom: '5px' }}>
                  Corporate Email *
                </label>
                <input
                  type="email"
                  value={editForm.email}
                  onChange={(e) => setEditForm({ ...editForm, email: e.target.value })}
                  style={{
                    width: '100%',
                    padding: '9px 12px',
                    borderRadius: '8px',
                    border: '1px solid var(--border)',
                    background: 'var(--bg)',
                    color: 'var(--text)',
                    fontSize: '0.86rem',
                    outline: 'none',
                    boxSizing: 'border-box'
                  }}
                  required
                />
              </div>

              <div style={{ marginBottom: '13px' }}>
                <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 600, color: 'var(--muted)', marginBottom: '5px' }}>
                  Role Assignment *
                </label>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '8px' }}>
                  {['ADMIN', 'ANALYST', 'VIEWER'].map((r) => (
                    <button
                      type="button"
                      key={r}
                      onClick={() => setEditForm({ ...editForm, role: r })}
                      style={{
                        padding: '9px 8px',
                        borderRadius: '8px',
                        border: `1px solid ${editForm.role === r ? '#4f46e5' : 'var(--border)'}`,
                        background: editForm.role === r ? 'rgba(79, 70, 229, 0.12)' : 'var(--bg)',
                        color: editForm.role === r ? '#818cf8' : 'var(--text)',
                        fontWeight: 600,
                        fontSize: '0.82rem',
                        cursor: 'pointer'
                      }}
                    >
                      {r}
                    </button>
                  ))}
                </div>
              </div>

              <div style={{ marginBottom: '13px' }}>
                <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 600, color: 'var(--muted)', marginBottom: '5px' }}>
                  Reset Password (leave blank to keep unchanged)
                </label>
                <input
                  type="password"
                  placeholder="New password (at least 8 chars)"
                  value={editForm.password}
                  onChange={(e) => setEditForm({ ...editForm, password: e.target.value })}
                  style={{
                    width: '100%',
                    padding: '9px 12px',
                    borderRadius: '8px',
                    border: '1px solid var(--border)',
                    background: 'var(--bg)',
                    color: 'var(--text)',
                    fontSize: '0.86rem',
                    outline: 'none',
                    boxSizing: 'border-box'
                  }}
                />
              </div>

              <div style={{ marginBottom: '22px' }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: '10px', cursor: 'pointer' }}>
                  <input
                    type="checkbox"
                    checked={editForm.enabled}
                    onChange={(e) => setEditForm({ ...editForm, enabled: e.target.checked })}
                    style={{ accentColor: '#10b981', width: '16px', height: '16px' }}
                  />
                  <span style={{ fontSize: '0.84rem', color: 'var(--text)', fontWeight: 500 }}>
                    Account status enabled (ACTIVE)
                  </span>
                </label>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                <button
                  type="button"
                  onClick={() => setEditUser(null)}
                  className="button button-secondary"
                  disabled={editing}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="button button-primary"
                  disabled={editing}
                  style={{
                    background: 'linear-gradient(135deg, #4f46e5 0%, #4338ca 100%)',
                    color: '#fff',
                    padding: '9px 18px',
                    fontWeight: 600,
                    borderRadius: '8px',
                    border: 'none',
                    cursor: editing ? 'not-allowed' : 'pointer'
                  }}
                >
                  {editing ? 'Saving...' : 'Save Changes'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* USER DETAILS MODAL */}
      {detailsUser && (
        <div style={{
          position: 'fixed',
          inset: 0,
          background: 'rgba(0,0,0,0.7)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '16px',
          zIndex: 1000,
          backdropFilter: 'blur(3px)'
        }}>
          <div style={{
            width: '100%',
            maxWidth: '480px',
            background: 'var(--panel)',
            border: '1px solid var(--border)',
            borderRadius: '14px',
            padding: '24px',
            boxShadow: '0 20px 40px rgba(0,0,0,0.35)'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <div style={{ width: '38px', height: '38px', borderRadius: '50%', background: '#4f46e5', color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 700 }}>
                  {detailsUser.username.slice(0, 2).toUpperCase()}
                </div>
                <div>
                  <h3 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 700, color: 'var(--text)' }}>
                    {detailsUser.fullName || detailsUser.username}
                  </h3>
                  <div style={{ fontSize: '0.78rem', color: 'var(--muted)' }}>@{detailsUser.username}</div>
                </div>
              </div>
              <button
                onClick={() => setDetailsUser(null)}
                style={{ background: 'none', border: 'none', color: 'var(--muted)', cursor: 'pointer' }}
              >
                <X size={18} />
              </button>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', fontSize: '0.85rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 12px', background: 'var(--bg)', borderRadius: '8px' }}>
                <span style={{ color: 'var(--muted)' }}>User ID</span>
                <span style={{ fontWeight: 600, color: 'var(--text)' }}>#{detailsUser.id}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 12px', background: 'var(--bg)', borderRadius: '8px' }}>
                <span style={{ color: 'var(--muted)' }}>Email Address</span>
                <span style={{ fontWeight: 600, color: 'var(--text)' }}>{detailsUser.email}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 12px', background: 'var(--bg)', borderRadius: '8px' }}>
                <span style={{ color: 'var(--muted)' }}>Role</span>
                <span style={{ fontWeight: 700, ...getRoleBadgeStyle(detailsUser.role), padding: '2px 8px', borderRadius: '10px' }}>
                  {detailsUser.role}
                </span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 12px', background: 'var(--bg)', borderRadius: '8px' }}>
                <span style={{ color: 'var(--muted)' }}>Account Status</span>
                <span style={{ fontWeight: 600, color: detailsUser.enabled ? '#10b981' : '#ef4444' }}>
                  {detailsUser.enabled ? 'ACTIVE' : 'DISABLED'}
                </span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 12px', background: 'var(--bg)', borderRadius: '8px' }}>
                <span style={{ color: 'var(--muted)' }}>Registered</span>
                <span style={{ color: 'var(--text)' }}>
                  {detailsUser.createdAt ? new Date(detailsUser.createdAt).toLocaleString() : '—'}
                </span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 12px', background: 'var(--bg)', borderRadius: '8px' }}>
                <span style={{ color: 'var(--muted)' }}>Last Profile Update</span>
                <span style={{ color: 'var(--text)' }}>
                  {detailsUser.updatedAt ? new Date(detailsUser.updatedAt).toLocaleString() : '—'}
                </span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 12px', background: 'var(--bg)', borderRadius: '8px' }}>
                <span style={{ color: 'var(--muted)' }}>Last Login Activity</span>
                <span style={{ color: 'var(--text)' }}>
                  {detailsUser.lastLoginAt ? new Date(detailsUser.lastLoginAt).toLocaleString() : 'Never logged in'}
                </span>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px', marginTop: '20px' }}>
              <button
                onClick={() => {
                  const u = detailsUser;
                  setDetailsUser(null);
                  openEditModal(u);
                }}
                className="button button-primary"
                style={{ fontSize: '0.82rem', padding: '7px 14px' }}
              >
                Edit User
              </button>
              <button
                onClick={() => setDetailsUser(null)}
                className="button button-secondary"
                style={{ fontSize: '0.82rem', padding: '7px 14px' }}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* QUICK ROLE CHANGE MODAL */}
      {roleModalUser && (
        <div style={{
          position: 'fixed',
          inset: 0,
          background: 'rgba(0,0,0,0.65)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '16px',
          zIndex: 1000,
          backdropFilter: 'blur(3px)'
        }}>
          <div style={{
            width: '100%',
            maxWidth: '440px',
            background: 'var(--panel)',
            border: '1px solid var(--border)',
            borderRadius: '14px',
            padding: '24px',
            boxShadow: '0 20px 40px rgba(0,0,0,0.35)'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 700, color: 'var(--text)' }}>
                Update Authorization Role
              </h3>
              <button
                onClick={() => setRoleModalUser(null)}
                style={{ background: 'none', border: 'none', color: 'var(--muted)', cursor: 'pointer' }}
              >
                <X size={18} />
              </button>
            </div>

            <p style={{ fontSize: '0.84rem', color: 'var(--muted)', lineHeight: 1.5, marginBottom: '18px' }}>
              Select a new role for <b>{roleModalUser.fullName || roleModalUser.username}</b> (@{roleModalUser.username}).
            </p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginBottom: '20px' }}>
              {['ADMIN', 'ANALYST', 'VIEWER'].map((r) => (
                <label
                  key={r}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '12px',
                    padding: '12px 14px',
                    borderRadius: '8px',
                    border: `1px solid ${newRoleSelection === r ? '#4f46e5' : 'var(--border)'}`,
                    background: newRoleSelection === r ? 'rgba(79, 70, 229, 0.08)' : 'transparent',
                    cursor: 'pointer'
                  }}
                >
                  <input
                    type="radio"
                    name="role"
                    value={r}
                    checked={newRoleSelection === r}
                    onChange={(e) => setNewRoleSelection(e.target.value)}
                    style={{ accentColor: '#4f46e5' }}
                  />
                  <div>
                    <div style={{ fontWeight: 600, fontSize: '0.86rem', color: 'var(--text)' }}>{r}</div>
                    <div style={{ fontSize: '0.74rem', color: 'var(--muted)' }}>
                      {r === 'ADMIN' && 'Full administrative access: user management, catalog CRUD, severity review.'}
                      {r === 'ANALYST' && 'Operational access: recall notice creation, editing, and severity updates.'}
                      {r === 'VIEWER' && 'Read-only access: inspect catalogues, browse recalls, and use AI assistant.'}
                    </div>
                  </div>
                </label>
              ))}
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
              <button
                type="button"
                onClick={() => setRoleModalUser(null)}
                className="button button-secondary"
                disabled={roleUpdating}
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleRoleChangeConfirm}
                className="button button-primary"
                disabled={roleUpdating || newRoleSelection === roleModalUser.role}
              >
                {roleUpdating ? 'Updating Role...' : 'Confirm Role Change'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* QUICK STATUS TOGGLE MODAL */}
      {statusModalUser && (
        <div style={{
          position: 'fixed',
          inset: 0,
          background: 'rgba(0,0,0,0.65)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '16px',
          zIndex: 1000,
          backdropFilter: 'blur(3px)'
        }}>
          <div style={{
            width: '100%',
            maxWidth: '440px',
            background: 'var(--panel)',
            border: '1px solid var(--border)',
            borderRadius: '14px',
            padding: '24px',
            boxShadow: '0 20px 40px rgba(0,0,0,0.35)'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '14px', color: statusModalUser.enabled ? '#ef4444' : '#10b981' }}>
              <AlertTriangle size={22} />
              <h3 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 700, color: 'var(--text)' }}>
                {statusModalUser.enabled ? 'Disable Account' : 'Re-enable Account'}
              </h3>
            </div>

            <p style={{ fontSize: '0.85rem', color: 'var(--muted)', lineHeight: 1.5, marginBottom: '20px' }}>
              Are you sure you want to <b>{statusModalUser.enabled ? 'disable' : 'enable'}</b> the account for{' '}
              <b>{statusModalUser.username}</b>?{' '}
              {statusModalUser.enabled
                ? 'The user will immediately be blocked from logging in or using their authentication tokens.'
                : 'The user will be able to log in and use workspace features again.'}
            </p>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
              <button
                type="button"
                onClick={() => setStatusModalUser(null)}
                className="button button-secondary"
                disabled={statusUpdating}
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleStatusToggleConfirm}
                className="button"
                style={{
                  background: statusModalUser.enabled ? '#dc2626' : '#10b981',
                  color: '#fff',
                  border: 'none',
                  borderRadius: '8px',
                  padding: '8px 16px',
                  fontWeight: 600,
                  fontSize: '0.85rem',
                  cursor: statusUpdating ? 'not-allowed' : 'pointer'
                }}
                disabled={statusUpdating}
              >
                {statusUpdating ? 'Processing...' : statusModalUser.enabled ? 'Yes, Disable Account' : 'Yes, Enable Account'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* AUDIT HISTORY MODAL */}
      {auditUser && (
        <div style={{
          position: 'fixed',
          inset: 0,
          background: 'rgba(0,0,0,0.65)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '16px',
          zIndex: 1000,
          backdropFilter: 'blur(3px)'
        }}>
          <div style={{
            width: '100%',
            maxWidth: '580px',
            background: 'var(--panel)',
            border: '1px solid var(--border)',
            borderRadius: '14px',
            padding: '24px',
            boxShadow: '0 20px 40px rgba(0,0,0,0.35)',
            maxHeight: '80vh',
            display: 'flex',
            flexDirection: 'column'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
              <div>
                <h3 style={{ margin: '0 0 2px 0', fontSize: '1.1rem', fontWeight: 700, color: 'var(--text)' }}>
                  Audit History: @{auditUser.username}
                </h3>
                <span style={{ fontSize: '0.78rem', color: 'var(--muted)' }}>
                  Recorded privilege, lifecycle, and credential modifications
                </span>
              </div>
              <button
                onClick={() => setAuditUser(null)}
                style={{ background: 'none', border: 'none', color: 'var(--muted)', cursor: 'pointer' }}
              >
                <X size={18} />
              </button>
            </div>

            <div style={{ flex: 1, overflowY: 'auto', paddingRight: '4px' }}>
              {auditLoading ? (
                <div style={{ textAlign: 'center', padding: '30px', color: 'var(--muted)' }}>Loading audit records...</div>
              ) : auditLogs.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '30px', color: 'var(--muted)', fontSize: '0.85rem' }}>
                  No administrative actions have been recorded for this user yet.
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                  {auditLogs.map((log) => (
                    <div
                      key={log.id}
                      style={{
                        padding: '10px 14px',
                        borderRadius: '8px',
                        border: '1px solid var(--border)',
                        background: 'var(--bg)',
                        fontSize: '0.8rem'
                      }}
                    >
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                        <span style={{ fontWeight: 700, color: '#4f46e5' }}>{log.action}</span>
                        <span style={{ color: 'var(--muted)', fontSize: '0.74rem' }}>
                          {log.createdAt ? new Date(log.createdAt).toLocaleString() : ''}
                        </span>
                      </div>
                      <div style={{ color: 'var(--text)', marginBottom: '4px' }}>{log.details}</div>
                      <div style={{ fontSize: '0.72rem', color: 'var(--muted)' }}>
                        Performed by: <b>{log.performedBy}</b> {log.previousValue && `| ${log.previousValue} → ${log.newValue}`}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div style={{ marginTop: '16px', textAlign: 'right' }}>
              <button
                onClick={() => setAuditUser(null)}
                className="button button-secondary"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ANALYST REQUEST DETAILS MODAL */}
      {selectedRequestDetails && (
        <div style={{
          position: 'fixed',
          inset: 0,
          background: 'rgba(0,0,0,0.75)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '16px',
          zIndex: 1000,
          backdropFilter: 'blur(3px)'
        }}>
          <div style={{
            width: '100%',
            maxWidth: '540px',
            background: 'var(--panel)',
            border: '1px solid var(--border)',
            borderRadius: '14px',
            padding: '26px',
            boxShadow: '0 20px 45px rgba(0,0,0,0.4)',
            maxHeight: '90vh',
            overflowY: 'auto'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <div style={{ width: '38px', height: '38px', borderRadius: '8px', background: 'rgba(99, 102, 241, 0.15)', color: '#818cf8', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <FileText size={18} />
                </div>
                <div>
                  <h3 style={{ margin: 0, fontSize: '1.15rem', fontWeight: 700, color: 'var(--text)' }}>
                    Analyst Access Request
                  </h3>
                  <div style={{ fontSize: '0.76rem', color: 'var(--muted)' }}>
                    Application ID #{selectedRequestDetails.id}
                  </div>
                </div>
              </div>
              <button
                onClick={() => setSelectedRequestDetails(null)}
                style={{ background: 'none', border: 'none', color: 'var(--muted)', cursor: 'pointer' }}
              >
                <X size={18} />
              </button>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '14px', marginBottom: '20px' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div style={{ background: 'var(--bg)', padding: '10px 12px', borderRadius: '8px', border: '1px solid var(--border)' }}>
                  <div style={{ fontSize: '0.72rem', color: 'var(--muted)', textTransform: 'uppercase', fontWeight: 600 }}>Full Name</div>
                  <div style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text)', marginTop: '2px' }}>
                    {selectedRequestDetails.fullName || '—'}
                  </div>
                </div>
                <div style={{ background: 'var(--bg)', padding: '10px 12px', borderRadius: '8px', border: '1px solid var(--border)' }}>
                  <div style={{ fontSize: '0.72rem', color: 'var(--muted)', textTransform: 'uppercase', fontWeight: 600 }}>Username</div>
                  <div style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text)', marginTop: '2px' }}>
                    @{selectedRequestDetails.username}
                  </div>
                </div>
              </div>

              <div style={{ background: 'var(--bg)', padding: '10px 12px', borderRadius: '8px', border: '1px solid var(--border)' }}>
                <div style={{ fontSize: '0.72rem', color: 'var(--muted)', textTransform: 'uppercase', fontWeight: 600 }}>Contact Email</div>
                <div style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text)', marginTop: '2px' }}>
                  {selectedRequestDetails.email}
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div style={{ background: 'var(--bg)', padding: '10px 12px', borderRadius: '8px', border: '1px solid var(--border)' }}>
                  <div style={{ fontSize: '0.72rem', color: 'var(--muted)', textTransform: 'uppercase', fontWeight: 600 }}>Organization</div>
                  <div style={{ fontSize: '0.86rem', color: 'var(--text)', marginTop: '2px' }}>
                    {selectedRequestDetails.organization || '—'}
                  </div>
                </div>
                <div style={{ background: 'var(--bg)', padding: '10px 12px', borderRadius: '8px', border: '1px solid var(--border)' }}>
                  <div style={{ fontSize: '0.72rem', color: 'var(--muted)', textTransform: 'uppercase', fontWeight: 600 }}>Department</div>
                  <div style={{ fontSize: '0.86rem', color: 'var(--text)', marginTop: '2px' }}>
                    {selectedRequestDetails.department || '—'}
                  </div>
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div style={{ background: 'var(--bg)', padding: '10px 12px', borderRadius: '8px', border: '1px solid var(--border)' }}>
                  <div style={{ fontSize: '0.72rem', color: 'var(--muted)', textTransform: 'uppercase', fontWeight: 600 }}>Job Title</div>
                  <div style={{ fontSize: '0.86rem', color: 'var(--text)', marginTop: '2px' }}>
                    {selectedRequestDetails.jobTitle || '—'}
                  </div>
                </div>
                <div style={{ background: 'var(--bg)', padding: '10px 12px', borderRadius: '8px', border: '1px solid var(--border)' }}>
                  <div style={{ fontSize: '0.72rem', color: 'var(--muted)', textTransform: 'uppercase', fontWeight: 600 }}>Requested Role</div>
                  <div style={{ fontSize: '0.86rem', fontWeight: 700, color: '#818cf8', marginTop: '2px' }}>
                    ANALYST
                  </div>
                </div>
              </div>

              <div style={{ background: 'var(--bg)', padding: '10px 12px', borderRadius: '8px', border: '1px solid var(--border)' }}>
                <div style={{ fontSize: '0.72rem', color: 'var(--muted)', textTransform: 'uppercase', fontWeight: 600 }}>Account Status</div>
                <div style={{ marginTop: '4px' }}>
                  <span style={{
                    display: 'inline-block',
                    padding: '3px 10px',
                    borderRadius: '8px',
                    fontSize: '0.78rem',
                    fontWeight: 700,
                    background: selectedRequestDetails.accountStatus === 'ACTIVE'
                      ? 'rgba(16, 185, 129, 0.15)'
                      : selectedRequestDetails.accountStatus === 'REJECTED'
                        ? 'rgba(239, 68, 68, 0.15)'
                        : 'rgba(245, 158, 11, 0.15)',
                    color: selectedRequestDetails.accountStatus === 'ACTIVE'
                      ? '#10b981'
                      : selectedRequestDetails.accountStatus === 'REJECTED'
                        ? '#ef4444'
                        : '#fbbf24',
                    border: `1px solid ${selectedRequestDetails.accountStatus === 'ACTIVE'
                      ? 'rgba(16, 185, 129, 0.3)'
                      : selectedRequestDetails.accountStatus === 'REJECTED'
                        ? 'rgba(239, 68, 68, 0.3)'
                        : 'rgba(245, 158, 11, 0.3)'}`
                  }}>
                    {selectedRequestDetails.accountStatus}
                  </span>
                </div>
              </div>

              {selectedRequestDetails.approvedBy && (
                <div style={{ background: 'rgba(16, 185, 129, 0.08)', padding: '10px 12px', borderRadius: '8px', border: '1px solid rgba(16, 185, 129, 0.25)' }}>
                  <div style={{ fontSize: '0.72rem', color: '#10b981', fontWeight: 700, textTransform: 'uppercase' }}>Approval Audit</div>
                  <div style={{ fontSize: '0.84rem', color: 'var(--text)', marginTop: '2px' }}>
                    Approved by <b>{selectedRequestDetails.approvedBy}</b> at {selectedRequestDetails.approvedAt ? new Date(selectedRequestDetails.approvedAt).toLocaleString() : '—'}
                  </div>
                </div>
              )}

              {selectedRequestDetails.rejectionReason && (
                <div style={{ background: 'rgba(239, 68, 68, 0.08)', padding: '10px 12px', borderRadius: '8px', border: '1px solid rgba(239, 68, 68, 0.25)' }}>
                  <div style={{ fontSize: '0.72rem', color: '#ef4444', fontWeight: 700, textTransform: 'uppercase' }}>Rejection Reason</div>
                  <div style={{ fontSize: '0.84rem', color: 'var(--text)', marginTop: '2px' }}>
                    {selectedRequestDetails.rejectionReason}
                  </div>
                </div>
              )}
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <button
                type="button"
                onClick={() => setSelectedRequestDetails(null)}
                className="button button-secondary"
              >
                Close
              </button>

              {selectedRequestDetails.accountStatus === 'PENDING_APPROVAL' && (
                <div style={{ display: 'flex', gap: '8px' }}>
                  <button
                    type="button"
                    onClick={() => {
                      const req = selectedRequestDetails;
                      setSelectedRequestDetails(null);
                      setRejectingRequest(req);
                      setRejectionReason('');
                    }}
                    style={{
                      padding: '8px 14px',
                      borderRadius: '8px',
                      border: '1px solid rgba(239, 68, 68, 0.4)',
                      background: 'rgba(239, 68, 68, 0.15)',
                      color: '#ef4444',
                      fontWeight: 600,
                      fontSize: '0.84rem',
                      cursor: 'pointer'
                    }}
                  >
                    Reject Request
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      const req = selectedRequestDetails;
                      setSelectedRequestDetails(null);
                      handleApproveRequest(req.id, req.fullName || req.username);
                    }}
                    style={{
                      padding: '8px 16px',
                      borderRadius: '8px',
                      border: 'none',
                      background: '#10b981',
                      color: '#ffffff',
                      fontWeight: 600,
                      fontSize: '0.84rem',
                      cursor: 'pointer'
                    }}
                  >
                    Approve Request
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* REJECT REQUEST MODAL */}
      {rejectingRequest && (
        <div style={{
          position: 'fixed',
          inset: 0,
          background: 'rgba(0,0,0,0.75)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '16px',
          zIndex: 1000,
          backdropFilter: 'blur(3px)'
        }}>
          <div style={{
            width: '100%',
            maxWidth: '480px',
            background: 'var(--panel)',
            border: '1px solid var(--border)',
            borderRadius: '14px',
            padding: '24px',
            boxShadow: '0 20px 45px rgba(0,0,0,0.4)'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <div style={{ width: '36px', height: '36px', borderRadius: '8px', background: 'rgba(239, 68, 68, 0.15)', color: '#ef4444', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <XCircle size={18} />
                </div>
                <div>
                  <h3 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 700, color: 'var(--text)' }}>
                    Reject Analyst Request
                  </h3>
                  <div style={{ fontSize: '0.76rem', color: 'var(--muted)' }}>
                    Application for {rejectingRequest.fullName} (@{rejectingRequest.username})
                  </div>
                </div>
              </div>
              <button
                onClick={() => setRejectingRequest(null)}
                style={{ background: 'none', border: 'none', color: 'var(--muted)', cursor: 'pointer' }}
              >
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleRejectSubmit}>
              <p style={{ fontSize: '0.84rem', color: 'var(--muted)', margin: '0 0 12px 0', lineHeight: 1.45 }}>
                Rejecting this request will mark the account as <b style={{ color: '#ef4444' }}>REJECTED</b> and deny access to the Analyst portal.
              </p>

              <div style={{ marginBottom: '16px' }}>
                <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 600, color: 'var(--text)', marginBottom: '6px' }}>
                  Rejection Reason (Optional, stored for audit purposes)
                </label>
                <textarea
                  rows={3}
                  value={rejectionReason}
                  onChange={(e) => setRejectionReason(e.target.value)}
                  placeholder="e.g. Applicant organizational affiliation could not be verified."
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: '1px solid var(--border)',
                    background: 'var(--bg)',
                    color: 'var(--text)',
                    fontSize: '0.84rem',
                    outline: 'none',
                    boxSizing: 'border-box',
                    fontFamily: 'inherit',
                    resize: 'vertical'
                  }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
                <button
                  type="button"
                  onClick={() => setRejectingRequest(null)}
                  className="button button-secondary"
                  disabled={actionLoading}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={actionLoading}
                  style={{
                    padding: '8px 16px',
                    borderRadius: '8px',
                    border: 'none',
                    background: '#ef4444',
                    color: '#ffffff',
                    fontWeight: 600,
                    fontSize: '0.84rem',
                    cursor: actionLoading ? 'not-allowed' : 'pointer'
                  }}
                >
                  {actionLoading ? 'Rejecting...' : 'Confirm Rejection'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
