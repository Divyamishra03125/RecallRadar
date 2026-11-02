import React from 'react';

/**
 * Safely parses and renders inline bold, code, and markdown links within text lines.
 */
export function renderInlineSpans(str) {
  if (!str) return '';

  // Match bold (**text**), inline code (`code`), or links ([text](url))
  const tokenRegex = /(\*\*[^*]+\*\*|`[^`]+`|\[[^\]]+\]\([^)]+\))/g;
  const parts = str.split(tokenRegex);

  return parts.map((part, i) => {
    if (!part) return null;

    // Bold: **text**
    if (part.startsWith('**') && part.endsWith('**')) {
      return <strong key={i}>{part.slice(2, -2)}</strong>;
    }

    // Inline Code: `code`
    if (part.startsWith('`') && part.endsWith('`')) {
      return (
        <code
          key={i}
          style={{
            background: 'rgba(99, 102, 241, 0.12)',
            color: '#818cf8',
            padding: '2px 6px',
            borderRadius: '4px',
            fontSize: '0.86em',
            fontFamily: 'monospace'
          }}
        >
          {part.slice(1, -1)}
        </code>
      );
    }

    // Link: [text](url)
    const linkMatch = part.match(/^\[([^\]]+)\]\(([^)]+)\)$/);
    if (linkMatch) {
      const href = linkMatch[2];
      const isExternal = href.startsWith('http://') || href.startsWith('https://');
      return (
        <a
          key={i}
          href={href}
          target={isExternal ? '_blank' : undefined}
          rel={isExternal ? 'noopener noreferrer' : undefined}
          style={{
            color: '#6366f1',
            textDecoration: 'underline',
            fontWeight: 500
          }}
        >
          {linkMatch[1]}
        </a>
      );
    }

    return part;
  });
}

/**
 * Enterprise-grade Markdown and Table Renderer for RecallRadar AI responses.
 * Renders headings, tables, bullet points, numbered lists, and paragraphs cleanly without raw markdown tags.
 */
export default function FormattedContent({ text }) {
  if (!text) return null;

  const lines = text.split('\n');
  const elements = [];
  let tableRows = [];
  let inTable = false;

  const flushTable = (key) => {
    if (tableRows.length > 0) {
      const headerRow = tableRows[0];
      const bodyRows = tableRows
        .slice(1)
        .filter((r) => !r.every((cell) => /^[:-\s]+$/.test(cell)));

      elements.push(
        <div key={`table-${key}`} style={{ overflowX: 'auto', margin: '10px 0' }}>
          <table
            style={{
              width: '100%',
              borderCollapse: 'collapse',
              fontSize: '0.84rem',
              border: '1px solid var(--border)',
              borderRadius: '6px',
              overflow: 'hidden'
            }}
          >
            <thead>
              <tr style={{ background: 'var(--panel)', borderBottom: '2px solid var(--border)' }}>
                {headerRow.map((h, i) => (
                  <th
                    key={i}
                    style={{
                      padding: '8px 10px',
                      textAlign: 'left',
                      fontWeight: 600,
                      color: 'var(--text)'
                    }}
                  >
                    {renderInlineSpans(h.trim())}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {bodyRows.map((row, rIdx) => (
                <tr
                  key={rIdx}
                  style={{
                    borderBottom: '1px solid var(--border)',
                    background: rIdx % 2 === 0 ? 'transparent' : 'rgba(255,255,255,0.02)'
                  }}
                >
                  {row.map((cell, cIdx) => (
                    <td key={cIdx} style={{ padding: '8px 10px', color: 'var(--text)' }}>
                      {renderInlineSpans(cell.trim())}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      );
      tableRows = [];
    }
    inTable = false;
  };

  for (let idx = 0; idx < lines.length; idx++) {
    const rawLine = lines[idx];
    const trimmed = rawLine.trim();

    // Table rows
    if (trimmed.startsWith('|') && trimmed.endsWith('|')) {
      inTable = true;
      const cells = trimmed
        .slice(1, -1)
        .split('|')
        .map((c) => c.trim());
      tableRows.push(cells);
      continue;
    } else if (inTable) {
      flushTable(idx);
    }

    // Empty lines
    if (!trimmed) {
      elements.push(<div key={`empty-${idx}`} style={{ height: '6px' }} />);
      continue;
    }

    // Horizontal Rule: ---
    if (trimmed === '---' || trimmed === '***') {
      elements.push(
        <hr
          key={`hr-${idx}`}
          style={{
            border: 'none',
            borderTop: '1px solid var(--border)',
            margin: '12px 0'
          }}
        />
      );
      continue;
    }

    // Header 3: ###
    if (trimmed.startsWith('### ')) {
      elements.push(
        <h4
          key={`h3-${idx}`}
          style={{
            margin: '12px 0 6px 0',
            fontSize: '0.98rem',
            fontWeight: 700,
            color: 'var(--text)'
          }}
        >
          {renderInlineSpans(trimmed.slice(4))}
        </h4>
      );
      continue;
    }

    // Header 2: ##
    if (trimmed.startsWith('## ')) {
      elements.push(
        <h3
          key={`h2-${idx}`}
          style={{
            margin: '14px 0 8px 0',
            fontSize: '1.08rem',
            fontWeight: 700,
            color: 'var(--text)'
          }}
        >
          {renderInlineSpans(trimmed.slice(3))}
        </h3>
      );
      continue;
    }

    // Header 1: #
    if (trimmed.startsWith('# ')) {
      elements.push(
        <h2
          key={`h1-${idx}`}
          style={{
            margin: '16px 0 8px 0',
            fontSize: '1.18rem',
            fontWeight: 700,
            color: 'var(--text)'
          }}
        >
          {renderInlineSpans(trimmed.slice(2))}
        </h2>
      );
      continue;
    }

    // Bullet points: * or - or •
    if (trimmed.startsWith('* ') || trimmed.startsWith('- ') || trimmed.startsWith('• ')) {
      const sliceIdx = trimmed.startsWith('• ') ? 2 : 2;
      elements.push(
        <div
          key={`bullet-${idx}`}
          style={{
            display: 'flex',
            alignItems: 'flex-start',
            gap: '8px',
            paddingLeft: '6px',
            lineHeight: 1.55,
            fontSize: '0.88rem'
          }}
        >
          <span style={{ color: '#6366f1', fontWeight: 'bold' }}>•</span>
          <span>{renderInlineSpans(trimmed.slice(sliceIdx))}</span>
        </div>
      );
      continue;
    }

    // Numbered list: 1.
    const matchNumber = trimmed.match(/^(\d+)\.\s+(.*)$/);
    if (matchNumber) {
      elements.push(
        <div
          key={`num-${idx}`}
          style={{
            display: 'flex',
            alignItems: 'flex-start',
            gap: '8px',
            paddingLeft: '4px',
            lineHeight: 1.55,
            fontSize: '0.88rem'
          }}
        >
          <span style={{ color: '#6366f1', fontWeight: 600, minWidth: '18px' }}>
            {matchNumber[1]}.
          </span>
          <span>{renderInlineSpans(matchNumber[2])}</span>
        </div>
      );
      continue;
    }

    // Standard paragraph
    elements.push(
      <p key={`p-${idx}`} style={{ margin: '4px 0', lineHeight: 1.55, fontSize: '0.88rem' }}>
        {renderInlineSpans(trimmed)}
      </p>
    );
  }

  if (inTable) {
    flushTable(lines.length);
  }

  return (
    <div className="formatted-content" style={{ display: 'grid', gap: '4px' }}>
      {elements}
    </div>
  );
}
