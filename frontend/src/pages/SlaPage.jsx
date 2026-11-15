import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../auth';
import { useApi, useReferenceData } from '../hooks';
import { ErrorAlert, Field, Loading, PriorityBadge, StatusBadge } from '../components/ui';
import { fmtDate, fmtMinutes } from '../format';

const BLANK = { name: '', priority: 'P3', serviceId: '', responseMinutes: 60, resolutionMinutes: 480, warningThresholdPercent: 75, active: true };

export default function SlaPage() {
  const { user } = useAuth();
  const admin = user.role === 'ADMIN';
  const policies = useApi('/sla/policies');
  const violations = useApi('/sla/violations');
  const { services } = useReferenceData();
  const [editing, setEditing] = useState(null);
  const [error, setError] = useState(null);
  const [monitor, setMonitor] = useState(null);

  const save = async (e) => {
    e.preventDefault();
    setError(null);
    const body = {
      ...editing,
      serviceId: editing.serviceId ? Number(editing.serviceId) : null,
      responseMinutes: Number(editing.responseMinutes),
      resolutionMinutes: Number(editing.resolutionMinutes),
      warningThresholdPercent: Number(editing.warningThresholdPercent),
    };
    try {
      if (editing.id) await api('/sla/policies/' + editing.id, { method: 'PUT', body });
      else await api('/sla/policies', { method: 'POST', body });
      setEditing(null);
      policies.reload();
    } catch (err) {
      setError(err);
    }
  };

  const runMonitor = async () => {
    setMonitor(await api('/sla/monitor/run', { method: 'POST' }));
    violations.reload();
  };

  return (
    <div className="stack">
      <div className="page-head">
        <div>
          <h1>SLA policies & violations</h1>
          <p className="muted">Response and resolution targets per priority. A service-specific policy overrides the default one.</p>
        </div>
        {admin && (
          <div className="row">
            <button className="secondary" onClick={runMonitor}>▶ Run SLA monitor now</button>
            <button onClick={() => setEditing(BLANK)}>➕ New policy</button>
          </div>
        )}
      </div>
      {monitor && (
        <div className="alert success">
          Monitor checked {monitor.checked} open incidents: {monitor.warnings} warning(s), {monitor.violations} new violation(s),
          {' '}{monitor.escalations} escalation(s), {monitor.autoClosed} auto-closed.
        </div>
      )}

      {editing && (
        <form className="card" onSubmit={save}>
          <h2>{editing.id ? 'Edit policy' : 'New SLA policy'}</h2>
          <ErrorAlert error={error} />
          <div className="grid grid-4">
            <Field label="Name"><input value={editing.name} onChange={(e) => setEditing({ ...editing, name: e.target.value })} required /></Field>
            <Field label="Priority">
              <select value={editing.priority} onChange={(e) => setEditing({ ...editing, priority: e.target.value })}>
                {['P1', 'P2', 'P3', 'P4'].map((p) => <option key={p}>{p}</option>)}
              </select>
            </Field>
            <Field label="Service (empty = default)">
              <select value={editing.serviceId ?? ''} onChange={(e) => setEditing({ ...editing, serviceId: e.target.value })}>
                <option value="">All services (default)</option>
                {services.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </Field>
            <Field label="Warning at % of time used">
              <input type="number" min={10} max={99} value={editing.warningThresholdPercent} onChange={(e) => setEditing({ ...editing, warningThresholdPercent: e.target.value })} />
            </Field>
            <Field label="Response target (minutes)">
              <input type="number" min={1} value={editing.responseMinutes} onChange={(e) => setEditing({ ...editing, responseMinutes: e.target.value })} />
            </Field>
            <Field label="Resolution target (minutes)">
              <input type="number" min={1} value={editing.resolutionMinutes} onChange={(e) => setEditing({ ...editing, resolutionMinutes: e.target.value })} />
            </Field>
            <Field label="Status">
              <label className="checkbox"><input type="checkbox" checked={editing.active} onChange={(e) => setEditing({ ...editing, active: e.target.checked })} /> Active</label>
            </Field>
          </div>
          <div className="row">
            <button type="submit">Save</button>
            <button type="button" className="secondary" onClick={() => setEditing(null)}>Cancel</button>
          </div>
        </form>
      )}

      <div className="card">
        <h2>Policies</h2>
        {policies.loading && !policies.data ? <Loading /> : (
          <table>
            <thead><tr><th>Name</th><th>Priority</th><th>Applies to</th><th>Response</th><th>Resolution</th><th>Warning at</th><th>Active</th>{admin && <th />}</tr></thead>
            <tbody>
              {(policies.data ?? []).map((p) => (
                <tr key={p.id}>
                  <td>{p.name}</td><td><PriorityBadge value={p.priority} /></td><td>{p.serviceName}</td>
                  <td>{fmtMinutes(p.responseMinutes)}</td><td>{fmtMinutes(p.resolutionMinutes)}</td>
                  <td>{p.warningThresholdPercent}%</td><td>{p.active ? '✓' : '—'}</td>
                  {admin && <td><button className="ghost small" onClick={() => setEditing({ ...p, serviceId: p.serviceId ?? '' })}>Edit</button></td>}
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="card">
        <h2>Violations ({violations.data?.length ?? 0})</h2>
        {violations.loading && !violations.data ? <Loading /> : (
          violations.data?.length ? (
            <table>
              <thead><tr><th>Incident</th><th>Type</th><th>Priority</th><th>Due</th><th>Detected</th><th>Technician</th><th>Now</th></tr></thead>
              <tbody>
                {violations.data.map((v) => (
                  <tr key={v.id}>
                    <td><Link className="mono" to={'/incidents/code/' + v.incidentCode}>{v.incidentCode}</Link><div className="small">{v.incidentTitle}</div></td>
                    <td><span className="badge b-BREACHED">{v.type.toLowerCase()}</span></td>
                    <td><PriorityBadge value={v.priority} /></td>
                    <td className="small">{fmtDate(v.dueAt)}</td>
                    <td className="small">{fmtDate(v.detectedAt)}</td>
                    <td className="small">{v.technicianName ?? '—'}</td>
                    <td><StatusBadge value={v.currentStatus} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : <div className="empty">No SLA violations 🎉</div>
        )}
      </div>
    </div>
  );
}
