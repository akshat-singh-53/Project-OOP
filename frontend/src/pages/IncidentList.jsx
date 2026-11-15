import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useAuth } from '../auth';
import { useApi, useReferenceData } from '../hooks';
import IncidentTable from '../components/IncidentTable';
import { ErrorAlert, Loading } from '../components/ui';

const BLANK = { code: '', text: '', status: '', priority: '', categoryId: '', serviceId: '', technicianId: '', slaStatus: '', from: '', to: '', openOnly: false, unassignedOnly: false };

export default function IncidentList() {
  const { user } = useAuth();
  const staff = user.role !== 'EMPLOYEE';
  const [searchParams] = useSearchParams();
  const { services, categories, enums } = useReferenceData();
  const technicians = useApi(staff ? '/technicians' : null);

  const presetFromUrl = () => ({
    ...BLANK,
    technicianId: searchParams.get('mine') ? String(user.id) : '',
    unassignedOnly: Boolean(searchParams.get('unassigned')),
    openOnly: Boolean(searchParams.get('mine') || searchParams.get('unassigned')),
  });
  const [draft, setDraft] = useState(presetFromUrl);
  const [filters, setFilters] = useState(presetFromUrl);

  useEffect(() => {
    const preset = presetFromUrl();
    setDraft(preset);
    setFilters(preset);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams]);

  const result = useApi('/incidents', filters);
  const set = (k) => (e) => setDraft({ ...draft, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value });
  const apply = (e) => { e.preventDefault(); setFilters(draft); };
  const reset = () => { setDraft(BLANK); setFilters(BLANK); };

  const title = searchParams.get('mine') ? 'My assigned queue'
    : searchParams.get('unassigned') ? 'Unassigned queue'
      : staff ? 'All incidents' : 'My incidents';

  return (
    <div className="stack">
      <div className="page-head">
        <div>
          <h1>{title}</h1>
          <p className="muted">{result.data ? `${result.data.length} incident(s)` : ' '}</p>
        </div>
        <button className="secondary" onClick={result.reload}>⟳ Refresh</button>
      </div>

      <form className="card" onSubmit={apply}>
        <div className="filters">
          <div><label>Incident ID</label><input value={draft.code} onChange={set('code')} placeholder="INC1001" /></div>
          <div><label>Keyword</label><input value={draft.text} onChange={set('text')} placeholder="title or description" /></div>
          <div>
            <label>Status</label>
            <select value={draft.status} onChange={set('status')}>
              <option value="">Any</option>
              {(enums.statuses ?? []).map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
            </select>
          </div>
          <div>
            <label>Priority</label>
            <select value={draft.priority} onChange={set('priority')}>
              <option value="">Any</option>
              {(enums.priorities ?? []).map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
            </select>
          </div>
          <div>
            <label>Category</label>
            <select value={draft.categoryId} onChange={set('categoryId')}>
              <option value="">Any</option>
              {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>
          <div>
            <label>Service</label>
            <select value={draft.serviceId} onChange={set('serviceId')}>
              <option value="">Any</option>
              {services.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </div>
          {staff && (
            <div>
              <label>Technician</label>
              <select value={draft.technicianId} onChange={set('technicianId')}>
                <option value="">Any</option>
                {(technicians.data ?? []).map((t) => <option key={t.id} value={t.id}>{t.fullName}</option>)}
              </select>
            </div>
          )}
          <div>
            <label>SLA status</label>
            <select value={draft.slaStatus} onChange={set('slaStatus')}>
              <option value="">Any</option>
              {(enums.slaStatuses ?? []).map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
            </select>
          </div>
          <div><label>Created from</label><input type="date" value={draft.from} onChange={set('from')} /></div>
          <div><label>Created to</label><input type="date" value={draft.to} onChange={set('to')} /></div>
          <div>
            <label className="checkbox"><input type="checkbox" checked={draft.openOnly} onChange={set('openOnly')} /> Open only</label>
            {staff && <label className="checkbox"><input type="checkbox" checked={draft.unassignedOnly} onChange={set('unassignedOnly')} /> Unassigned only</label>}
          </div>
          <div className="row">
            <button type="submit">Search</button>
            <button type="button" className="secondary" onClick={reset}>Clear</button>
          </div>
        </div>
      </form>

      <div className="card">
        <ErrorAlert error={result.error} />
        {result.loading && !result.data ? <Loading /> : <IncidentTable incidents={result.data} showReporter={staff} />}
      </div>
    </div>
  );
}
