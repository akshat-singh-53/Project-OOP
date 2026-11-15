import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../auth';
import { useApi, useInterval, useReferenceData } from '../hooks';
import { Bar, ErrorAlert, Field, Loading, PriorityBadge, SlaBadge, StatusBadge } from '../components/ui';
import { fmtDate, fmtMinutes, label, relative } from '../format';

/** Resolves /incidents/code/INC1001 links (used by notifications) to the numeric route. */
export function IncidentByCode() {
  const { code } = useParams();
  const navigate = useNavigate();
  const [error, setError] = useState(null);
  useEffect(() => {
    api('/incidents/code/' + code)
      .then((d) => navigate('/incidents/' + d.summary.id, { replace: true }))
      .catch(setError);
  }, [code, navigate]);
  return error ? <ErrorAlert error={error} /> : <Loading />;
}

/** Dialogs that need extra input before calling the API. */
const FORM_ACTIONS = {
  REQUEST_INFO: { title: 'Ask the reporter for more information', field: 'reason', path: 'request-info', placeholder: 'What do you need to know?' },
  PROVIDE_INFO: { title: 'Provide the requested information', field: 'reason', path: 'provide-info', placeholder: 'Your answer…' },
  REOPEN: { title: 'Re-open: the problem is not fixed', field: 'reason', path: 'reopen', placeholder: 'What is still wrong?' },
  CLOSE: { title: 'Close incident', field: 'notes', path: 'close', placeholder: 'Closure notes (optional)', optional: true },
  MARK_DUPLICATE: { title: 'Mark as duplicate of another incident', field: 'originalCode', path: 'duplicate', placeholder: 'INC1001', input: true },
};

const SIMPLE_ACTIONS = {
  ACKNOWLEDGE: { label: '👋 Acknowledge', path: 'acknowledge' },
  START_WORK: { label: '▶ Start work', path: 'start' },
  VERIFY: { label: '✔ Confirm it is fixed', path: 'verify' },
};

