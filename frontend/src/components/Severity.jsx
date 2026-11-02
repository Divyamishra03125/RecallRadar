import React from 'react';
import { normalizeSeverity } from '../services/api';

export default function Severity({ value }) {
  const norm = normalizeSeverity(value);
  const isKnown = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'].includes(norm);
  const badgeClass = isKnown ? norm.toLowerCase() : 'unknown';
  const label = norm === 'UNKNOWN' ? (value ? String(value).trim().toUpperCase() : 'UNKNOWN') : norm;

  return (
    <span
      className={`badge badge-${badgeClass}`}
      title={`Severity: ${label}`}
    >
      {label}
    </span>
  );
}
