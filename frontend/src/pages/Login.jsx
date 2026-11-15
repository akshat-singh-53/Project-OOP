import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth';
import { ErrorAlert, Field } from '../components/ui';

const DEMO = [
  { username: 'amit', password: 'pass123', label: 'Employee (Amit)' },
  { username: 'neha', password: 'pass123', label: 'Employee (Neha)' },
  { username: 'rahul', password: 'pass123', label: 'Team Lead (Rahul)' },
  { username: 'priya', password: 'pass123', label: 'Technician (Priya)' },
  { username: 'admin', password: 'admin123', label: 'IT Manager (Admin)' },
];

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ username: '', password: '' });
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e, creds = form) => {
    e?.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await login(creds.username.trim(), creds.password);
      navigate('/');
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="auth-wrap">
      <div className="auth-hero">
        <h1>Intelligent IT Service Management</h1>
        <p style={{ opacity: 0.9, marginBottom: 20 }}>Report, track and resolve IT incidents with an assistant that does the triage for you.</p>
        <ul>
          <li>Automatic categorisation and priority from the impact × urgency matrix</li>
          <li>Smart technician assignment based on skills, workload and experience</li>
          <li>SLA monitoring with warnings, violations and automatic escalation</li>
          <li>Duplicate detection, suggested resolutions and major-incident correlation</li>
          <li>Complete audit trail of every action</li>
        </ul>
      </div>
      <div className="auth-form">
        <div className="card">
          <h1>Sign in</h1>
          <p className="muted" style={{ marginBottom: 18 }}>Use your I-ITSM account.</p>
          <ErrorAlert error={error} />
          <form onSubmit={submit}>
            <Field label="Username">
              <input value={form.username} autoFocus onChange={(e) => setForm({ ...form, username: e.target.value })} />
            </Field>
            <Field label="Password">
              <input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
            </Field>
            <button type="submit" disabled={busy || !form.username || !form.password} style={{ width: '100%', justifyContent: 'center' }}>
              {busy ? 'Signing in…' : 'Sign in'}
            </button>
          </form>
          <p className="small" style={{ marginTop: 14 }}>
            New employee? <Link to="/register">Create an account</Link>
          </p>
          <hr style={{ border: 0, borderTop: '1px solid var(--border)', margin: '16px 0' }} />
          <div className="small muted" style={{ marginBottom: 8 }}>Demo accounts (one click):</div>
          <div className="demo-accounts">
            {DEMO.map((d) => (
              <button key={d.username} type="button" className="secondary small" disabled={busy}
                      onClick={() => { setForm(d); submit(null, d); }}>
                {d.label}
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