export default function IncidentDetail() {
  const { id } = useParams();
  const { user } = useAuth();
  const staff = user.role !== 'EMPLOYEE';
  const detail = useApi('/incidents/' + id);
  const [error, setError] = useState(null);
  const [flash, setFlash] = useState(null);
  const [dialog, setDialog] = useState(null);

  if (detail.loading && !detail.data) return <Loading />;
  if (detail.error) return <ErrorAlert error={detail.error} />;
  const d = detail.data;
  const s = d.summary;
  const actions = new Set(d.allowedActions);

  const run = async (path, body = {}, message) => {
    setError(null);
    try {
      const updated = await api(`/incidents/${id}/${path}`, { method: 'POST', body });
      detail.setData(updated);
      setDialog(null);
      setFlash(message ?? 'Done');
      setTimeout(() => setFlash(null), 3500);
      return true;
    } catch (e) {
      setError(e);
      return false;
    }
  };

  return (
    <div className="stack">
      <div className="page-head">
        <div>
          <div className="row" style={{ gap: 8 }}>
            <span className="mono muted">{s.code}</span>
            <PriorityBadge value={s.priority} />
            <StatusBadge value={s.status} />
            <SlaBadge value={s.slaStatus} />
            {s.escalationLevel > 0 && <span className="badge b-BREACHED">Escalated · level {s.escalationLevel}</span>}
          </div>
          <h1 style={{ marginTop: 6 }}>{s.title}</h1>
          <p className="muted small">
            Reported by {s.reporter?.fullName} ({s.reporter?.userCode}) · {fmtDate(s.createdAt)} · {s.serviceName}
          </p>
        </div>
        <button className="secondary" onClick={detail.reload}>⟳ Refresh</button>
      </div>

      {flash && <div className="alert success">{flash}</div>}
      <ErrorAlert error={error} />
      {s.suspectedDuplicateCode && !d.duplicateOfCode && (
        <div className="alert warning">
          The system thinks this may be a duplicate of <Link to={'/incidents/code/' + s.suspectedDuplicateCode}>{s.suspectedDuplicateCode}</Link>.
        </div>
      )}
      {d.duplicateOfCode && (
        <div className="alert info">Closed as a duplicate of <Link to={'/incidents/code/' + d.duplicateOfCode}>{d.duplicateOfCode}</Link>.</div>
      )}

      <div className="grid detail-grid">
        <div className="stack">
          <div className="card">
            <h2>Description</h2>
            <p style={{ whiteSpace: 'pre-wrap' }}>{d.description}</p>
          </div>

          {d.resolution && (
            <div className="card" style={{ borderColor: '#a7f3d0' }}>
              <h2>✅ Resolution</h2>
              <p style={{ whiteSpace: 'pre-wrap' }}>{d.resolution}</p>
              {d.rootCause && <p className="small"><strong>Root cause:</strong> {d.rootCause}</p>}
              {d.closureNotes && <p className="small"><strong>Closure notes:</strong> {d.closureNotes}</p>}
              {d.feedbackRating && (
                <p className="small"><strong>User rating:</strong> {'★'.repeat(d.feedbackRating)}{'☆'.repeat(5 - d.feedbackRating)} {d.feedbackComment}</p>
              )}
            </div>
          )}

          <ActionPanel d={d} actions={actions} user={user} run={run} setDialog={setDialog} />
          {dialog && <ActionDialog spec={dialog} run={run} onCancel={() => setDialog(null)} />}

          {user.role === 'EMPLOYEE' && !d.feedbackRating && ['RESOLVED', 'VERIFIED', 'CLOSED'].includes(s.status)
            && s.reporter?.id === user.id && !d.duplicateOfCode && <FeedbackForm run={run} />}

          <Notes d={d} staff={staff} run={run} />
          <History history={d.history} />
        </div>

        <div className="stack">
          <SlaCard d={d} />
          <div className="card">
            <h2>Details</h2>
            <dl className="kv">
              <dt>Category</dt>
              <dd>{s.category?.name ?? '—'} {d.categorySuggestedAutomatically && <span className="badge b-accent">auto</span>}</dd>
              <dt>Service</dt><dd>{s.serviceName} <span className="muted small">(criticality {d.businessCriticality}/5)</span></dd>
              <dt>Impact / urgency</dt><dd>{label(d.impact)} / {label(d.urgency)}</dd>
              <dt>Users affected</dt><dd>{d.affectedUsers}</dd>
              <dt>Technician</dt>
              <dd>{s.technician ? `${s.technician.fullName} (${s.technician.userCode})` : <span className="muted">Unassigned</span>}</dd>
              {d.assignmentStrategy && (<><dt>Assigned by</dt><dd>{d.assignmentStrategy}</dd></>)}
              <dt>Reopened</dt><dd>{d.reopenCount} time(s)</dd>
            </dl>
            {d.priorityExplanation && (
              <div className="alert info small" style={{ marginTop: 12, marginBottom: 0 }}>
                <strong>Why {s.priority}?</strong> {d.priorityExplanation}
              </div>
            )}
          </div>
          {staff && s.status !== 'CLOSED' && <StaffTools d={d} run={run} actions={actions} user={user} />}
          {staff && <SuggestedResolutions id={id} />}
          {staff && <SimilarIncidents id={id} />}
        </div>
      </div>
    </div>
  );
}

function ActionPanel({ d, actions, user, run, setDialog }) {
  const [resolving, setResolving] = useState(false);
  const s = d.summary;
  const has = (a) => actions.has(a);
  const canTake = user.role === 'TECHNICIAN' && has('ASSIGN') && !s.technician;
  const any = d.allowedActions.length > 0;

  if (!any) {
    return null;
  }
  return (
    <div className="card">
      <h2>Next steps</h2>
      <div className="row">
        {canTake && <button onClick={() => run('take', {}, 'You are now working on this incident')}>🙋 Take this incident</button>}
        {Object.entries(SIMPLE_ACTIONS).filter(([a]) => has(a)).map(([a, spec]) => (
          <button key={a} onClick={() => run(spec.path, {}, label(a) + ' — done')}>{spec.label}</button>
        ))}
        {has('RESOLVE') && <button onClick={() => setResolving(!resolving)}>✅ Resolve</button>}
        {has('REQUEST_INFO') && <button className="secondary" onClick={() => setDialog(FORM_ACTIONS.REQUEST_INFO)}>❓ Request info</button>}
        {has('PROVIDE_INFO') && <button onClick={() => setDialog(FORM_ACTIONS.PROVIDE_INFO)}>💬 Provide info</button>}
        {has('REOPEN') && <button className="secondary" onClick={() => setDialog(FORM_ACTIONS.REOPEN)}>↩ Not fixed — reopen</button>}
        {has('CLOSE') && <button className="secondary" onClick={() => setDialog(FORM_ACTIONS.CLOSE)}>🔒 Close</button>}
        {has('MARK_DUPLICATE') && <button className="secondary" onClick={() => setDialog(FORM_ACTIONS.MARK_DUPLICATE)}>🔗 Mark duplicate</button>}
      </div>
      {s.status === 'PENDING_INFO' && user.id === s.reporter?.id && (
        <div className="alert warning" style={{ marginTop: 12, marginBottom: 0 }}>The support team is waiting for your answer. See the latest note below.</div>
      )}
      {s.status === 'RESOLVED' && user.id === s.reporter?.id && (
        <div className="alert info" style={{ marginTop: 12, marginBottom: 0 }}>Please confirm the fix works, or re-open the incident if it does not.</div>
      )}
      {resolving && <ResolveForm run={run} onDone={() => setResolving(false)} />}
    </div>
  );
}

