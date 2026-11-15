import { useState } from 'react';
import { api } from '../../api';
import { useReferenceData } from '../../hooks';
import { ErrorAlert, Field } from '../../components/ui';

const NEW_CATEGORY = { code: '', name: '', description: '', keywords: '', active: true };
const NEW_SERVICE = { name: '', description: '', businessCriticality: 3, defaultCategoryId: '', ownerTeam: '', active: true };

export default function AdminCatalog() {
  const { services, categories, reload } = useReferenceData();
  const [category, setCategory] = useState(null);
  const [service, setService] = useState(null);
  const [error, setError] = useState(null);

  const saveCategory = async (e) => {
    e.preventDefault();
    setError(null);
    try {
      if (category.id) await api('/categories/' + category.id, { method: 'PUT', body: category });
      else await api('/categories', { method: 'POST', body: category });
      setCategory(null);
      reload();
    } catch (err) {
      setError(err);
    }
  };

  const saveService = async (e) => {
    e.preventDefault();
    setError(null);
    const body = { ...service, businessCriticality: Number(service.businessCriticality), defaultCategoryId: service.defaultCategoryId ? Number(service.defaultCategoryId) : null };
    try {
      if (service.id) await api('/services/' + service.id, { method: 'PUT', body });
      else await api('/services', { method: 'POST', body });
      setService(null);
      reload();
    } catch (err) {
      setError(err);
    }
  };

  return (
    <div className="stack">
      <div className="page-head">
        <div>
          <h1>Services & categories</h1>
          <p className="muted">The service catalogue drives priority (business criticality) and categorisation (keywords and default category).</p>
        </div>
      </div>
      <ErrorAlert error={error} />

      <div className="card">
        <div className="row between">
          <h2>IT services</h2>
          <button className="small" onClick={() => setService(NEW_SERVICE)}>➕ New service</button>
        </div>
        {service && (
          <form onSubmit={saveService} className="note" style={{ marginBottom: 14 }}>
            <div className="grid grid-3">
              <Field label="Name"><input value={service.name} onChange={(e) => setService({ ...service, name: e.target.value })} required /></Field>
              <Field label="Business criticality (1-5)">
                <input type="number" min={1} max={5} value={service.businessCriticality} onChange={(e) => setService({ ...service, businessCriticality: e.target.value })} />
              </Field>
              <Field label="Default category">
                <select value={service.defaultCategoryId ?? ''} onChange={(e) => setService({ ...service, defaultCategoryId: e.target.value })}>
                  <option value="">None</option>
                  {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                </select>
              </Field>
              <Field label="Owner team"><input value={service.ownerTeam ?? ''} onChange={(e) => setService({ ...service, ownerTeam: e.target.value })} /></Field>
              <Field label="Description"><input value={service.description ?? ''} onChange={(e) => setService({ ...service, description: e.target.value })} /></Field>
              <Field label="Status"><label className="checkbox"><input type="checkbox" checked={service.active} onChange={(e) => setService({ ...service, active: e.target.checked })} /> Active</label></Field>
            </div>
            <div className="row"><button type="submit">Save service</button><button type="button" className="secondary" onClick={() => setService(null)}>Cancel</button></div>
          </form>
        )}
        <table>
          <thead><tr><th>Service</th><th>Criticality</th><th>Default category</th><th>Owner team</th><th>Active</th><th /></tr></thead>
          <tbody>
            {services.map((s) => (
              <tr key={s.id}>
                <td><div style={{ fontWeight: 600 }}>{s.name}</div><div className="muted small">{s.description}</div></td>
                <td>{'●'.repeat(s.businessCriticality)}{'○'.repeat(5 - s.businessCriticality)}</td>
                <td>{s.defaultCategory?.name ?? '—'}</td>
                <td className="small">{s.ownerTeam}</td>
                <td>{s.active ? '✓' : '—'}</td>
                <td><button className="ghost small" onClick={() => setService({ ...s, defaultCategoryId: s.defaultCategory?.id ?? '' })}>Edit</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="card">
        <div className="row between">
          <h2>Categories</h2>
          <button className="small" onClick={() => setCategory(NEW_CATEGORY)}>➕ New category</button>
        </div>
        {category && (
          <form onSubmit={saveCategory} className="note" style={{ marginBottom: 14 }}>
            <div className="grid grid-3">
              <Field label="Code"><input value={category.code} onChange={(e) => setCategory({ ...category, code: e.target.value })} required /></Field>
              <Field label="Name"><input value={category.name} onChange={(e) => setCategory({ ...category, name: e.target.value })} required /></Field>
              <Field label="Status"><label className="checkbox"><input type="checkbox" checked={category.active} onChange={(e) => setCategory({ ...category, active: e.target.checked })} /> Active</label></Field>
            </div>
            <Field label="Description"><input value={category.description ?? ''} onChange={(e) => setCategory({ ...category, description: e.target.value })} /></Field>
            <Field label="Keywords used by the auto-classifier (comma separated)">
              <textarea value={category.keywords ?? ''} onChange={(e) => setCategory({ ...category, keywords: e.target.value })} rows={3} />
            </Field>
            <div className="row"><button type="submit">Save category</button><button type="button" className="secondary" onClick={() => setCategory(null)}>Cancel</button></div>
          </form>
        )}
        <table>
          <thead><tr><th>Code</th><th>Name</th><th>Keywords</th><th>Active</th><th /></tr></thead>
          <tbody>
            {categories.map((c) => (
              <tr key={c.id}>
                <td className="mono">{c.code}</td>
                <td><div style={{ fontWeight: 600 }}>{c.name}</div><div className="muted small">{c.description}</div></td>
                <td className="small muted" style={{ maxWidth: 480 }}>{c.keywords}</td>
                <td>{c.active ? '✓' : '—'}</td>
                <td><button className="ghost small" onClick={() => setCategory(c)}>Edit</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
