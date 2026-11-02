import React, { useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  ShieldCheck,
  Eye,
  EyeOff,
  Lock,
  Mail,
  ArrowRight,
  ArrowLeft,
  AlertCircle,
  KeyRound,
  X
} from 'lucide-react';

export default function RoleLoginView({
  portalRole, // 'ADMIN' | 'ANALYST' | 'VIEWER'
  title,
  subtitle,
  emailLabel,
  emailPlaceholder,
  buttonText,
  badgeText,
  accentColor,
  accentGlow,
  demoEmail,
  demoPassword,
  defaultEmail, // backward compat
  defaultPassword, // backward compat
  showRegisterLink = false
}) {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();

  // Inputs always start strictly blank
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [showForgotPassword, setShowForgotPassword] = useState(false);

  const redirectPath = location.state?.from?.pathname || '/';

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (loading) return; // Prevent duplicate requests
    setError('');

    const cleanEmail = email.trim();
    if (!cleanEmail) {
      setError(`Please enter your registered ${portalRole.toLowerCase()} email.`);
      return;
    }

    // Email format validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(cleanEmail)) {
      setError('Please enter a valid email address (e.g. analyst@company.com).');
      return;
    }

    if (!password) {
      setError('Please enter your account password.');
      return;
    }

    setLoading(true);
    try {
      await login({
        email: cleanEmail,
        username: cleanEmail,
        password: password,
        requestedPortal: portalRole
      });
      navigate(redirectPath, { replace: true });
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.response?.data?.error ||
        err.message ||
        'Authentication failed. Please verify your email, password, and portal authorization.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      flexDirection: 'column',
      justifyContent: 'center',
      alignItems: 'center',
      padding: '24px 16px',
      background: 'linear-gradient(135deg, #090d16 0%, #0f172a 50%, #1e1b4b 100%)',
      color: '#f8fafc',
      fontFamily: 'Inter, system-ui, -apple-system, sans-serif'
    }}>
      {/* Brand Header */}
      <div style={{ textAlign: 'center', marginBottom: '24px' }}>
        <div style={{
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          width: '52px',
          height: '52px',
          borderRadius: '14px',
          background: 'linear-gradient(135deg, #4f46e5 0%, #3730a3 100%)',
          color: '#ffffff',
          boxShadow: '0 8px 24px rgba(79, 70, 229, 0.35)',
          marginBottom: '10px'
        }}>
          <ShieldCheck size={30} />
        </div>
        <h1 style={{ fontSize: '1.6rem', fontWeight: 800, margin: '0 0 4px 0', letterSpacing: '-0.02em', color: '#ffffff' }}>
          RecallRadar
        </h1>
        <p style={{ fontSize: '0.82rem', color: '#94a3b8', margin: 0, textTransform: 'uppercase', letterSpacing: '0.04em', fontWeight: 600 }}>
          AI Product Safety & Risk Intelligence
        </p>
      </div>

      {/* Main Login Card */}
      <div style={{
        width: '100%',
        maxWidth: '440px',
        background: '#0f172a',
        border: `1px solid ${accentColor}40`,
        borderRadius: '16px',
        padding: '30px 28px',
        boxShadow: `0 20px 45px rgba(0, 0, 0, 0.45), 0 0 30px ${accentGlow}`,
        backdropFilter: 'blur(10px)'
      }}>
        {/* Back Link & Badge */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px' }}>
          <Link
            to="/login"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              fontSize: '0.78rem',
              color: '#94a3b8',
              textDecoration: 'none',
              fontWeight: 500
            }}
          >
            <ArrowLeft size={14} />
            <span>All Portals</span>
          </Link>

          <span style={{
            fontSize: '0.7rem',
            fontWeight: 700,
            letterSpacing: '0.04em',
            padding: '2px 8px',
            borderRadius: '10px',
            background: `${accentColor}1a`,
            color: accentColor,
            border: `1px solid ${accentColor}40`
          }}>
            {badgeText}
          </span>
        </div>

        {/* Title & Subtitle */}
        <div style={{ marginBottom: '22px' }}>
          <h2 style={{ fontSize: '1.28rem', fontWeight: 700, margin: '0 0 6px 0', color: '#ffffff' }}>
            {title}
          </h2>
          <p style={{ fontSize: '0.84rem', color: '#94a3b8', margin: 0, lineHeight: 1.45 }}>
            {subtitle}
          </p>
        </div>

        {/* Error Alert */}
        {error && (
          <div style={{
            display: 'flex',
            alignItems: 'flex-start',
            gap: '10px',
            padding: '12px 14px',
            borderRadius: '8px',
            background: 'rgba(239, 68, 68, 0.12)',
            border: '1px solid rgba(239, 68, 68, 0.3)',
            color: '#fca5a5',
            fontSize: '0.84rem',
            lineHeight: 1.45,
            marginBottom: '18px'
          }}>
            <AlertCircle size={17} style={{ flexShrink: 0, marginTop: '2px', color: '#ef4444' }} />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          {/* Email Field */}
          <div style={{ marginBottom: '16px' }}>
            <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: '#cbd5e1', marginBottom: '6px' }}>
              {emailLabel || `${portalRole.charAt(0) + portalRole.slice(1).toLowerCase()} email`}
            </label>
            <div style={{ position: 'relative' }}>
              <span style={{
                position: 'absolute',
                left: '12px',
                top: '50%',
                transform: 'translateY(-50%)',
                color: '#64748b',
                display: 'flex'
              }}>
                <Mail size={16} />
              </span>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder={emailPlaceholder || `Enter your registered ${portalRole.toLowerCase()} email`}
                autoComplete="email"
                disabled={loading}
                style={{
                  width: '100%',
                  padding: '10px 12px 10px 38px',
                  borderRadius: '8px',
                  border: '1px solid #334155',
                  background: '#1e293b',
                  color: '#ffffff',
                  fontSize: '0.88rem',
                  outline: 'none',
                  boxSizing: 'border-box'
                }}
              />
            </div>
          </div>

          {/* Password Field */}
          <div style={{ marginBottom: '16px' }}>
            <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: '#cbd5e1', marginBottom: '6px' }}>
              Password
            </label>
            <div style={{ position: 'relative' }}>
              <span style={{
                position: 'absolute',
                left: '12px',
                top: '50%',
                transform: 'translateY(-50%)',
                color: '#64748b',
                display: 'flex'
              }}>
                <Lock size={16} />
              </span>
              <input
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Enter your account password"
                autoComplete="current-password"
                disabled={loading}
                style={{
                  width: '100%',
                  padding: '10px 40px 10px 38px',
                  borderRadius: '8px',
                  border: '1px solid #334155',
                  background: '#1e293b',
                  color: '#ffffff',
                  fontSize: '0.88rem',
                  outline: 'none',
                  boxSizing: 'border-box'
                }}
              />
              <button
                type="button"
                onClick={() => setShowPassword((v) => !v)}
                style={{
                  position: 'absolute',
                  right: '10px',
                  top: '50%',
                  transform: 'translateY(-50%)',
                  background: 'none',
                  border: 'none',
                  color: '#94a3b8',
                  cursor: 'pointer',
                  padding: '4px',
                  display: 'flex'
                }}
                tabIndex={-1}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
              >
                {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
          </div>

          {/* Remember Session & Forgot Password */}
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '22px' }}>
            <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.8rem', color: '#94a3b8', cursor: 'pointer' }}>
              <input
                type="checkbox"
                checked={rememberMe}
                onChange={(e) => setRememberMe(e.target.checked)}
                style={{ accentColor: accentColor, cursor: 'pointer' }}
              />
              <span>Remember session</span>
            </label>
            <button
              type="button"
              onClick={() => setShowForgotPassword(true)}
              style={{
                background: 'none',
                border: 'none',
                color: accentColor,
                fontSize: '0.78rem',
                fontWeight: 500,
                cursor: 'pointer',
                padding: '0',
                textDecoration: 'none'
              }}
            >
              Forgot password?
            </button>
          </div>

          {/* Submit Button */}
          <button
            type="submit"
            disabled={loading}
            style={{
              width: '100%',
              padding: '11px 16px',
              borderRadius: '8px',
              border: 'none',
              background: loading ? '#4338ca' : `linear-gradient(135deg, ${accentColor} 0%, #312e81 100%)`,
              color: '#ffffff',
              fontSize: '0.9rem',
              fontWeight: 600,
              cursor: loading ? 'not-allowed' : 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '8px',
              boxShadow: `0 4px 14px ${accentGlow}`,
              transition: 'opacity 0.2s'
            }}
          >
            {loading ? (
              <>
                <span className="spinner" style={{
                  width: '16px',
                  height: '16px',
                  border: '2px solid rgba(255,255,255,0.3)',
                  borderTopColor: '#fff',
                  borderRadius: '50%',
                  animation: 'spin 0.6s linear infinite'
                }} />
                <span>Authenticating...</span>
              </>
            ) : (
              <>
                <span>{buttonText}</span>
                <ArrowRight size={16} />
              </>
            )}
          </button>
        </form>

        {/* Analyst Portal Access Request Link */}
        {portalRole === 'ANALYST' && (
          <div style={{
            marginTop: '16px',
            paddingTop: '12px',
            borderTop: '1px solid rgba(255, 255, 255, 0.08)',
            textAlign: 'center',
            fontSize: '0.82rem',
            color: '#94a3b8'
          }}>
            New to RecallRadar?{' '}
            <Link
              to="/register/analyst"
              style={{ color: accentColor, fontWeight: 600, textDecoration: 'none' }}
            >
              Request Analyst Access
            </Link>
          </div>
        )}

        {/* Optional Register Link for other roles */}
        {showRegisterLink && portalRole !== 'ANALYST' && (
          <div style={{
            marginTop: '16px',
            paddingTop: '12px',
            borderTop: '1px solid rgba(255, 255, 255, 0.08)',
            textAlign: 'center',
            fontSize: '0.84rem',
            color: '#94a3b8'
          }}>
            Don't have an enterprise account?{' '}
            <Link
              to="/register"
              style={{ color: '#818cf8', fontWeight: 600, textDecoration: 'none' }}
            >
              Create account
            </Link>
          </div>
        )}
      </div>

      {/* Forgot Password Security Notice Modal */}
      {showForgotPassword && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0, 0, 0, 0.75)',
          backdropFilter: 'blur(6px)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 9999,
          padding: '16px'
        }}>
          <div style={{
            background: '#0f172a',
            border: '1px solid #334155',
            borderRadius: '14px',
            width: '100%',
            maxWidth: '460px',
            padding: '24px',
            boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.6)'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <KeyRound size={20} color={accentColor} />
                <h3 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 700, color: '#f8fafc' }}>
                  Password Reset Procedure
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setShowForgotPassword(false)}
                style={{
                  background: 'none',
                  border: 'none',
                  color: '#94a3b8',
                  cursor: 'pointer',
                  padding: '4px',
                  display: 'flex'
                }}
              >
                <X size={18} />
              </button>
            </div>
            <p style={{ fontSize: '0.85rem', color: '#cbd5e1', lineHeight: 1.5, margin: '0 0 14px 0' }}>
              For enterprise security compliance, automated password resets via unverified public endpoints are disabled.
            </p>
            <div style={{
              background: '#1e293b',
              border: '1px solid #334155',
              borderRadius: '8px',
              padding: '12px 14px',
              fontSize: '0.8rem',
              color: '#94a3b8',
              lineHeight: 1.5,
              marginBottom: '18px'
            }}>
              <strong style={{ color: '#f8fafc', display: 'block', marginBottom: '4px' }}>
                How to reset your password:
              </strong>
              1. Contact your RecallRadar Organization Administrator.<br />
              2. Or contact the security operations team at <span style={{ color: accentColor }}>security@recallradar.com</span> with your registered email and username.
            </div>
            <button
              type="button"
              onClick={() => setShowForgotPassword(false)}
              style={{
                width: '100%',
                padding: '9px 16px',
                borderRadius: '8px',
                border: 'none',
                background: accentColor,
                color: '#ffffff',
                fontWeight: 600,
                fontSize: '0.85rem',
                cursor: 'pointer'
              }}
            >
              Understood
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