function ActionDialog({ spec, run, onCancel }) {
  const [value, setValue] = useState('');
  return (
    <div className="card" style={{ borderColor: 'var(--accent)' }}>
      <h3>{spec.title}</h3>
      {spec.input
        ? <input value={value} onChange={(e) => setValue(e.target.value)} placeholder={spec.placeholder} autoFocus />
        : <textarea value={value} onChange={(e) => setValue(e.target.value)} placeholder={spec.placeholder} autoFocus />}
      <div className="row" style={{ marginTop: 10 }}>
        <button disabled={!spec.optional && !value.trim()}
                onClick={() => run(spec.path, { [spec.field]: value.trim() || null }, spec.title + ' — done')}>Submit</button>
        <button className="secondary" onClick={onCancel}>Cancel</button>
      </div>
    </div>
  );
}

function ResolveForm({ run, onDone }) {
  const [form, setForm] = useState({ resolution: '', rootCause: '' });
  return (
    <div style={{ marginTop: 14 }}>
      <Field label="Resolution — what did you do to fix it? (min 10 chars)">
        <textarea value={form.resolution} onChange={(e) => setForm({ ...form, resolution: e.target.value })} />
      </Field>
      <Field label="Root cause (optional)">
        <input value={form.rootCause} onChange={(e) => setForm({ ...form, rootCause: e.target.value })} />
      </Field>
      <div className="row">
        <button disabled={form.resolution.trim().length < 10}
                onClick={async () => { if (await run('resolve', form, 'Incident resolved — the reporter will be asked to verify')) onDone(); }}>
          Mark resolved
        </button>
        <button className="secondary" onClick={onDone}>Cancel</button>
      </div>
    </div>
  );
}

function FeedbackForm({ run }) {
  const [rating, setRating] = useState(0);
  const [comment, setComment] = useState('');
  return (
    <div className="card">
      <h2>How did we do?</h2>
      <div className="stars">
        {[1, 2, 3, 4, 5].map((n) => (
          <button key={n} type="button" onClick={() => setRating(n)}>{n <= rating ? '★' : '☆'}</button>
        ))}
      </div>
      <textarea placeholder="Optional comment" value={comment} onChange={(e) => setComment(e.target.value)} style={{ marginTop: 8 }} />
      <button style={{ marginTop: 8 }} disabled={!rating} onClick={() => run('feedback', { rating, comment }, 'Thanks for your feedback!')}>
        Send feedback
      </button>
    </div>
  );
}

function Notes({ d, staff, run }) {
  const [text, setText] = useState('');
  const [internal, setInternal] = useState(false);
  const closed = d.summary.status === 'CLOSED';
  return (
    <div className="card">
      <h2>Conversation & notes ({d.notes.length})</h2>
      {d.notes.length === 0 && <p className="muted small">No notes yet.</p>}
      {d.notes.map((n) => (
        <div key={n.id} className={'note ' + n.type + (n.internal ? ' internal' : '')}>
          <div className="row between">
            <strong className="small">{n.author?.fullName ?? 'System'} <span className="muted">({n.author?.userCode ?? 'auto'})</span></strong>
            <span className="muted small">
              {n.internal && <span className="badge b-AT_RISK" style={{ marginRight: 6 }}>internal</span>}
              {label(n.type)} · {fmtDate(n.createdAt)}
            </span>
          </div>
          <div style={{ whiteSpace: 'pre-wrap', marginTop: 4 }}>{n.text}</div>
        </div>
      ))}
      {!closed && (
        <div style={{ marginTop: 12 }}>
          <textarea value={text} onChange={(e) => setText(e.target.value)}
                    placeholder={staff ? 'Add an update for the user, or an internal investigation note…' : 'Add a comment for the support team…'} />
          <div className="row between" style={{ marginTop: 8 }}>
            {staff ? (
              <label className="checkbox"><input type="checkbox" checked={internal} onChange={(e) => setInternal(e.target.checked)} /> Internal (hidden from employee)</label>
            ) : <span />}
            <button disabled={!text.trim()} onClick={async () => { if (await run('notes', { text, internal }, 'Note added')) setText(''); }}>Add note</button>
          </div>
        </div>
      )}
    </div>
  );
}

