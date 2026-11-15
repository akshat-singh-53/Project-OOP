import { useNavigate } from 'react-router-dom';
import { api } from '../api';
import { useApi } from '../hooks';
import { ErrorAlert, Loading } from '../components/ui';
import { fmtDate } from '../format';

const ICONS = { IN_APP: '🔔', EMAIL: '✉️', SMS: '📱' };

export default function Notifications() {
  const list = useApi('/notifications', { limit: 100 });
  const navigate = useNavigate();

  const open = async (n) => {
    if (!n.read) await api(`/notifications/${n.id}/read`, { method: 'POST' });
    if (n.incidentCode) navigate('/incidents/code/' + n.incidentCode);
    else list.reload();
  };

  return (
    <div className="stack">
      <div className="page-head">
        <div>
          <h1>Notifications</h1>
          <p className="muted">Every message sent to you, across in-app, e-mail and SMS channels.</p>
        </div>
        <button className="secondary" onClick={async () => { await api('/notifications/read-all', { method: 'POST' }); list.reload(); }}>
          Mark all as read
        </button>
      </div>
      <div className="card">
        <ErrorAlert error={list.error} />
        {list.loading && !list.data ? <Loading /> : (
          list.data.length === 0 ? <div className="empty">No notifications yet.</div> : (
            <table>
              <thead><tr><th>Channel</th><th>Subject</th><th>Incident</th><th>Sent to</th><th>When</th></tr></thead>
              <tbody>
                {list.data.map((n) => (
                  <tr key={n.id} className="clickable" onClick={() => open(n)} style={n.read ? {} : { background: 'var(--accent-soft)' }}>
                    <td>{ICONS[n.channel]} {n.channel.replace('_', '-').toLowerCase()}</td>
                    <td><div style={{ fontWeight: n.read ? 400 : 700 }}>{n.subject}</div><div className="muted small">{n.message}</div></td>
                    <td className="mono">{n.incidentCode ?? '—'}</td>
                    <td className="small">{n.destination}</td>
                    <td className="small">{fmtDate(n.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )
        )}
      </div>
    </div>
  );
}
