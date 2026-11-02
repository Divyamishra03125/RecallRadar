import React from 'react';
import RoleLoginView from '../components/RoleLoginView';

export default function AnalystLogin() {
  return (
    <RoleLoginView
      portalRole="ANALYST"
      title="Analyst Login"
      subtitle="Access your product safety investigation and recall review workspace."
      emailLabel="Analyst email"
      emailPlaceholder="Enter your registered analyst email"
      buttonText="Analyst Sign In"
      badgeText="ANALYST PORTAL"
      accentColor="#818cf8"
      accentGlow="rgba(129, 140, 248, 0.25)"
      showRegisterLink={false}
    />
  );
}