function History({ history }) {
  return (
    <div className="card">
      <h2>Audit trail</h2>
      <ul className="timeline">
        {history.map((h) => (
          <li key={h.id} className={h.actor === 'SYSTEM' ? 'sys' : ''}>
            <div className="when">{fmtDate(h.timestamp)} · {h.actor}</div>
            <div><strong>{label(h.action)}</strong>
              {h.previousState && h.newState && <span className="muted small"> {label(h.previousState)} → {label(h.newState)}</span>}
            </div>
            <div className="small">{h.description}</div>
          </li>
        ))}
      </ul>
    </div>
  );
}

function SlaCard({ d }) {
  const s = d.summary;
  const [, tick] = useState(0);
  useInterval(() => tick((x) => x + 1), 30000);
  const open = !['RESOLVED', 'VERIFIED', 'CLOSED'].includes(s.status);
  const created = new Date(s.createdAt).getTime();
  const elapsed = (Date.now() - created) / 60000;
  const responded = Boolean(d.acknowledgedAt);

  const row = (title, due, target, done, breached) => {
    const used = done ? (new Date(done).getTime() - created) / 60000 : elapsed;
    const pct = (100 * used) / (target || 1);
    const tone = breached || pct >= 100 ? 'danger' : pct >= 75 ? 'warning' : '';
    return (
      <div style={{ marginBottom: 14 }}>
        <div className="row between small">
          <strong>{title}</strong>
          <span>target {fmtMinutes(target)}</span>
        </div>
        <Bar value={Math.min(used, target)} max={target} tone={tone} />
        <div className="small muted" style={{ marginTop: 3 }}>
          {done ? `done after ${fmtMinutes(used)}${breached ? ' — breached' : ''}`
            : open ? (pct >= 100 ? `⚠ overdue by ${fmtMinutes(used - target)}` : `due ${relative(due)} (${fmtDate(due)})`) : '—'}
        </div>
      </div>
    );
  };

  return (
    <div className="card">
      <div className="row between"><h2>⏱ SLA</h2><SlaBadge value={s.slaStatus} /></div>
      {row('Response', s.responseDueAt, d.responseTargetMinutes, responded ? d.acknowledgedAt : null, d.responseBreached)}
      {row('Resolution', s.resolutionDueAt, d.resolutionTargetMinutes, d.resolvedAt, d.resolutionBreached)}
    </div>
  );
}

