import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useApi } from '../hooks';
import { Bar, ErrorAlert, Loading, PriorityBadge, StatusBadge } from '../components/ui';
import { fmtMinutes, label } from '../format';

export default function Intelligence() {
  const [hours, setHours] = useState(24);
  const clusters = useApi('/intelligence/correlations', { hours });
  const risks = useApi('/intelligence/sla-risks');

  return (
    <div className="stack">
      <div className="page-head">
        <div>
          <h1>SLA risk & incident correlation</h1>
          <p className="muted">Predicts which open incidents are likely to miss their SLA, and groups related incidents that may be one major incident.</p>
        </div>
        <button className="secondary" onClick={() => { clusters.reload(); risks.reload(); }}>⟳ Refresh</button>
      </div>

      <div className="card">
        <div className="row between">
          <h2>⚠ SLA breach prediction</h2>
          <span className="muted small">Uses historical resolution times per category & priority</span>
        </div>
        <ErrorAlert error={risks.error} />
        {risks.loading && !risks.data ? <Loading /> : (
          risks.data.length === 0 ? <div className="empty">No open incidents.</div> : (
            <div className="table-wrap">
              <table>
                <thead><tr><th>Incident</th><th>Priority</th><th>Status</th><th>Technician</th><th>Time left</th><th>Expected effort</th><th>Risk</th><th>Why</th></tr></thead>
                <tbody>
                  {risks.data.map((r) => (
                    <tr key={r.incidentCode}>
                      <td><Link className="mono" to={'/incidents/code/' + r.incidentCode}>{r.incidentCode}</Link><div className="small">{r.title}</div></td>
                      <td><PriorityBadge value={r.priority} /></td>
                      <td><StatusBadge value={r.status} /></td>
                      <td className="small">{r.technicianName ?? <span className="muted">Unassigned</span>}</td>
                      <td className="small">{r.minutesRemaining < 0 ? <span style={{ color: 'var(--danger)' }}>overdue {fmtMinutes(-r.minutesRemaining)}</span> : fmtMinutes(r.minutesRemaining)}</td>
                      <td className="small">{fmtMinutes(r.expectedMinutesToResolve)}</td>
                      <td style={{ minWidth: 110 }}>
                        <span className={'badge b-' + r.riskLevel}>{label(r.riskLevel)} · {r.riskScore}</span>
                        <Bar value={r.riskScore} max={100} tone={r.riskLevel === 'HIGH' ? 'danger' : r.riskLevel === 'MEDIUM' ? 'warning' : ''} />
                      </td>
                      <td className="small">{r.explanation}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )
        )}
      </div>

      <div className="card">
        <div className="row between">
          <h2>🔗 Correlated incidents</h2>
          <div className="row">
            <span className="small muted">Window</span>
            <select value={hours} onChange={(e) => setHours(Number(e.target.value))} style={{ width: 'auto' }}>
              <option value={2}>last 2 hours</option>
              <option value={6}>last 6 hours</option>
              <option value={24}>last 24 hours</option>
              <option value={72}>last 3 days</option>
              <option value={168}>last 7 days</option>
            </select>
          </div>
        </div>
        <ErrorAlert error={clusters.error} />
        {clusters.loading && !clusters.data ? <Loading /> : (
          clusters.data.length === 0 ? <div className="empty">No related incident groups in this window.</div> : (
            <div className="grid grid-2">
              {clusters.data.map((c) => (
                <div key={c.groupType + c.groupName} className={'note'} style={c.suspectedMajorIncident ? { borderColor: '#fecaca', background: '#fef2f2' } : {}}>
                  <div className="row between">
                    <strong>{c.suspectedMajorIncident ? '🚨 ' : ''}{c.groupName}</strong>
                    <span className="badge b-accent">{label(c.groupType)}</span>
                  </div>
                  <div className="small" style={{ margin: '6px 0' }}>{c.insight}</div>
                  <div className="row small">
                    <span>{c.incidentCount} incidents</span>
                    {c.highestPriority && <PriorityBadge value={c.highestPriority} />}
                  </div>
                  <div className="row small" style={{ marginTop: 6 }}>
                    {c.incidentCodes.map((code) => <Link key={code} className="mono" to={'/incidents/code/' + code}>{code}</Link>)}
                  </div>
                </div>
              ))}
            </div>
          )
        )}
      </div>
    </div>
  );
}
