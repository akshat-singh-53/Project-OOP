import { useState } from 'react';
import { api } from '../../api';
import { useApi } from '../../hooks';
import { ErrorAlert, Loading } from '../../components/ui';

const TOGGLES = [
  ['assignment.auto', 'Assign new incidents automatically', 'When off, new incidents wait in the unassigned queue for a team lead.'],
  ['escalation.enabled', 'Automatic escalation', 'Escalate P1-P2 incidents not acknowledged in time to team lead → system admin → IT manager.'],
  ['notification.email.enabled', 'E-mail notifications', 'Simulated e-mail gateway — messages appear in the outbox.'],
  ['notification.sms.enabled', 'SMS notifications', 'Third-party SMS API through an adapter — urgent incidents only.'],
  ['ai.classifier.enabled', 'Use LLM classifier', 'Ask an OpenAI-compatible model for a category when an API key is configured.'],
];

export default function AdminSettings() {
  const settings = useApi('/settings');
  const [error, setError] = useState(null);
  const [message, setMessage] = useState(null);

  if (settings.loading && !settings.data) return <Loading />;
  if (settings.error) return <ErrorAlert error={settings.error} />;
  const s = settings.data;
  const v = s.values;

  const update = async (changes) => {
    setError(null);
    try {
      settings.setData(await api('/settings', { method: 'PUT', body: changes }));
      setMessage('Saved');
      setTimeout(() => setMessage(null), 2500);
    } catch (err) {
      setError(err);
    }
  };

  return (
    <div className="stack" style={{ maxWidth: 1000 }}>
      <div className="page-head">
        <div>
          <h1>Strategies & settings</h1>
          <p className="muted">Switch algorithms at runtime — each one is a Strategy implementation picked from a registry.</p>
        </div>
      </div>
      {message && <div className="alert success">{message}</div>}
      <ErrorAlert error={error} />

      <div className="grid grid-2">
        <StrategyPicker title="Technician assignment strategy" options={s.assignmentStrategies}
                        value={v['assignment.strategy']} onChange={(key) => update({ 'assignment.strategy': key })} />
        <StrategyPicker title="Priority calculation strategy" options={s.priorityStrategies}
                        value={v['priority.strategy']} onChange={(key) => update({ 'priority.strategy': key })} />
      </div>

      <div className="card">
        <h2>Automation</h2>
        {TOGGLES.map(([key, title, hint]) => (
          <div key={key} className="row between" style={{ padding: '10px 0', borderBottom: '1px solid var(--border)' }}>
            <div>
              <strong>{title}</strong>
              <div className="muted small">{hint}</div>
              {key === 'notification.email.enabled' && (
                <div className="small" style={{ marginTop: 2 }}>
                  {s.emailDelivery.startsWith('Real') ? '✅ ' : '⚪ '}{s.emailDelivery}
                </div>
              )}
              {key === 'ai.classifier.enabled' && (
                <div className="small" style={{ marginTop: 2 }}>
                  {s.llmConfigured ? `✅ API key configured (model ${s.llmModel})` : '⚪ No API key — set ITSM_AI_API_KEY to enable. The built-in keyword + history classifier is used meanwhile.'}
                </div>
              )}
            </div>
            <label className="checkbox">
              <input type="checkbox" checked={v[key] === 'true'} onChange={(e) => update({ [key]: String(e.target.checked) })} />
              {v[key] === 'true' ? 'On' : 'Off'}
            </label>
          </div>
        ))}
        <div className="row between" style={{ padding: '10px 0' }}>
          <div>
            <strong>Auto-close resolved incidents after (hours)</strong>
            <div className="muted small">Resolved incidents that the reporter does not verify are closed automatically.</div>
          </div>
          <AutoClose value={v['incident.autoCloseHours']} onSave={(h) => update({ 'incident.autoCloseHours': h })} />
        </div>
      </div>

      <TestEmail onError={setError} onSent={(m) => { setMessage(m); setTimeout(() => setMessage(null), 5000); }} />
    </div>
  );
}

function StrategyPicker({ title, options, value, onChange }) {
  return (
    <div className="card">
      <h2>{title}</h2>
      {options.map((o) => (
        <label key={o.key} className="note" style={{ display: 'block', cursor: 'pointer', borderColor: value === o.key ? 'var(--accent)' : undefined, background: value === o.key ? 'var(--accent-soft)' : undefined }}>
          <div className="row">
            <input type="radio" name={title} checked={value === o.key} onChange={() => onChange(o.key)} style={{ width: 'auto' }} />
            <strong>{o.name}</strong>
            <span className="mono muted">{o.key}</span>
          </div>
          <div className="small muted" style={{ marginTop: 4, fontWeight: 400 }}>{o.description}</div>
        </label>
      ))}
    </div>
  );
}

function TestEmail({ onSent, onError }) {
  const [to, setTo] = useState('');
  const [busy, setBusy] = useState(false);
  const send = async (e) => {
    e.preventDefault();
    setBusy(true);
    onError(null);
    try {
      const res = await api('/settings/test-email', { method: 'POST', body: { to } });
      onSent(`${res.message}. ${res.delivery}.`);
    } catch (err) {
      onError(err);
    } finally {
      setBusy(false);
    }
  };
  return (
    <form className="card" onSubmit={send}>
      <h2>Send a test e-mail</h2>
      <p className="muted small">Checks the e-mail channel end to end. With real SMTP configured, the message arrives within a few seconds.</p>
      <div className="row">
        <input type="email" required placeholder="someone@gmail.com" value={to} onChange={(e) => setTo(e.target.value)} style={{ maxWidth: 340 }} />
        <button type="submit" disabled={busy || !to}>{busy ? 'Sending…' : 'Send test e-mail'}</button>
      </div>
    </form>
  );
}

function AutoClose({ value, onSave }) {
  const [hours, setHours] = useState(value ?? '72');
  return (
    <div className="row">
      <input type="number" min={1} value={hours} onChange={(e) => setHours(e.target.value)} style={{ width: 90 }} />
      <button className="small" disabled={String(hours) === String(value)} onClick={() => onSave(String(hours))}>Save</button>
    </div>
  );
}
