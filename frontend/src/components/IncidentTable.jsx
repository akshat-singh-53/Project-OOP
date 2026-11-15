import { useNavigate } from 'react-router-dom';
import { fmtDate, relative } from '../format';
import { PriorityBadge, SlaBadge, StatusBadge } from './ui';

export default function IncidentTable({ incidents, showReporter = true, showTechnician = true, empty = 'No incidents found' }) {
  const navigate = useNavigate();
  if (!incidents?.length) return <div className="empty">{empty}</div>;
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>ID</th><th>Title</th><th>Priority</th><th>Status</th><th>Category</th><th>Service</th>
            {showReporter && <th>Reporter</th>}
            {showTechnician && <th>Technician</th>}
            <th>SLA</th><th>Created</th>
          </tr>
        </thead>
        <tbody>
          {incidents.map((i) => (
            <tr key={i.id} className="clickable" onClick={() => navigate('/incidents/' + i.id)}>
              <td className="mono">{i.code}</td>
              <td>
                <div style={{ fontWeight: 600 }}>{i.title}</div>
                {i.suspectedDuplicateCode && <span className="badge b-AT_RISK">possible duplicate of {i.suspectedDuplicateCode}</span>}
                {i.escalationLevel > 0 && <span className="badge b-BREACHED" style={{ marginLeft: 4 }}>escalated L{i.escalationLevel}</span>}
              </td>
              <td><PriorityBadge value={i.priority} /></td>
              <td><StatusBadge value={i.status} /></td>
              <td>{i.category?.name ?? '—'}</td>
              <td>{i.serviceName}</td>
              {showReporter && <td>{i.reporter?.fullName}</td>}
              {showTechnician && <td>{i.technician?.fullName ?? <span className="muted">Unassigned</span>}</td>}
              <td>
                <SlaBadge value={i.slaStatus} />
                {['RESOLVED', 'VERIFIED', 'CLOSED'].includes(i.status) ? null : (
                  <div className="muted small">due {relative(i.resolutionDueAt)}</div>
                )}
              </td>
              <td className="small">{fmtDate(i.createdAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
