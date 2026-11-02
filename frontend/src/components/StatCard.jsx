import React from 'react';

export default function StatCard({ label, value, note }) {
  return (
    <article className="stat">
      <small>{label}</small>
      <strong>{value}</strong>
      {note && <p>{note}</p>}
    </article>
  );
}
