import { label } from '../format';

export const PriorityBadge = ({ value }) =>
  value ? <span className={'badge b-' + value}>{value}</span> : <span className="muted">—</span>;

export const StatusBadge = ({ value }) => <span className={'badge b-status b-' + value}>{label(value)}</span>;

export const SlaBadge = ({ value }) => (value ? <span className={'badge b-' + value}>{label(value)}</span> : null);

export function Stat({ label: text, value, tone = '', hint }) {
  return (
    <div className={'card stat ' + tone}>
      <span className="label">{text}</span>
      <span className="value">{value}</span>
      {hint && <span className="muted small">{hint}</span>}
    </div>
  );
}

export function ErrorAlert({ error }) {
  if (!error) return null;
  const fields = error.body?.fieldErrors;
  return (
    <div className="alert error">
      {error.message}
      {fields && (
        <ul style={{ margin: '6px 0 0', paddingLeft: 18 }}>
          {Object.entries(fields).map(([k, v]) => <li key={k}>{k}: {v}</li>)}
        </ul>
      )}
    </div>
  );
}

export function Loading() {
  return <div className="empty">Loading…</div>;
}

export function Field({ label: text, children, error }) {
  return (
    <div className="field">
      <label>{text}</label>
      {children}
      {error && <div className="field-error">{error}</div>}
    </div>
  );
}

export function Bar({ value, max = 100, tone }) {
  const pct = Math.max(0, Math.min(100, (100 * value) / (max || 1)));
  return <div className={'bar ' + (tone ?? '')}><span style={{ width: pct + '%' }} /></div>;
}
