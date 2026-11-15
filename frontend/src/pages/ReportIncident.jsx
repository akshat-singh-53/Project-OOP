import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api';
import { useReferenceData } from '../hooks';
import { ErrorAlert, Field, StatusBadge } from '../components/ui';

const EMPTY = { title: '', description: '', serviceId: '', categoryId: '', impact: 'MEDIUM', urgency: 'MEDIUM', affectedUsers: 1 };

export default function ReportIncident() {
  const { services, categories, enums } = useReferenceData();
  const navigate = useNavigate();
  const [form, setForm] = useState(EMPTY);
  const [suggestions, setSuggestions] = useState([]);
  const [duplicates, setDuplicates] = useState([]);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const [created, setCreated] = useState(null);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });
  const fieldErrors = error?.body?.fieldErrors ?? {};

  // Debounced live analysis while the user is typing.
  const text = `${form.title} ${form.description}`.trim();
  useEffect(() => {
    if (text.length < 8) {
      setSuggestions([]);
      setDuplicates([]);
      return undefined;
    }
    const body = { text, serviceId: form.serviceId || null };
    const t = setTimeout(() => {
      api('/intelligence/classify', { method: 'POST', body }).then(setSuggestions).catch(() => {});
      api('/intelligence/duplicates', { method: 'POST', body }).then(setDuplicates).catch(() => {});
    }, 450);
    return () => clearTimeout(t);
  }, [text, form.serviceId]);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const res = await api('/incidents', {
        method: 'POST',
        body: {
          ...form,
          serviceId: form.serviceId ? Number(form.serviceId) : null,
          categoryId: form.categoryId ? Number(form.categoryId) : null,
          affectedUsers: Number(form.affectedUsers) || 1,
        },
      });
      setCreated(res);
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  };

  if (created) {
    const s = created.incident.summary;
    return (
      <div className="card" style={{ maxWidth: 720 }}>
        <h1>✅ Incident {s.code} created</h1>
        <p className="muted">The system analysed your report and did the following automatically:</p>
        <dl className="kv" style={{ margin: '14px 0' }}>
          <dt>Category</dt><dd>{s.category?.name ?? '—'} {created.incident.categorySuggestedAutomatically && <span className="badge b-accent">auto</span>}</dd>
          <dt>Priority</dt><dd><span className={'badge b-' + s.priority}>{s.priority}</span> <span className="small muted">{created.incident.priorityExplanation}</span></dd>
          <dt>Status</dt><dd><StatusBadge value={s.status} /></dd>
          <dt>Assignment</dt><dd>{created.assignmentMessage}</dd>
          <dt>Respond by</dt><dd>{new Date(s.responseDueAt).toLocaleString()}</dd>
          <dt>Resolve by</dt><dd>{new Date(s.resolutionDueAt).toLocaleString()}</dd>
        </dl>
        {created.possibleDuplicates?.length > 0 && (
          <div className="alert warning">
            This looks similar to {created.possibleDuplicates.map((d) => d.incidentCode).join(', ')}. The support team has been told,
            and may link the incidents.
          </div>
        )}
        <div className="row">
          <button onClick={() => navigate('/incidents/' + s.id)}>Open incident</button>
          <button className="secondary" onClick={() => { setCreated(null); setForm(EMPTY); }}>Report another</button>
        </div>
      </div>
    );
  }

  return (
    <div className="stack">
      <div className="page-head">
        <div>
          <h1>Report an incident</h1>
          <p className="muted">Describe the problem in your own words. We'll categorise it, set its priority and route it to the right engineer.</p>
        </div>
      </div>
      <div className="grid detail-grid">
        <form className="card" onSubmit={submit}>
          <ErrorAlert error={error} />
          <Field label="Affected service *" error={fieldErrors.serviceId}>
            <select value={form.serviceId} onChange={set('serviceId')} required>
              <option value="">Select the service that is affected…</option>
              {services.filter((s) => s.active).map((s) => (
                <option key={s.id} value={s.id}>{s.name} (criticality {s.businessCriticality}/5)</option>
              ))}
            </select>
          </Field>
          <Field label="Short title *" error={fieldErrors.title}>
            <input value={form.title} onChange={set('title')} placeholder="e.g. Outlook crashes when opening attachments" maxLength={150} required />
          </Field>
          <Field label="Description *" error={fieldErrors.description}>
            <textarea value={form.description} onChange={set('description')} rows={6} required
                      placeholder="What happened? When did it start? Any error message? What have you tried?" />
          </Field>
          <div className="grid grid-3">
            <Field label="Impact *" error={fieldErrors.impact}>
              <select value={form.impact} onChange={set('impact')}>
                {(enums.impacts ?? []).map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
              <div className="muted small">{enums.impacts?.find((o) => o.value === form.impact)?.description}</div>
            </Field>
            <Field label="Urgency *" error={fieldErrors.urgency}>
              <select value={form.urgency} onChange={set('urgency')}>
                {(enums.urgencies ?? []).map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
              <div className="muted small">{enums.urgencies?.find((o) => o.value === form.urgency)?.description}</div>
            </Field>
            <Field label="Users affected" error={fieldErrors.affectedUsers}>
              <input type="number" min={1} value={form.affectedUsers} onChange={set('affectedUsers')} />
            </Field>
          </div>
          <Field label="Category (optional — leave empty for automatic)">
            <select value={form.categoryId} onChange={set('categoryId')}>
              <option value="">Let the system decide</option>
              {categories.filter((c) => c.active).map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </Field>
          <div className="row between">
            <Link to="/incidents">Cancel</Link>
            <button type="submit" disabled={busy}>{busy ? 'Submitting…' : 'Submit incident'}</button>
          </div>
        </form>

        <div className="stack">
          <div className="card">
            <h2>🧠 Live analysis</h2>
            {suggestions.length === 0 && <p className="muted small">Start typing a title and description to see the suggested category.</p>}
            {suggestions.slice(0, 3).map((s, i) => (
              <div key={s.categoryId} className="suggestion">
                <div className="row between">
                  <strong>{i === 0 ? '⭐ ' : ''}{s.name}</strong>
                  <span className="badge b-accent">{Math.round(s.confidence * 100)}%</span>
                </div>
                {s.evidence?.length > 0 && <div className="muted small">matched: {s.evidence.join(', ')}</div>}
                <div className="row" style={{ marginTop: 6 }}>
                  <span className="muted small">source: {s.source}</span>
                  <button type="button" className="ghost small" onClick={() => setForm({ ...form, categoryId: String(s.categoryId) })}>Use this</button>
                </div>
              </div>
            ))}
          </div>
          <div className="card">
            <h2>🔍 Similar open incidents</h2>
            {duplicates.length === 0 ? (
              <p className="muted small">No similar open incidents found.</p>
            ) : (
              <>
                <div className="alert warning">Someone may already have reported this. Check before submitting.</div>
                {duplicates.map((d) => (
                  <div key={d.incidentCode} className="note">
                    <div className="row between">
                      <Link to={'/incidents/code/' + d.incidentCode} className="mono">{d.incidentCode}</Link>
                      <span className="badge b-AT_RISK">{Math.round(d.score * 100)}% match</span>
                    </div>
                    <div style={{ fontWeight: 600 }}>{d.title}</div>
                    <div className="muted small">{d.serviceName} · {d.status.replaceAll('_', ' ').toLowerCase()}</div>
                  </div>
                ))}
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
