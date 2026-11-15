import { useState } from 'react';
import { api } from '../api';
import { useAuth } from '../auth';
import { ErrorAlert, Field } from '../components/ui';
import { fmtDate } from '../format';

export default function Profile() {
  const { user, refresh } = useAuth();
  const [pw, setPw] = useState({ currentPassword: '', newPassword: '', confirm: '' });
  const [error, setError] = useState(null);
  const [message, setMessage] = useState(null);

  const changePassword = async (e) => {
    e.preventDefault();
    setError(null);
    setMessage(null);
    if (pw.newPassword !== pw.confirm) {
      setError({ message: 'New passwords do not match' });
      return;
    }
    try {
      await api('/auth/change-password', { method: 'POST', body: { currentPassword: pw.currentPassword, newPassword: pw.newPassword } });
      setPw({ currentPassword: '', newPassword: '', confirm: '' });
      setMessage('Password changed');
    } catch (err) {
      setError(err);
    }
  };

  const toggleAvailability = async () => {
    setError(null);
    try {
      await api('/technicians/me/availability', { method: 'PUT', body: { available: !user.available } });
      await refresh();
      setMessage(user.available ? 'You are now marked as away — no new incidents will be auto-assigned to you' : 'You are available for new incidents');
    } catch (err) {
      setError(err);
    }
  };

  return (
    <div className="stack" style={{ maxWidth: 900 }}>
      <h1>My profile</h1>
      {message && <div className="alert success">{message}</div>}
      <ErrorAlert error={error} />
      <div className="grid grid-2">
        <div className="card">
          <h2>Account</h2>
          <dl className="kv">
            <dt>User ID</dt><dd className="mono">{user.userCode}</dd>
            <dt>Name</dt><dd>{user.fullName}</dd>
            <dt>Username</dt><dd>{user.username}</dd>
            <dt>E-mail</dt><dd>{user.email}</dd>
            <dt>Phone</dt><dd>{user.phone || '—'}</dd>
            <dt>Role</dt><dd>{user.role}</dd>
            {user.department && (<><dt>Department</dt><dd>{user.department}</dd></>)}
            {user.levelTitle && (<><dt>Level</dt><dd>{user.levelTitle}</dd></>)}
            {user.experienceYears != null && (<><dt>Experience</dt><dd>{user.experienceYears} years</dd></>)}
            {user.maxWorkload != null && (<><dt>Max workload</dt><dd>{user.maxWorkload} open incidents</dd></>)}
            {user.skills && (<><dt>Skills</dt><dd>{user.skills.map((s) => <span key={s.id} className="badge b-accent" style={{ marginRight: 4 }}>{s.name}</span>)}</dd></>)}
            {user.itManager != null && (<><dt>IT Manager</dt><dd>{user.itManager ? 'Yes' : 'No'}</dd></>)}
            <dt>Member since</dt><dd>{fmtDate(user.createdAt)}</dd>
          </dl>
          {user.role === 'TECHNICIAN' && (
            <div style={{ marginTop: 16 }}>
              <div className="row between">
                <div>
                  <strong>Availability:</strong> {user.available ? '🟢 Available' : '⚪ Away'}
                  <div className="muted small">Away technicians are skipped by automatic assignment.</div>
                </div>
                <button className="secondary" onClick={toggleAvailability}>{user.available ? 'Go away' : 'Become available'}</button>
              </div>
            </div>
          )}
        </div>
        <form className="card" onSubmit={changePassword}>
          <h2>Change password</h2>
          <Field label="Current password"><input type="password" value={pw.currentPassword} onChange={(e) => setPw({ ...pw, currentPassword: e.target.value })} required /></Field>
          <Field label="New password (min 6)"><input type="password" value={pw.newPassword} onChange={(e) => setPw({ ...pw, newPassword: e.target.value })} required /></Field>
          <Field label="Confirm new password"><input type="password" value={pw.confirm} onChange={(e) => setPw({ ...pw, confirm: e.target.value })} required /></Field>
          <button type="submit">Update password</button>
        </form>
      </div>
    </div>
  );
}