function StaffTools({ d, run, actions, user }) {
  const s = d.summary;
  const { categories, enums } = useReferenceData();
  const [strategy, setStrategy] = useState('');
  const [showAll, setShowAll] = useState(false);
  const recs = useApi(actions.has('ASSIGN') ? `/incidents/${s.id}/recommendations` : null, { strategy }, [s.technician?.id]);
  const [tab, setTab] = useState(actions.has('ASSIGN') ? 'assign' : 'priority');
  const [prio, setPrio] = useState({ impact: d.impact, urgency: d.urgency, affectedUsers: d.affectedUsers, reason: '' });
  const [cat, setCat] = useState({ categoryId: s.category?.id ?? '', reason: '' });
  const open = !['RESOLVED', 'VERIFIED', 'CLOSED'].includes(s.status);

  return (
    <div className="card">
      <div className="row" style={{ marginBottom: 12 }}>
        {actions.has('ASSIGN') && <button className={tab === 'assign' ? 'small' : 'secondary small'} onClick={() => setTab('assign')}>Assign</button>}
        {open && <button className={tab === 'priority' ? 'small' : 'secondary small'} onClick={() => setTab('priority')}>Re-prioritise</button>}
        {open && <button className={tab === 'category' ? 'small' : 'secondary small'} onClick={() => setTab('category')}>Re-categorise</button>}
      </div>

      {tab === 'assign' && actions.has('ASSIGN') && (
        <>
          <h3>🤖 Recommended technicians</h3>
          <div className="row" style={{ marginBottom: 8 }}>
            <select value={strategy} onChange={(e) => setStrategy(e.target.value)} style={{ width: 'auto' }}>
              <option value="">Configured strategy</option>
              <option value="smart">Smart (skills + load + experience)</option>
              <option value="skill">Skill-based</option>
              <option value="load">Load-based</option>
              <option value="round-robin">Round robin</option>
            </select>
          </div>
          {recs.loading && <Loading />}
          {(recs.data ?? []).slice(0, showAll ? undefined : 5).map((r, i) => (
            <div key={r.technicianId} className="note">
              <div className="row between">
                <strong>{i === 0 ? '⭐ ' : ''}{r.technicianName}</strong>
                <span className="badge b-accent">score {r.score.toFixed(0)}</span>
              </div>
              <div className="muted small">{r.level} · load {r.activeWorkload}/{r.maxWorkload} {r.skillMatch ? '· skill match ✓' : ''}</div>
              <ul className="small" style={{ margin: '4px 0', paddingLeft: 18 }}>{r.reasons.map((x) => <li key={x}>{x}</li>)}</ul>
              <button className="small" disabled={s.technician?.id === r.technicianId}
                      onClick={() => run('assign', { technicianId: r.technicianId }, 'Assigned to ' + r.technicianName)}>
                {s.technician?.id === r.technicianId ? 'Currently assigned' : r.technicianId === user.id ? 'Assign to me' : 'Assign'}
              </button>
            </div>
          ))}
          {recs.data?.length > 5 && (
            <button className="ghost small" onClick={() => setShowAll(!showAll)}>
              {showAll ? 'Show top 5 only' : `Show all ${recs.data.length} technicians`}
            </button>
          )}
          {recs.data?.length === 0 && <p className="muted small">No available technician has capacity right now.</p>}
        </>
      )}

      {tab === 'priority' && open && (
        <>
          <h3>Re-assess impact and urgency</h3>
          <div className="grid grid-2">
            <Field label="Impact">
              <select value={prio.impact} onChange={(e) => setPrio({ ...prio, impact: e.target.value })}>
                {(enums.impacts ?? []).map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
            </Field>
            <Field label="Urgency">
              <select value={prio.urgency} onChange={(e) => setPrio({ ...prio, urgency: e.target.value })}>
                {(enums.urgencies ?? []).map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
            </Field>
          </div>
          <Field label="Users affected"><input type="number" min={1} value={prio.affectedUsers} onChange={(e) => setPrio({ ...prio, affectedUsers: e.target.value })} /></Field>
          <Field label="Reason"><input value={prio.reason} onChange={(e) => setPrio({ ...prio, reason: e.target.value })} /></Field>
          <button disabled={!prio.reason.trim()} onClick={() => run('reprioritise', { ...prio, affectedUsers: Number(prio.affectedUsers) }, 'Priority recalculated')}>Recalculate priority</button>
        </>
      )}

      {tab === 'category' && open && (
        <>
          <h3>Change category</h3>
          <Field label="Category">
            <select value={cat.categoryId} onChange={(e) => setCat({ ...cat, categoryId: e.target.value })}>
              {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </Field>
          <Field label="Reason"><input value={cat.reason} onChange={(e) => setCat({ ...cat, reason: e.target.value })} /></Field>
          <button disabled={!cat.reason.trim() || !cat.categoryId}
                  onClick={() => run('recategorise', { categoryId: Number(cat.categoryId), reason: cat.reason }, 'Category updated')}>Save category</button>
        </>
      )}
    </div>
  );
}

function SuggestedResolutions({ id }) {
  const { data } = useApi(`/incidents/${id}/suggested-resolutions`);
  return (
    <div className="card">
      <h2>💡 Suggested resolutions</h2>
      {!data?.length && <p className="muted small">No similar resolved incidents yet.</p>}
      {(data ?? []).map((r) => (
        <div key={r.incidentCode} className="suggestion">
          <div className="row between">
            <Link className="mono" to={'/incidents/code/' + r.incidentCode}>{r.incidentCode}</Link>
            <span className="badge b-accent">{Math.round(r.similarity * 100)}% similar</span>
          </div>
          <div style={{ fontWeight: 600 }}>{r.title}</div>
          <div className="small">{r.resolution}</div>
          {r.rootCause && <div className="muted small">Root cause: {r.rootCause}</div>}
          {r.feedbackRating && <div className="small">{'★'.repeat(r.feedbackRating)}</div>}
        </div>
      ))}
    </div>
  );
}

function SimilarIncidents({ id }) {
  const { data } = useApi(`/incidents/${id}/duplicates`);
  if (!data?.length) return null;
  return (
    <div className="card">
      <h2>🔍 Possible duplicates</h2>
      {data.map((x) => (
        <div key={x.incidentCode} className="note">
          <div className="row between">
            <Link className="mono" to={'/incidents/code/' + x.incidentCode}>{x.incidentCode}</Link>
            <span className="badge b-AT_RISK">{Math.round(x.score * 100)}%</span>
          </div>
          <div className="small">{x.title}</div>
          <div className="muted small">{x.serviceName} · {label(x.status)}</div>
        </div>
      ))}
    </div>
  );
}
