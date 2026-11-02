import React from 'react';
import RoleLoginView from '../components/RoleLoginView';

export default function ViewerLogin() {
  return (
    <RoleLoginView
      portalRole="VIEWER"
      title="Viewer Login"
      subtitle="Access product safety intelligence and recall information."
      emailLabel="Viewer email"
      emailPlaceholder="Enter your registered viewer email"
      buttonText="Viewer Sign In"
      badgeText="VIEWER PORTAL"
      accentColor="#38bdf8"
      accentGlow="rgba(56, 189, 248, 0.25)"
      showRegisterLink={true}
    />
  );
}
