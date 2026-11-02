import React from 'react';

export default function DataTable({
  columns,
  rows,
  loading,
  error,
  emptyMessage = 'No records found.'
}) {
  return (
    <div className="table-wrap">
      {error && <p className="error">{error}</p>}
      {loading ? (
        <div className="empty">Loading records…</div>
      ) : !rows || rows.length === 0 ? (
        <div className="empty">{error ? 'Records could not be loaded.' : emptyMessage}</div>
      ) : (
        <div className="scroll" role="region" aria-label="Data table" tabIndex={0}>
          <table>
            <thead>
              <tr>
                {columns.map(col => (
                  <th
                    key={col.key}
                    className={col.className || ''}
                    style={col.style}
                  >
                    {col.label}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {rows.map((row, index) => (
                <tr key={row.id ?? index}>
                  {columns.map(col => (
                    <td
                      key={col.key}
                      className={col.className || ''}
                      style={col.style}
                    >
                      {col.render ? col.render(row) : row[col.key]}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
