import { useState } from 'react';
import { api } from '../../api';
import { useApi, useReferenceData } from '../../hooks';
import { ErrorAlert, Field, Loading } from '../../components/ui';
import { fmtDate } from '../../format';

const NEW_USER = {
  role: 'TECHNICIAN', username: '', password: '', fullName: '', email: '', phone: '', department: '',
  level: 'L1', experienceYears: 1, maxWorkload: 5, skillIds: [], itManager: false,
};

export default function AdminUsers() {
  const [roleFilter, setRoleFilter] = useState('');
  const users = useApi('/users', { role: roleFilter });
  const { categories, enums } = useReferenceData();
  const [form, setForm] = useState(null);
  const [error, setError] = useState(null);
  const [message, setMessage] = useState(null);

  const editing = form?.id != null;
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value });
  const toggleSkill = (id) => setForm({
    ...form, skillIds: form.skillIds.includes(id) ? form.skillIds.filter((x) => x !== id) : [...form.skillIds, id],
  });

  const startEdit = (u) => setForm({
    id: u.id, role: u.role, username: u.username, fullName: u.fullName, email: u.email, phone: u.phone ?? '',
    department: u.department ?? '', level: u.level ?? 'L1', experienceYears: u.experienceYears ?? 0,
    maxWorkload: u.maxWorkload ?? 5, available: u.available ?? true, skillIds: (u.skills ?? []).map((s) => s.id),
    itManager: Boolean(u.itManager), active: u.active,
  });

  const save = async (e) => {
    e.preventDefault();
    setError(null);
    const tech = form.role === 'TECHNICIAN';
    const common = {
      fullName: form.fullName, email: form.email, phone: form.phone, department: form.role === 'EMPLOYEE' ? form.department : null,
      level: tech ? form.level : null, experienceYears: tech ? Number(form.experienceYears) : null,
      maxWorkload: tech ? Number(form.maxWorkload) : null, skillIds: tech ? form.skillIds : null,
      itManager: form.role === 'ADMIN' ? form.itManager : null,
    };
    try {
      if (editing) {
        await api('/users/' + form.id, { method: 'PUT', body: { ...common, available: tech ? form.available : null, active: form.active } });
        setMessage('User updated');
      } else {
        await api('/users', { method: 'POST', body: { ...common, role: form.role, username: form.username, password: form.password } });
        setMessage('User created');
      }
      setForm(null);
      users.reload();
    } catch (err) {
      setError(err);
    }
  };

  const resetPassword = async (u) => {
    const pw = window.prompt(`New password for ${u.fullName} (min 6 characters):`);
    if (!pw) return;
    try {
      await api(`/users/${u.id}/reset-password`, { method: 'POST', body: { newPassword: pw } });
      setMessage('Password reset for ' + u.fullName);
    } catch (err) {
      setError(err);
    }
  };

  const fieldErrors = error?.body?.fieldErrors ?? {};

  return (
    <div className="stack">
      <div className="page-head">
        <div>
          <h1>Users & technicians</h1>
          <p className="muted">Create accounts, maintain technician skills, levels and capacity, and deactivate leavers.</p>
        </div>
        <div className="row">
          <select value={roleFilter} onChange={(e) => setRoleFilter(e.target.value)} style={{ width: 'auto' }}>
            <option value="">All roles</option>
            <option value="EMPLOYEE">Employees</option>
            <option value="TECHNICIAN">Technicians</option>
            <option value="ADMIN">Administrators</option>
          </select>
          <button onClick={() => { setError(null); setForm(NEW_USER); }}>➕ New user</button>
        </div>
      </div>
      {message && <div className="alert success">{message}</div>}

      {form && (
        <form className="card" onSubmit={save}>
          <h2>{editing ? `Edit ${form.fullName}` : 'Create user'}</h2>
          <ErrorAlert error={error} />
          <div className="grid grid-4">
            <Field label="Role">
              <select value={form.role} onChange={set('role')} disabled={editing}>
                <option value="EMPLOYEE">Employee</option>
                <option value="TECHNICIAN">Technician</option>
                <option value="ADMIN">Administrator</option>
              </select>
            </Field>
            <Field label="Username" error={fieldErrors.username}><input value={form.username} onChange={set('username')} disabled={editing} required /></Field>
            {!editing && <Field label="Initial password" error={fieldErrors.password}><input type="password" value={form.password} onChange={set('password')} required /></Field>}
            <Field label="Full name" error={fieldErrors.fullName}><input value={form.fullName} onChange={set('fullName')} required /></Field>
            <Field label="E-mail" error={fieldErrors.email}><input type="email" value={form.email} onChange={set('email')} required /></Field>
            <Field label="Phone" error={fieldErrors.phone}><input value={form.phone} onChange={set('phone')} /></Field>
            {form.role === 'EMPLOYEE' && <Field label="Department"><input value={form.department} onChange={set('department')} /></Field>}
            {form.role === 'TECHNICIAN' && (
              <>
                <Field label="Level">
                  <select value={form.level} onChange={set('level')}>
                    {(enums.technicianLevels ?? []).map((l) => <option key={l.value} value={l.value}>{l.label}</option>)}
                  </select>
                </Field>
                <Field label="Experience (years)"><input type="number" min={0} max={50} value={form.experienceYears} onChange={set('experienceYears')} /></Field>
                <Field label="Max open incidents"><input type="number" min={1} max={50} value={form.maxWorkload} onChange={set('maxWorkload')} /></Field>
                {editing && <Field label="Availability"><label className="checkbox"><input type="checkbox" checked={form.available} onChange={set('available')} /> Available</label></Field>}
              </>
            )}
            {form.role === 'ADMIN' && <Field label="IT Manager"><label className="checkbox"><input type="checkbox" checked={form.itManager} onChange={set('itManager')} /> Receives level-3 escalations</label></Field>}
            {editing && <Field label="Account"><label className="checkbox"><input type="checkbox" checked={form.active} onChange={set('active')} /> Active</label></Field>}
          </div>
          {form.role === 'TECHNICIAN' && (
            <Field label="Skills (categories this technician can handle)">
              <div className="row">
                {categories.map((c) => (
                  <label key={c.id} className="checkbox" style={{ marginRight: 12 }}>
                    <input type="checkbox" checked={form.skillIds.includes(c.id)} onChange={() => toggleSkill(c.id)} /> {c.name}
                  </label>
                ))}
              </div>
            </Field>
          )}
          <div className="row">
            <button type="submit">{editing ? 'Save changes' : 'Create user'}</button>
            <button type="button" className="secondary" onClick={() => setForm(null)}>Cancel</button>
          </div>
        </form>
      )}

      <div className="card">
        {!form && <ErrorAlert error={error} />}
        {users.loading && !users.data ? <Loading /> : (
          <div className="table-wrap">
            <table>
              <thead><tr><th>ID</th><th>Name</th><th>Role</th><th>Details</th><th>Contact</th><th>Status</th><th>Created</th><th /></tr></thead>
              <tbody>
                {(users.data ?? []).map((u) => (
                  <tr key={u.id}>
                    <td className="mono">{u.userCode}</td>
                    <td><div style={{ fontWeight: 600 }}>{u.fullName}</div><div className="muted small">@{u.username}</div></td>
                    <td>{u.role}</td>
                    <td className="small">
                      {u.role === 'EMPLOYEE' && u.department}
                      {u.role === 'TECHNICIAN' && (
                        <>
                          <div>{u.levelTitle} · {u.experienceYears}y · load {u.activeWorkload ?? 0}/{u.maxWorkload}</div>
                          <div className="muted">{(u.skills ?? []).map((s) => s.name).join(', ')}</div>
                        </>
                      )}
                      {u.role === 'ADMIN' && (u.itManager ? 'IT Manager' : 'System administrator')}
                    </td>
                    <td className="small">{u.email}<div className="muted">{u.phone}</div></td>
                    <td>
                      {u.active ? <span className="badge b-ON_TRACK">active</span> : <span className="badge b-CLOSED">inactive</span>}
                      {u.role === 'TECHNICIAN' && !u.available && <span className="badge b-AT_RISK" style={{ marginLeft: 4 }}>away</span>}
                    </td>
                    <td className="small">{fmtDate(u.createdAt)}</td>
                    <td>
                      <button className="ghost small" onClick={() => { setError(null); startEdit(u); }}>Edit</button>
                      <button className="ghost small" onClick={() => resetPassword(u)}>Reset password</button>
                    </td>
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
