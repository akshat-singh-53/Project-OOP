import { Link } from 'react-router-dom';
import {
  Bar as RBar, BarChart, CartesianGrid, Cell, Legend, Line, LineChart, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts';
import { useAuth } from '../auth';
import { useApi } from '../hooks';
import IncidentTable from '../components/IncidentTable';
import { Bar, ErrorAlert, Loading, Stat } from '../components/ui';
import { fmtMinutes, label } from '../format';

const PRIORITY_COLORS = { P1: '#dc2626', P2: '#ea580c', P3: '#ca8a04', P4: '#0284c7' };
const PALETTE = ['#4f46e5', '#0d9488', '#c2410c', '#0284c7', '#a21caf', '#65a30d', '#db2777', '#475569', '#ca8a04'];

export default function Dashboard() {
  const { user } = useAuth();
  const overview = useApi('/reports/overview');
  if (overview.loading && !overview.data) return <Loading />;
  if (overview.error) return <ErrorAlert error={overview.error} />;
  const o = overview.data;

  return (
    <div className="stack">
      <div className="page-head">
        <div>
          <h1>Welcome back, {user.fullName.split(' ')[0]}</h1>
          <p className="muted">
            {user.role === 'EMPLOYEE' && 'Track the incidents you have reported and get help fast.'}
            {user.role === 'TECHNICIAN' && 'Your queue, SLA deadlines and the team workload at a glance.'}
            {user.role === 'ADMIN' && 'Service desk performance across all services and teams.'}
          </p>
        </div>
        {user.role !== 'ADMIN' && <Link to="/report"><button>➕ Report an incident</button></Link>}
      </div>

      {user.role === 'EMPLOYEE' && <EmployeeDashboard o={o} />}
      {user.role === 'TECHNICIAN' && <TechnicianDashboard o={o} user={user} />}
      {user.role === 'ADMIN' && <AdminDashboard o={o} />}
    </div>
  );
}

function EmployeeDashboard({ o }) {
  const mine = useApi('/incidents');
  const waiting = (mine.data ?? []).filter((i) => i.status === 'PENDING_INFO' || i.status === 'RESOLVED');
  return (
    <>
      <div className="grid grid-4">
        <Stat label="Open incidents" value={o.openIncidents} tone="accent" />
        <Stat label="Waiting for you" value={waiting.length} tone="warning" hint="info requested or fix to confirm" />
        <Stat label="Resolved for you" value={o.resolvedIncidents} tone="success" hint={`${o.closedIncidents} closed`} />
        <Stat label="Average resolution time" value={fmtMinutes(o.avgResolutionMinutes)} />
      </div>
      {waiting.length > 0 && (
        <div className="alert warning">
          {waiting.length} incident(s) need your attention: the support team either asked for more information or
          marked them resolved and is waiting for your confirmation.
        </div>
      )}
      <div className="card">
        <div className="row between"><h2>My incidents</h2><Link to="/incidents">View all →</Link></div>
        {mine.loading ? <Loading /> : (
          <IncidentTable incidents={(mine.data ?? []).slice(0, 8)} showReporter={false}
                         empty="You have not reported any incidents yet." />
        )}
      </div>
    </>
  );
}

function TechnicianDashboard({ o, user }) {
  const mine = useApi('/incidents', { technicianId: user.id, openOnly: true });
  const unassigned = useApi('/incidents', { unassignedOnly: true, openOnly: true });
  const risks = useApi('/intelligence/sla-risks');
  const myRisks = (risks.data ?? []).filter((r) => r.technicianName === user.fullName);
  return (
    <>
      <div className="grid grid-4">
        <Stat label="My open incidents" value={mine.data?.length ?? '…'} tone="accent"
              hint={`capacity ${user.maxWorkload ?? '—'}`} />
        <Stat label="Unassigned in queue" value={unassigned.data?.length ?? '…'} tone="warning" />
        <Stat label="My incidents at SLA risk" value={myRisks.filter((r) => r.riskLevel !== 'LOW').length} tone="danger" />
        <Stat label="Team SLA compliance" value={o.slaCompliancePercent.toFixed(0) + '%'} tone="success" />
      </div>
      <div className="card">
        <div className="row between"><h2>My assigned queue</h2><Link to="/incidents?mine=1">Open queue →</Link></div>
        {mine.loading ? <Loading /> : <IncidentTable incidents={mine.data} showTechnician={false} empty="Nothing assigned to you right now 🎉" />}
      </div>
      <div className="grid grid-2">
        <div className="card">
          <div className="row between"><h2>Unassigned incidents</h2><Link to="/incidents?unassigned=1">View →</Link></div>
          {unassigned.loading ? <Loading /> : <IncidentTable incidents={(unassigned.data ?? []).slice(0, 6)} showTechnician={false} showReporter={false} empty="Queue is empty" />}
        </div>
        <WorkloadCard rows={o.technicianWorkload} />
      </div>
    </>
  );
}

function AdminDashboard({ o }) {
  const byStatus = Object.entries(o.byStatus).filter(([, v]) => v > 0).map(([k, v]) => ({ name: label(k), value: v }));
  const byPriority = Object.entries(o.byPriority).map(([k, v]) => ({ name: k, value: v }));
  const resolutionByPriority = Object.entries(o.avgResolutionByPriority).map(([k, v]) => ({ name: k, hours: +(v / 60).toFixed(1) }));
  const trend = o.trend.map((d) => ({ ...d, day: d.date.slice(5) }));

  return (
    <>
      <div className="grid grid-4">
        <Stat label="Total incidents" value={o.totalIncidents} />
        <Stat label="Open" value={o.openIncidents} tone="accent" hint={`${o.unassigned} unassigned`} />
        <Stat label="Critical open (P1/P2)" value={o.criticalOpen} tone="danger" hint={`${o.escalated} escalated`} />
        <Stat label="SLA compliance" value={o.slaCompliancePercent.toFixed(1) + '%'} tone="success"
              hint={`${o.slaViolations} violations recorded`} />
      </div>
      <div className="grid grid-4">
        <Stat label="Resolved" value={o.resolvedIncidents} />
        <Stat label="Closed" value={o.closedIncidents} />
        <Stat label="Avg. resolution time" value={fmtMinutes(o.avgResolutionMinutes)} />
        <Stat label="Avg. user rating" value={o.avgFeedback ? o.avgFeedback.toFixed(1) + ' ★' : '—'} tone="warning" />
      </div>

      <div className="grid grid-2">
        <div className="card">
          <h2>Incident trend (14 days)</h2>
          <ResponsiveContainer width="100%" height={240}>
            <LineChart data={trend}>
              <CartesianGrid strokeDasharray="3 3" stroke="#eef1f6" />
              <XAxis dataKey="day" fontSize={11} />
              <YAxis allowDecimals={false} fontSize={11} />
              <Tooltip />
              <Legend />
              <Line type="monotone" dataKey="created" stroke="#c2410c" strokeWidth={2} />
              <Line type="monotone" dataKey="resolved" stroke="#059669" strokeWidth={2} />
            </LineChart>
          </ResponsiveContainer>
        </div>
        <div className="card">
          <h2>By status</h2>
          <ResponsiveContainer width="100%" height={240}>
            <PieChart>
              <Pie data={byStatus} dataKey="value" nameKey="name" innerRadius={50} outerRadius={85} paddingAngle={2}>
                {byStatus.map((_, i) => <Cell key={i} fill={PALETTE[i % PALETTE.length]} />)}
              </Pie>
              <Tooltip />
              <Legend />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="grid grid-3">
        <div className="card">
          <h2>By priority</h2>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={byPriority}>
              <XAxis dataKey="name" fontSize={11} />
              <YAxis allowDecimals={false} fontSize={11} />
              <Tooltip />
              <RBar dataKey="value" name="incidents">
                {byPriority.map((p) => <Cell key={p.name} fill={PRIORITY_COLORS[p.name]} />)}
              </RBar>
            </BarChart>
          </ResponsiveContainer>
        </div>
        <div className="card">
          <h2>By category</h2>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={o.byCategory} layout="vertical" margin={{ left: 30 }}>
              <XAxis type="number" allowDecimals={false} fontSize={11} />
              <YAxis type="category" dataKey="name" fontSize={10} width={90} />
              <Tooltip />
              <RBar dataKey="count" fill="#4f46e5" />
            </BarChart>
          </ResponsiveContainer>
        </div>
        <div className="card">
          <h2>Avg. resolution (hours)</h2>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={resolutionByPriority}>
              <XAxis dataKey="name" fontSize={11} />
              <YAxis fontSize={11} />
              <Tooltip />
              <RBar dataKey="hours">
                {resolutionByPriority.map((p) => <Cell key={p.name} fill={PRIORITY_COLORS[p.name]} />)}
              </RBar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="grid grid-2">
        <div className="card">
          <h2>Services</h2>
          <table>
            <thead><tr><th>Service</th><th>Criticality</th><th>Total</th><th>Open</th><th>SLA breached</th></tr></thead>
            <tbody>
              {o.byService.filter((s) => s.total > 0).map((s) => (
                <tr key={s.name}>
                  <td>{s.name}</td><td>{'●'.repeat(s.criticality)}</td><td>{s.total}</td><td>{s.open}</td>
                  <td>{s.breached > 0 ? <span className="badge b-BREACHED">{s.breached}</span> : 0}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <WorkloadCard rows={o.technicianWorkload} />
      </div>
    </>
  );
}

export function WorkloadCard({ rows }) {
  return (
    <div className="card">
      <h2>Technician workload</h2>
      <table>
        <thead><tr><th>Technician</th><th>Load</th><th>Resolved</th><th>Rating</th></tr></thead>
        <tbody>
          {rows.map((t) => {
            const ratio = t.active / (t.max || 1);
            return (
              <tr key={t.id}>
                <td>
                  <div style={{ fontWeight: 600 }}>{t.name} {!t.available && <span className="badge b-CLOSED">away</span>}</div>
                  <div className="muted small">{t.code} · {t.level}</div>
                </td>
                <td style={{ minWidth: 120 }}>
                  <div className="small">{t.active} / {t.max}</div>
                  <Bar value={t.active} max={t.max} tone={ratio >= 1 ? 'danger' : ratio >= 0.7 ? 'warning' : ''} />
                </td>
                <td>{t.resolved}</td>
                <td>{t.avgRating ? t.avgRating.toFixed(1) + ' ★' : '—'}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
