export function fmtDate(value) {
  if (!value) return '—';
  return new Date(value).toLocaleString(undefined, {
    day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit',
  });
}

export function fmtMinutes(min) {
  if (min === null || min === undefined) return '—';
  const m = Math.round(Math.abs(min));
  if (m < 60) return `${m} min`;
  const h = Math.floor(m / 60);
  if (h < 48) return `${h}h ${m % 60}m`;
  return `${Math.floor(h / 24)}d ${h % 24}h`;
}

/** "in 12 min" / "8 min ago" relative to now. */
export function relative(value) {
  if (!value) return '—';
  const diff = (new Date(value).getTime() - Date.now()) / 60000;
  return diff >= 0 ? `in ${fmtMinutes(diff)}` : `${fmtMinutes(diff)} ago`;
}

export const label = (s) => (s
  ? s.replaceAll('_', ' ').toLowerCase().replace(/^\w/, (c) => c.toUpperCase()).replace(/\bsla\b/gi, 'SLA')
  : '—');
