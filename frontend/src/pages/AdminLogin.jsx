import React from 'react';
import RoleLoginView from '../components/RoleLoginView';

export default function AdminLogin() {
  return (
    <RoleLoginView
      portalRole="ADMIN"
      title="Administrator Login"
      subtitle="Secure access to the RecallRadar administration workspace."
      emailLabel="Admin email"
      emailPlaceholder="Enter your registered admin email"
      buttonText="Admin Sign In"
      badgeText="ADMIN PORTAL"
      accentColor="#10b981"
      accentGlow="rgba(16, 185, 129, 0.25)"
      showRegisterLink={false}
    />
  );
}
