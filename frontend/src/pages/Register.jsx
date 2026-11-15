import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth';
import { ErrorAlert, Field } from '../components/ui';

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ username: '', password: '', confirm: '', fullName: '', email: '', phone: '', department: '' });
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });
  const fieldErrors = error?.body?.fieldErrors ?? {};

  const submit = async (e) => {
    e.preventDefault();
    if (form.password !== form.confirm) {
      setError({ message: 'Passwords do not match' });
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const { confirm, ...body } = form;
      await register(body);
      navigate('/');
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="auth-form" style={{ minHeight: '100vh' }}>
      <div className="card" style={{ maxWidth: 520 }}>
        <h1>Create employee account</h1>
        <p className="muted" style={{ marginBottom: 18 }}>Technician and administrator accounts are created by an administrator.</p>
        <ErrorAlert error={error} />
        <form onSubmit={submit}>
          <div className="grid grid-2">
            <Field label="Full name" error={fieldErrors.fullName}><input value={form.fullName} onChange={set('fullName')} required /></Field>
            <Field label="Username" error={fieldErrors.username}><input value={form.username} onChange={set('username')} required /></Field>
            <Field label="E-mail" error={fieldErrors.email}><input type="email" value={form.email} onChange={set('email')} required /></Field>
            <Field label="Phone (for SMS alerts)" error={fieldErrors.phone}><input value={form.phone} onChange={set('phone')} /></Field>
            <Field label="Department" error={fieldErrors.department}><input value={form.department} onChange={set('department')} /></Field>
            <div />
            <Field label="Password (min 6)" error={fieldErrors.password}><input type="password" value={form.password} onChange={set('password')} required /></Field>
            <Field label="Confirm password"><input type="password" value={form.confirm} onChange={set('confirm')} required /></Field>
          </div>
          <div className="row between">
            <Link to="/login">← Back to sign in</Link>
            <button type="submit" disabled={busy}>{busy ? 'Creating…' : 'Create account'}</button>
          </div>
        </form>
      </div>
    </div>
  );
}
