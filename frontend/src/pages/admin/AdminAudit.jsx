import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useApi } from '../../hooks';
import { ErrorAlert, Loading } from '../../components/ui';
import { fmtDate, label } from '../../format';

export default function AdminAudit() {
  const [draft, setDraft] = useState({ incidentCode: '', actor: '', action: '' });
  const [filters, setFilters] = useState(draft);
  const audit = useApi('/audit', filters);

  return (
    <div className="stack">
      <div className="page-head">
        <div>
          <h1>Audit log</h1>
          <p className="muted">Every state change, assignment, escalation, login and admin change — who did what, and when.</p>
        </div>
      </div>
      <form className="card" onSubmit={(e) => { e.preventDefault(); setFilters(draft); }}>
        <div className="filters">
          <div><label>Incident</label><input value={draft.incidentCode} onChange={(e) => setDraft({ ...draft, incidentCode: e.target.value })} placeholder="INC1001" /></div>
          <div><label>Actor</label><input value={draft.actor} onChange={(e) => setDraft({ ...draft, actor: e.target.value })} placeholder="username or SYSTEM" /></div>
          <div><label>Action</label><input value={draft.action} onChange={(e) => setDraft({ ...draft, action: e.target.value })} placeholder="ESCALATED, LOGIN…" /></div>
          <div className="row"><button type="submit">Filter</button>
            <button type="button" className="secondary" onClick={() => { const b = { incidentCode: '', actor: '', action: '' }; setDraft(b); setFilters(b); }}>Clear</button></div>
        </div>
      </form>
      <div className="card">
        <ErrorAlert error={audit.error} />
        {audit.loading && !audit.data ? <Loading /> : (
          <div className="table-wrap">
            <table>
              <thead><tr><th>When</th><th>Actor</th><th>Action</th><th>Incident</th><th>State change</th><th>Description</th></tr></thead>
              <tbody>
                {(audit.data ?? []).map((a) => (
                  <tr key={a.id}>
                    <td className="small" style={{ whiteSpace: 'nowrap' }}>{fmtDate(a.timestamp)}</td>
                    <td className="small">{a.actor}</td>
                    <td><span className="badge b-status">{a.action}</span></td>
                    <td className="mono">{a.incidentCode ? <Link to={'/incidents/code/' + a.incidentCode}>{a.incidentCode}</Link> : '—'}</td>
                    <td className="small">{a.previousState || a.newState ? `${label(a.previousState)} → ${label(a.newState)}` : ''}</td>
                    <td className="small">{a.description}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
