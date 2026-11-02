import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  ShieldCheck,
  User,
  AtSign,
  Mail,
  Lock,
  Eye,
  EyeOff,
  Building2,
  Briefcase,
  Layers,
  ArrowRight,
  ArrowLeft,
  AlertCircle,
  CheckCircle2,
  Info
} from 'lucide-react';
import { authApi } from '../services/api';

export default function AnalystRegister() {
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    fullName: '',
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
    organization: '',
    department: '',
    jobTitle: '',
    termsAccepted: false
  });

  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [submittedSuccess, setSubmittedSuccess] = useState(false);
  const [registeredEmail, setRegisteredEmail] = useState('');

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  // Password strength calculation
  const calculatePasswordStrength = (pwd) => {
    if (!pwd) return { score: 0, label: 'None', color: '#64748b' };
    let score = 0;
    if (pwd.length >= 12) score += 1;
    if (pwd.length >= 16) score += 1;
    if (/[A-Z]/.test(pwd)) score += 1;
    if (/[a-z]/.test(pwd)) score += 1;
    if (/[0-9]/.test(pwd)) score += 1;
    if (/[^A-Za-z0-9]/.test(pwd)) score += 1;

    if (score <= 2) return { score: 1, label: 'Weak', color: '#ef4444' };
    if (score <= 4) return { score: 2, label: 'Moderate', color: '#f59e0b' };
    if (score <= 5) return { score: 3, label: 'Strong', color: '#10b981' };
    return { score: 4, label: 'Very Strong', color: '#059669' };
  };

  const strength = calculatePasswordStrength(formData.password);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (loading) return;
    setError('');

    // Validations
    if (!formData.fullName.trim()) {
      setError('Please enter your full name.');
      return;
    }
    if (!formData.username.trim()) {
      setError('Please choose a username.');
      return;
    }
    if (formData.username.trim().length < 3) {
      setError('Username must be at least 3 characters.');
      return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!formData.email.trim() || !emailRegex.test(formData.email.trim())) {
      setError('Please provide a valid professional email address.');
      return;
    }

    if (!formData.password) {
      setError('Please enter a password.');
      return;
    }
    if (formData.password.length < 12) {
      setError('Password must be at least 12 characters long for security compliance.');
      return;
    }

    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match. Please re-enter your confirm password.');
      return;
    }

    if (!formData.termsAccepted) {
      setError('You must accept the RecallRadar Platform Terms & Acceptable Use Policy.');
      return;
    }

    setLoading(true);

    try {
      const payload = {
        fullName: formData.fullName.trim(),
        username: formData.username.trim(),
        email: formData.email.trim().toLowerCase(),
        password: formData.password,
        confirmPassword: formData.confirmPassword,
        organization: formData.organization.trim() || null,
        department: formData.department.trim() || null,
        jobTitle: formData.jobTitle.trim() || null,
        termsAccepted: formData.termsAccepted
      };

      await authApi.registerAnalyst(payload);
      setRegisteredEmail(payload.email);
      setSubmittedSuccess(true);
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.response?.data?.error ||
        err.message ||
        'Failed to submit Analyst access request. Please check your information.';
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
      padding: '32px 16px',
      background: 'linear-gradient(135deg, #090d16 0%, #0f172a 50%, #1e1b4b 100%)',
      color: '#f8fafc',
      fontFamily: 'Inter, system-ui, -apple-system, sans-serif'
    }}>
      {/* Brand Header */}
      <div style={{ textAlign: 'center', marginBottom: '20px' }}>
        <div style={{
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          width: '52px',
          height: '52px',
          borderRadius: '14px',
          background: 'linear-gradient(135deg, #6366f1 0%, #4338ca 100%)',
          color: '#ffffff',
          boxShadow: '0 8px 24px rgba(99, 102, 241, 0.35)',
          marginBottom: '10px'
        }}>
          <ShieldCheck size={30} />
        </div>
        <h1 style={{ fontSize: '1.6rem', fontWeight: 800, margin: '0 0 4px 0', letterSpacing: '-0.02em', color: '#ffffff' }}>
          RecallRadar
        </h1>
        <p style={{ fontSize: '0.82rem', color: '#94a3b8', margin: 0, textTransform: 'uppercase', letterSpacing: '0.04em', fontWeight: 600 }}>
          Product Safety & Risk Intelligence Platform
        </p>
      </div>

      {/* Main Card Container */}
      <div style={{
        width: '100%',
        maxWidth: '560px',
        background: '#0f172a',
        border: '1px solid rgba(129, 140, 248, 0.25)',
        borderRadius: '16px',
        padding: '32px 28px',
        boxShadow: '0 20px 45px rgba(0, 0, 0, 0.45), 0 0 30px rgba(99, 102, 241, 0.15)',
        backdropFilter: 'blur(10px)'
      }}>
        {/* Navigation & Badge */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px' }}>
          <Link
            to="/login/analyst"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              fontSize: '0.8rem',
              color: '#94a3b8',
              textDecoration: 'none',
              fontWeight: 500
            }}
          >
            <ArrowLeft size={14} />
            <span>Return to Analyst Login</span>
          </Link>

          <span style={{
            fontSize: '0.72rem',
            fontWeight: 700,
            letterSpacing: '0.04em',
            padding: '3px 9px',
            borderRadius: '10px',
            background: 'rgba(99, 102, 241, 0.15)',
            color: '#818cf8',
            border: '1px solid rgba(99, 102, 241, 0.3)'
          }}>
            ANALYST PORTAL REGISTRATION
          </span>
        </div>

        {submittedSuccess ? (
          /* Confirmation Success View */
          <div style={{ textAlign: 'center', padding: '16px 8px' }}>
            <div style={{
              width: '64px',
              height: '64px',
              borderRadius: '50%',
              background: 'rgba(16, 185, 129, 0.15)',
              border: '2px solid rgba(16, 185, 129, 0.4)',
              color: '#10b981',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              marginBottom: '20px'
            }}>
              <CheckCircle2 size={36} />
            </div>

            <h2 style={{ fontSize: '1.4rem', fontWeight: 700, color: '#ffffff', marginBottom: '12px' }}>
              Access Request Submitted
            </h2>

            <div style={{
              background: 'rgba(30, 41, 59, 0.8)',
              border: '1px solid #334155',
              borderRadius: '12px',
              padding: '20px',
              textAlign: 'left',
              marginBottom: '24px'
            }}>
              <p style={{ margin: '0 0 12px 0', fontSize: '0.92rem', color: '#e2e8f0', lineHeight: 1.55 }}>
                Your Analyst access request has been submitted successfully. Your account is awaiting administrator approval. You will be able to access the Analyst workspace after your request has been reviewed and approved.
              </p>
              <div style={{ fontSize: '0.82rem', color: '#94a3b8', borderTop: '1px solid #334155', paddingTop: '10px' }}>
                <span style={{ fontWeight: 600, color: '#cbd5e1' }}>Registered Email:</span> {registeredEmail}
                <br />
                <span style={{ fontWeight: 600, color: '#cbd5e1' }}>Current Status:</span>{' '}
                <span style={{
                  display: 'inline-block',
                  background: 'rgba(245, 158, 11, 0.15)',
                  color: '#fbbf24',
                  border: '1px solid rgba(245, 158, 11, 0.3)',
                  padding: '2px 8px',
                  borderRadius: '6px',
                  fontSize: '0.74rem',
                  fontWeight: 700,
                  marginTop: '4px'
                }}>
                  PENDING_APPROVAL
                </span>
              </div>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              <Link
                to="/login/analyst"
                style={{
                  width: '100%',
                  padding: '12px',
                  borderRadius: '8px',
                  background: 'linear-gradient(135deg, #6366f1 0%, #4338ca 100%)',
                  color: '#ffffff',
                  fontWeight: 600,
                  fontSize: '0.9rem',
                  textDecoration: 'none',
                  display: 'inline-flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '8px',
                  boxShadow: '0 4px 14px rgba(99, 102, 241, 0.35)',
                  boxSizing: 'border-box'
                }}
              >
                <span>Go to Analyst Login</span>
                <ArrowRight size={16} />
              </Link>
              <Link
                to="/login"
                style={{
                  color: '#94a3b8',
                  fontSize: '0.84rem',
                  textDecoration: 'none',
                  padding: '6px'
                }}
              >
                Back to All Portals
              </Link>
            </div>
          </div>
        ) : (
          /* Registration Form View */
          <>
            <div style={{ marginBottom: '22px' }}>
              <h2 style={{ fontSize: '1.35rem', fontWeight: 700, margin: '0 0 6px 0', color: '#ffffff' }}>
                Analyst Access Request
              </h2>
              <p style={{ fontSize: '0.84rem', color: '#94a3b8', margin: 0, lineHeight: 1.45 }}>
                Submit an access request for product safety analysis and recall investigation capabilities.
              </p>
            </div>

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
                marginBottom: '20px'
              }}>
                <AlertCircle size={17} style={{ flexShrink: 0, marginTop: '2px', color: '#ef4444' }} />
                <span>{error}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} noValidate>
              {/* Row 1: Full Name & Username */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px', marginBottom: '14px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 600, color: '#cbd5e1', marginBottom: '6px' }}>
                    Full Name *
                  </label>
                  <div style={{ position: 'relative' }}>
                    <span style={{ position: 'absolute', left: '11px', top: '50%', transform: 'translateY(-50%)', color: '#64748b', display: 'flex' }}>
                      <User size={15} />
                    </span>
                    <input
                      type="text"
                      name="fullName"
                      value={formData.fullName}
                      onChange={handleChange}
                      placeholder="Jane Doe"
                      disabled={loading}
                      required
                      style={{
                        width: '100%',
                        padding: '9px 12px 9px 34px',
                        borderRadius: '8px',
                        border: '1px solid #334155',
                        background: '#1e293b',
                        color: '#ffffff',
                        fontSize: '0.84rem',
                        outline: 'none',
                        boxSizing: 'border-box'
                      }}
                    />
                  </div>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 600, color: '#cbd5e1', marginBottom: '6px' }}>
                    Username *
                  </label>
                  <div style={{ position: 'relative' }}>
                    <span style={{ position: 'absolute', left: '11px', top: '50%', transform: 'translateY(-50%)', color: '#64748b', display: 'flex' }}>
                      <AtSign size={15} />
                    </span>
                    <input
                      type="text"
                      name="username"
                      value={formData.username}
                      onChange={handleChange}
                      placeholder="janedoe_analyst"
                      disabled={loading}
                      required
                      style={{
                        width: '100%',
                        padding: '9px 12px 9px 34px',
                        borderRadius: '8px',
                        border: '1px solid #334155',
                        background: '#1e293b',
                        color: '#ffffff',
                        fontSize: '0.84rem',
                        outline: 'none',
                        boxSizing: 'border-box'
                      }}
                    />
                  </div>
                </div>
              </div>

              {/* Email */}
              <div style={{ marginBottom: '14px' }}>
                <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 600, color: '#cbd5e1', marginBottom: '6px' }}>
                  Professional / Corporate Email *
                </label>
                <div style={{ position: 'relative' }}>
                  <span style={{ position: 'absolute', left: '11px', top: '50%', transform: 'translateY(-50%)', color: '#64748b', display: 'flex' }}>
                    <Mail size={15} />
                  </span>
                  <input
                    type="email"
                    name="email"
                    value={formData.email}
                    onChange={handleChange}
                    placeholder="jane.doe@enterprise.com"
                    disabled={loading}
                    required
                    style={{
                      width: '100%',
                      padding: '9px 12px 9px 34px',
                      borderRadius: '8px',
                      border: '1px solid #334155',
                      background: '#1e293b',
                      color: '#ffffff',
                      fontSize: '0.84rem',
                      outline: 'none',
                      boxSizing: 'border-box'
                    }}
                  />
                </div>
              </div>

              {/* Row 2: Password & Confirm Password */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px', marginBottom: '8px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 600, color: '#cbd5e1', marginBottom: '6px' }}>
                    Password (min 12 chars) *
                  </label>
                  <div style={{ position: 'relative' }}>
                    <span style={{ position: 'absolute', left: '11px', top: '50%', transform: 'translateY(-50%)', color: '#64748b', display: 'flex' }}>
                      <Lock size={15} />
                    </span>
                    <input
                      type={showPassword ? 'text' : 'password'}
                      name="password"
                      value={formData.password}
                      onChange={handleChange}
                      placeholder="••••••••••••"
                      disabled={loading}
                      required
                      style={{
                        width: '100%',
                        padding: '9px 36px 9px 34px',
                        borderRadius: '8px',
                        border: '1px solid #334155',
                        background: '#1e293b',
                        color: '#ffffff',
                        fontSize: '0.84rem',
                        outline: 'none',
                        boxSizing: 'border-box'
                      }}
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword((v) => !v)}
                      style={{
                        position: 'absolute',
                        right: '8px',
                        top: '50%',
                        transform: 'translateY(-50%)',
                        background: 'none',
                        border: 'none',
                        color: '#94a3b8',
                        cursor: 'pointer',
                        padding: '2px',
                        display: 'flex'
                      }}
                      tabIndex={-1}
                    >
                      {showPassword ? <EyeOff size={15} /> : <Eye size={15} />}
                    </button>
                  </div>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 600, color: '#cbd5e1', marginBottom: '6px' }}>
                    Confirm Password *
                  </label>
                  <div style={{ position: 'relative' }}>
                    <span style={{ position: 'absolute', left: '11px', top: '50%', transform: 'translateY(-50%)', color: '#64748b', display: 'flex' }}>
                      <Lock size={15} />
                    </span>
                    <input
                      type={showConfirmPassword ? 'text' : 'password'}
                      name="confirmPassword"
                      value={formData.confirmPassword}
                      onChange={handleChange}
                      placeholder="••••••••••••"
                      disabled={loading}
                      required
                      style={{
                        width: '100%',
                        padding: '9px 36px 9px 34px',
                        borderRadius: '8px',
                        border: '1px solid #334155',
                        background: '#1e293b',
                        color: '#ffffff',
                        fontSize: '0.84rem',
                        outline: 'none',
                        boxSizing: 'border-box'
                      }}
                    />
                    <button
                      type="button"
                      onClick={() => setShowConfirmPassword((v) => !v)}
                      style={{
                        position: 'absolute',
                        right: '8px',
                        top: '50%',
                        transform: 'translateY(-50%)',
                        background: 'none',
                        border: 'none',
                        color: '#94a3b8',
                        cursor: 'pointer',
                        padding: '2px',
                        display: 'flex'
                      }}
                      tabIndex={-1}
                    >
                      {showConfirmPassword ? <EyeOff size={15} /> : <Eye size={15} />}
                    </button>
                  </div>
                </div>
              </div>

              {/* Password Strength Meter */}
              {formData.password && (
                <div style={{ marginBottom: '14px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                    <span style={{ fontSize: '0.72rem', color: '#94a3b8' }}>
                      Length: {formData.password.length} chars (min 12 required)
                    </span>
                    <span style={{ fontSize: '0.72rem', fontWeight: 700, color: strength.color }}>
                      Strength: {strength.label}
                    </span>
                  </div>
                  <div style={{ height: '4px', background: '#334155', borderRadius: '2px', overflow: 'hidden' }}>
                    <div style={{
                      height: '100%',
                      width: `${(strength.score / 4) * 100}%`,
                      background: strength.color,
                      transition: 'width 0.3s ease'
                    }} />
                  </div>
                </div>
              )}

              {/* Optional Fields Section Header */}
              <div style={{
                marginTop: '16px',
                marginBottom: '10px',
                paddingTop: '12px',
                borderTop: '1px solid rgba(255, 255, 255, 0.08)',
                display: 'flex',
                alignItems: 'center',
                gap: '6px'
              }}>
                <span style={{ fontSize: '0.76rem', fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                  Professional Details (Optional)
                </span>
              </div>

              {/* Organization & Department */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px', marginBottom: '14px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 600, color: '#94a3b8', marginBottom: '6px' }}>
                    Organization / Company
                  </label>
                  <div style={{ position: 'relative' }}>
                    <span style={{ position: 'absolute', left: '11px', top: '50%', transform: 'translateY(-50%)', color: '#64748b', display: 'flex' }}>
                      <Building2 size={15} />
                    </span>
                    <input
                      type="text"
                      name="organization"
                      value={formData.organization}
                      onChange={handleChange}
                      placeholder="Acme Safety Corp"
                      disabled={loading}
                      style={{
                        width: '100%',
                        padding: '9px 12px 9px 34px',
                        borderRadius: '8px',
                        border: '1px solid #334155',
                        background: '#1e293b',
                        color: '#ffffff',
                        fontSize: '0.84rem',
                        outline: 'none',
                        boxSizing: 'border-box'
                      }}
                    />
                  </div>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 600, color: '#94a3b8', marginBottom: '6px' }}>
                    Department
                  </label>
                  <div style={{ position: 'relative' }}>
                    <span style={{ position: 'absolute', left: '11px', top: '50%', transform: 'translateY(-50%)', color: '#64748b', display: 'flex' }}>
                      <Layers size={15} />
                    </span>
                    <input
                      type="text"
                      name="department"
                      value={formData.department}
                      onChange={handleChange}
                      placeholder="Quality Assurance / Risk"
                      disabled={loading}
                      style={{
                        width: '100%',
                        padding: '9px 12px 9px 34px',
                        borderRadius: '8px',
                        border: '1px solid #334155',
                        background: '#1e293b',
                        color: '#ffffff',
                        fontSize: '0.84rem',
                        outline: 'none',
                        boxSizing: 'border-box'
                      }}
                    />
                  </div>
                </div>
              </div>

              {/* Job Title */}
              <div style={{ marginBottom: '16px' }}>
                <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 600, color: '#94a3b8', marginBottom: '6px' }}>
                  Job Title
                </label>
                <div style={{ position: 'relative' }}>
                  <span style={{ position: 'absolute', left: '11px', top: '50%', transform: 'translateY(-50%)', color: '#64748b', display: 'flex' }}>
                    <Briefcase size={15} />
                  </span>
                  <input
                    type="text"
                    name="jobTitle"
                    value={formData.jobTitle}
                    onChange={handleChange}
                    placeholder="Product Safety Officer / Risk Analyst"
                    disabled={loading}
                    style={{
                      width: '100%',
                      padding: '9px 12px 9px 34px',
                      borderRadius: '8px',
                      border: '1px solid #334155',
                      background: '#1e293b',
                      color: '#ffffff',
                      fontSize: '0.84rem',
                      outline: 'none',
                      boxSizing: 'border-box'
                    }}
                  />
                </div>
              </div>

              {/* Security Policy Notice */}
              <div style={{
                background: 'rgba(30, 41, 59, 0.5)',
                border: '1px solid rgba(255, 255, 255, 0.06)',
                borderRadius: '8px',
                padding: '10px 12px',
                marginBottom: '16px',
                fontSize: '0.76rem',
                color: '#94a3b8',
                display: 'flex',
                gap: '8px',
                alignItems: 'flex-start'
              }}>
                <Info size={16} style={{ color: '#818cf8', flexShrink: 0, marginTop: '2px' }} />
                <span>
                  All submitted requests are subject to administrator identity review and approval. Access will be granted once your organizational credentials are confirmed.
                </span>
              </div>

              {/* Terms & Conditions Acceptance */}
              <div style={{ marginBottom: '22px' }}>
                <label style={{ display: 'flex', alignItems: 'flex-start', gap: '9px', fontSize: '0.8rem', color: '#cbd5e1', cursor: 'pointer' }}>
                  <input
                    type="checkbox"
                    name="termsAccepted"
                    checked={formData.termsAccepted}
                    onChange={handleChange}
                    style={{ accentColor: '#6366f1', marginTop: '3px', cursor: 'pointer' }}
                    required
                  />
                  <span style={{ lineHeight: 1.45 }}>
                    I acknowledge and accept the RecallRadar Acceptable Use Policy, Data Protection standards, and certify that this request is for legitimate safety analysis purposes.
                  </span>
                </label>
              </div>

              {/* Submit Button */}
              <button
                type="submit"
                disabled={loading}
                style={{
                  width: '100%',
                  padding: '12px 16px',
                  borderRadius: '8px',
                  border: 'none',
                  background: loading ? '#4338ca' : 'linear-gradient(135deg, #6366f1 0%, #4338ca 100%)',
                  color: '#ffffff',
                  fontSize: '0.92rem',
                  fontWeight: 600,
                  cursor: loading ? 'not-allowed' : 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '8px',
                  boxShadow: '0 4px 14px rgba(99, 102, 241, 0.35)'
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
                    <span>Submitting Request...</span>
                  </>
                ) : (
                  <>
                    <span>Submit Analyst Access Request</span>
                    <ArrowRight size={16} />
                  </>
                )}
              </button>
            </form>

            {/* Bottom Links */}
            <div style={{
              marginTop: '18px',
              paddingTop: '14px',
              borderTop: '1px solid rgba(255, 255, 255, 0.08)',
              textAlign: 'center',
              fontSize: '0.82rem',
              color: '#94a3b8'
            }}>
              Already have an approved account?{' '}
              <Link
                to="/login/analyst"
                style={{ color: '#818cf8', fontWeight: 600, textDecoration: 'none' }}
              >
                Sign in here
              </Link>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
