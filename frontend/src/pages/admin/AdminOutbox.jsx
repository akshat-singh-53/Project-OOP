import { useState } from 'react';
import { useApi } from '../../hooks';
import { ErrorAlert, Loading } from '../../components/ui';
import { fmtDate } from '../../format';

export default function AdminOutbox() {
  const outbox = useApi('/notifications/outbox', { limit: 500 });
  const [channel, setChannel] = useState('');
  const rows = (outbox.data ?? []).filter((n) => !channel || n.channel === channel);
  const counts = (outbox.data ?? []).reduce((acc, n) => ({ ...acc, [n.channel]: (acc[n.channel] ?? 0) + 1 }), {});

  return (
    <div className="stack">
      <div className="page-head">
        <div>
          <h1>Notification outbox</h1>
          <p className="muted">Every message sent by the Observer-driven notification engine. E-mail and SMS go through simulated gateways.</p>
        </div>
        <div className="row">
          {['', 'IN_APP', 'EMAIL', 'SMS'].map((c) => (
            <button key={c} className={channel === c ? 'small' : 'secondary small'} onClick={() => setChannel(c)}>
              {c || 'All'} {c && `(${counts[c] ?? 0})`}
            </button>
          ))}
          <button className="secondary small" onClick={outbox.reload}>⟳</button>
        </div>
      </div>
      <div className="card">
        <ErrorAlert error={outbox.error} />
        {outbox.loading && !outbox.data ? <Loading /> : (
          <div className="table-wrap">
            <table>
              <thead><tr><th>When</th><th>Channel</th><th>Recipient</th><th>Destination</th><th>Subject / message</th><th>Incident</th></tr></thead>
              <tbody>
                {rows.map((n) => (
                  <tr key={n.id}>
                    <td className="small" style={{ whiteSpace: 'nowrap' }}>{fmtDate(n.createdAt)}</td>
                    <td><span className="badge b-accent">{n.channel}</span></td>
                    <td className="small">{n.recipientName}</td>
                    <td className="small mono">{n.destination}</td>
                    <td><div style={{ fontWeight: 600 }}>{n.subject}</div><div className="small muted">{n.message}</div></td>
                    <td className="mono">{n.incidentCode ?? '—'}</td>
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
